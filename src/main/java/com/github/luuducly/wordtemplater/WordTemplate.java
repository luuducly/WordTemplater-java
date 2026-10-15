package com.github.luuducly.wordtemplater;

import com.github.luuducly.wordtemplater.evaluators.Evaluators.*;
import com.github.luuducly.wordtemplater.utils.ExpressionParser;
import com.github.luuducly.wordtemplater.utils.TokenResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class WordTemplate {
    public static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    public static final String V_NS = "urn:schemas-microsoft-com:vml";

    private final byte[] docxBytes;
    private final Map<String, IEvaluator> evaluators = new HashMap<>();
    private final ExportOptions defaultOptions = new ExportOptions();

    private WordTemplate(byte[] docxBytes) {
        this.docxBytes = docxBytes;
        registerDefaultEvaluators();
    }

    public static WordTemplate load(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return load(fis);
        }
    }

    public static WordTemplate load(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return new WordTemplate(baos.toByteArray());
    }

    public static WordTemplate load(byte[] bytes) {
        return new WordTemplate(bytes);
    }

    private void registerDefaultEvaluators() {
        registerEvaluator("upper", new UpperEvaluator());
        registerEvaluator("lower", new LowerEvaluator());
        registerEvaluator("trim", new TrimEvaluator());
        registerEvaluator("sub", new SubEvaluator());
        registerEvaluator("left", new LeftEvaluator());
        registerEvaluator("right", new RightEvaluator());
        registerEvaluator("replace", new ReplaceEvaluator());
        registerEvaluator("padleft", new PadLeftEvaluator());
        registerEvaluator("padright", new PadRightEvaluator());
        registerEvaluator("currency", new CurrencyEvaluator());
        registerEvaluator("number", new NumberEvaluator());
        registerEvaluator("percentage", new PercentageEvaluator());
        registerEvaluator("date", new DateEvaluator());
        registerEvaluator("default", new DefaultValueEvaluator());
        registerEvaluator("coalesce", new DefaultValueEvaluator());
        registerEvaluator("if", new IfEvaluator());
    }

    public void registerEvaluator(String name, IEvaluator evaluator) {
        evaluators.put(name.toLowerCase(), evaluator);
    }

    public void setWatermark(String text, String color, double angle, double fontSize) {
        defaultOptions.setWatermarkText(text);
        defaultOptions.setWatermarkColor(color);
        defaultOptions.setWatermarkAngle(angle);
        defaultOptions.setWatermarkFontSize(fontSize);
    }

    public void enableTableZebraStriping(String oddColor, String evenColor) {
        defaultOptions.setTableOddRowColor(oddColor);
        defaultOptions.setTableEvenRowColor(evenColor);
    }

    public byte[] export(Object data) throws Exception {
        return export(data, defaultOptions);
    }

    public byte[] export(Object data, ExportOptions options) throws Exception {
        Map<String, byte[]> zipEntries = new LinkedHashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buf)) != -1) {
                    entryBaos.write(buf, 0, len);
                }
                zipEntries.put(entry.getName(), entryBaos.toByteArray());
            }
        }

        byte[] docXmlBytes = zipEntries.get("word/document.xml");
        if (docXmlBytes == null) {
            throw new IllegalStateException("word/document.xml not found in .docx");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(docXmlBytes));

        // 1. Process Tables
        processTables(doc, data, options);

        // 2. Process Loops
        NodeList bodies = doc.getElementsByTagNameNS(W_NS, "body");
        if (bodies.getLength() > 0) {
            processElementLoops((Element) bodies.item(0), data, doc);
        }

        // 3. Process Fields
        processFields(doc, data);

        // Serialize back
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        ByteArrayOutputStream docBaos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(docBaos));
        zipEntries.put("word/document.xml", docBaos.toByteArray());

        // 4. Inject Watermark
        if (options.getWatermarkText() != null && !options.getWatermarkText().trim().isEmpty()) {
            injectWatermark(zipEntries, options);
        }

        // Create output zip
        ByteArrayOutputStream outBaos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outBaos)) {
            for (Map.Entry<String, byte[]> e : zipEntries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                zos.putNextEntry(ze);
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }

        return outBaos.toByteArray();
    }

    private void processFields(Document doc, Object data) {
        // Clean instrText and fldChar
        cleanNodes(doc, "instrText");
        cleanNodes(doc, "fldChar");

        Pattern placeholderRegex = Pattern.compile("«([^»]+)»");
        NodeList paragraphs = doc.getElementsByTagNameNS(W_NS, "p");

        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element p = (Element) paragraphs.item(i);
            List<Element> tNodes = getElementsByTagName(p, "t");
            if (tNodes.isEmpty()) continue;

            StringBuilder combined = new StringBuilder();
            for (Element t : tNodes) {
                combined.append(t.getTextContent());
            }

            String s = combined.toString();
            Matcher matcher = placeholderRegex.matcher(s);
            if (matcher.find()) {
                StringBuffer sb = new StringBuffer();
                matcher.reset();
                while (matcher.find()) {
                    String expr = matcher.group(1);
                    Object val = evaluateExpression(expr, data);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(val != null ? val.toString() : ""));
                }
                matcher.appendTail(sb);

                tNodes.get(0).setTextContent(sb.toString());
                for (int k = 1; k < tNodes.size(); k++) {
                    tNodes.get(k).setTextContent("");
                }
            }
        }
    }

    private void processTables(Document doc, Object data, ExportOptions options) {
        Pattern placeholderRegex = Pattern.compile("«([^»]+)»");
        Pattern tableRegex = Pattern.compile("«table\\(([^)]+)\\)»", Pattern.CASE_INSENSITIVE);

        NodeList tables = doc.getElementsByTagNameNS(W_NS, "tbl");
        for (int i = 0; i < tables.getLength(); i++) {
            Element tbl = (Element) tables.item(i);
            List<Element> rows = getElementsByTagName(tbl, "tr");

            for (Element row : rows) {
                String rowText = row.getTextContent();
                Matcher m = tableRegex.matcher(rowText);

                if (m.find()) {
                    ExpressionParser.ParsedExpression parsed = ExpressionParser.parse(m.group(0));
                    Object items = TokenResolver.resolve(data, parsed.fieldPath);

                    Object oddColor = !parsed.args.isEmpty() ? parsed.args.get(0) : options.getTableOddRowColor();
                    Object evenColor = parsed.args.size() > 1 ? parsed.args.get(1) : options.getTableEvenRowColor();

                    Node parent = row.getParentNode();
                    if (items instanceof List && !((List<?>) items).isEmpty()) {
                        List<?> list = (List<?>) items;
                        for (int idx = 0; idx < list.size(); idx++) {
                            Object item = list.get(idx);
                            Element clonedRow = (Element) row.cloneNode(true);

                            Map<String, Object> itemContext = new HashMap<>();
                            if (item instanceof Map) {
                                itemContext.putAll((Map<String, Object>) item);
                            } else {
                                itemContext.put("item", item);
                            }
                            itemContext.put("_index", idx + 1);
                            itemContext.put("_first", idx == 0);
                            itemContext.put("_last", idx == list.size() - 1);
                            itemContext.put("_even", (idx + 1) % 2 == 0);
                            itemContext.put("_odd", (idx + 1) % 2 != 0);

                            // Replace placeholders
                            replaceRowPlaceholders(clonedRow, itemContext, placeholderRegex);

                            // Shading
                            if (oddColor != null && (idx + 1) % 2 != 0) {
                                setRowShading(clonedRow, oddColor.toString());
                            } else if (evenColor != null && (idx + 1) % 2 == 0) {
                                setRowShading(clonedRow, evenColor.toString());
                            }

                            parent.insertBefore(clonedRow, row);
                        }
                        parent.removeChild(row);
                    } else {
                        parent.removeChild(row);
                    }
                }
            }
        }
    }

    private void replaceRowPlaceholders(Element row, Map<String, Object> context, Pattern placeholderRegex) {
        List<Element> paragraphs = getElementsByTagName(row, "p");
        for (Element p : paragraphs) {
            List<Element> tNodes = getElementsByTagName(p, "t");
            if (tNodes.isEmpty()) continue;

            StringBuilder combined = new StringBuilder();
            for (Element t : tNodes) {
                combined.append(t.getTextContent());
            }

            String s = combined.toString().replaceAll("(?i)«table\\([^)]+\\)»", "").replaceAll("(?i)«endtable»", "");
            Matcher matcher = placeholderRegex.matcher(s);
            if (matcher.find()) {
                StringBuffer sb = new StringBuffer();
                matcher.reset();
                while (matcher.find()) {
                    String expr = matcher.group(1);
                    Object val = evaluateExpression(expr, context);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(val != null ? val.toString() : ""));
                }
                matcher.appendTail(sb);

                tNodes.get(0).setTextContent(sb.toString());
                for (int k = 1; k < tNodes.size(); k++) {
                    tNodes.get(k).setTextContent("");
                }
            }
        }
    }

    private void processElementLoops(Element container, Object data, Document doc) {
        Pattern loopRegex = Pattern.compile("«loop\\(([^)]+)\\)»", Pattern.CASE_INSENSITIVE);
        Pattern endloopRegex = Pattern.compile("«endloop»", Pattern.CASE_INSENSITIVE);
        Pattern placeholderRegex = Pattern.compile("«([^»]+)»");

        NodeList children = container.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                i++;
                continue;
            }

            Element el = (Element) child;
            String text = el.getTextContent();
            Matcher loopMatch = loopRegex.matcher(text);

            if (loopMatch.find()) {
                String loopPath = loopMatch.group(1).trim();
                List<Element> loopElements = new ArrayList<>();
                int depth = 1;
                int j = i + 1;

                while (j < children.getLength()) {
                    Node nextChild = children.item(j);
                    if (nextChild.getNodeType() == Node.ELEMENT_NODE) {
                        Element nextEl = (Element) nextChild;
                        String nextText = nextEl.getTextContent();
                        if (loopRegex.matcher(nextText).find()) depth++;
                        if (endloopRegex.matcher(nextText).find()) {
                            depth--;
                            if (depth == 0) break;
                        }
                        loopElements.add(nextEl);
                    }
                    j++;
                }

                Object items = TokenResolver.resolve(data, loopPath);
                Node endloopChild = j < children.getLength() ? children.item(j) : null;

                if (items instanceof List && !((List<?>) items).isEmpty()) {
                    List<?> list = (List<?>) items;
                    for (int idx = 0; idx < list.size(); idx++) {
                        Object item = list.get(idx);
                        Map<String, Object> itemContext = new HashMap<>();
                        if (item instanceof Map) {
                            itemContext.putAll((Map<String, Object>) item);
                        } else {
                            itemContext.put("item", item);
                        }
                        itemContext.put("_index", idx + 1);
                        itemContext.put("_first", idx == 0);
                        itemContext.put("_last", idx == list.size() - 1);
                        itemContext.put("_even", (idx + 1) % 2 == 0);
                        itemContext.put("_odd", (idx + 1) % 2 != 0);

                        for (Element loopEl : loopElements) {
                            Element clonedEl = (Element) loopEl.cloneNode(true);
                            processElementLoops(clonedEl, itemContext, doc);

                            List<Element> pList = clonedEl.getLocalName().equals("p")
                                    ? Collections.singletonList(clonedEl)
                                    : getElementsByTagName(clonedEl, "p");

                            for (Element p : pList) {
                                List<Element> tNodes = getElementsByTagName(p, "t");
                                if (tNodes.isEmpty()) continue;
                                StringBuilder combined = new StringBuilder();
                                for (Element t : tNodes) combined.append(t.getTextContent());

                                Matcher m = placeholderRegex.matcher(combined.toString());
                                if (m.find()) {
                                    StringBuffer sb = new StringBuffer();
                                    m.reset();
                                    while (m.find()) {
                                        Object val = evaluateExpression(m.group(1), itemContext);
                                        m.appendReplacement(sb, Matcher.quoteReplacement(val != null ? val.toString() : ""));
                                    }
                                    m.appendTail(sb);
                                    tNodes.get(0).setTextContent(sb.toString());
                                    for (int k = 1; k < tNodes.size(); k++) tNodes.get(k).setTextContent("");
                                }
                            }

                            container.insertBefore(clonedEl, el);
                        }
                    }
                }

                container.removeChild(el);
                for (Element loopEl : loopElements) {
                    if (loopEl.getParentNode() == container) container.removeChild(loopEl);
                }
                if (endloopChild != null && endloopChild.getParentNode() == container) {
                    container.removeChild(endloopChild);
                }

                processElementLoops(container, data, doc);
                return;
            }

            i++;
        }
    }

    private Object evaluateExpression(String expr, Object context) {
        ExpressionParser.ParsedExpression parsed = ExpressionParser.parse(expr);
        Object rawVal = parsed.fieldPath != null && !parsed.fieldPath.isEmpty()
                ? TokenResolver.resolve(context, parsed.fieldPath) : null;

        if (parsed.funcName.isEmpty()) return rawVal;

        IEvaluator evaluator = evaluators.get(parsed.funcName);
        if (evaluator == null) return rawVal;

        return evaluator.evaluate(rawVal, parsed.args);
    }

    private void setRowShading(Element row, String colorHex) {
        String clean = colorHex.replace("#", "");
        List<Element> cells = getElementsByTagName(row, "tc");
        Document doc = row.getOwnerDocument();

        for (Element cell : cells) {
            Element tcPr = getFirstChild(cell, "tcPr");
            if (tcPr == null) {
                tcPr = doc.createElementNS(W_NS, "w:tcPr");
                cell.insertBefore(tcPr, cell.getFirstChild());
            }

            Element shd = getFirstChild(tcPr, "shd");
            if (shd == null) {
                shd = doc.createElementNS(W_NS, "w:shd");
                tcPr.appendChild(shd);
            }

            shd.setAttributeNS(W_NS, "w:val", "clear");
            shd.setAttributeNS(W_NS, "w:color", "auto");
            shd.setAttributeNS(W_NS, "w:fill", clean);
        }
    }

    private void injectWatermark(Map<String, byte[]> zipEntries, ExportOptions options) {
        String cleanColor = options.getWatermarkColor().replace("#", "");
        String escaped = options.getWatermarkText().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String headerXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<w:hdr xmlns:w=\"" + W_NS + "\" xmlns:v=\"" + V_NS + "\">\n" +
                "  <w:p>\n" +
                "    <w:pPr><w:pStyle w:val=\"Header\"/></w:pPr>\n" +
                "    <w:r>\n" +
                "      <w:pict>\n" +
                "        <v:shape id=\"WordTemplaterWatermark\" type=\"#_x0000_t136\"\n" +
                "          style=\"position:absolute;left:0;text-align:left;margin-left:0;margin-top:0;width:412.5pt;height:247.5pt;rotation:" + (int) options.getWatermarkAngle() + ";z-index:-251654144;mso-position-horizontal:center;mso-position-horizontal-relative:margin;mso-position-vertical:center;mso-position-vertical-relative:margin\"\n" +
                "          fillcolor=\"#" + cleanColor + "\" stroked=\"f\">\n" +
                "          <v:fill opacity=\"0.3\"/>\n" +
                "          <v:textpath style=\"font-family:'" + options.getWatermarkFontName() + "';font-size:" + (int) options.getWatermarkFontSize() + "pt\" string=\"" + escaped + "\"/>\n" +
                "        </v:shape>\n" +
                "      </w:pict>\n" +
                "    </w:r>\n" +
                "  </w:p>\n" +
                "</w:hdr>";

        zipEntries.put("word/header1.xml", headerXml.getBytes(StandardCharsets.UTF_8));
    }

    private void cleanNodes(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS(W_NS, localName);
        for (int i = nodes.getLength() - 1; i >= 0; i--) {
            Node n = nodes.item(i);
            if (n.getParentNode() != null) {
                n.getParentNode().removeChild(n);
            }
        }
    }

    private List<Element> getElementsByTagName(Element parent, String localName) {
        List<Element> list = new ArrayList<>();
        NodeList nl = parent.getElementsByTagNameNS(W_NS, localName);
        for (int i = 0; i < nl.getLength(); i++) {
            list.add((Element) nl.item(i));
        }
        return list;
    }

    private Element getFirstChild(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && localName.equalsIgnoreCase(n.getLocalName())) {
                return (Element) n;
            }
        }
        return null;
    }
}
