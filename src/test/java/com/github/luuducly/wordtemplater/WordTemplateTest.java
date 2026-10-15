package com.github.luuducly.wordtemplater;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class WordTemplateTest {

    @Test
    public void testInMemoryTemplateExport() throws Exception {
        String docXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n" +
                "  <w:body>\n" +
                "    <w:p><w:r><w:t>«upper(Customer.Name)»</w:t></w:r></w:p>\n" +
                "    <w:p><w:r><w:t>«currency(Total, 'vi-VN')»</w:t></w:r></w:p>\n" +
                "    <w:tbl>\n" +
                "      <w:tr>\n" +
                "        <w:tc><w:p><w:r><w:t>«table(Items, 'F0F0F0', 'FFFFFF')»«_index»</w:t></w:r></w:p></w:tc>\n" +
                "        <w:tc><w:p><w:r><w:t>«Description»«endtable»</w:t></w:r></w:p></w:tc>\n" +
                "      </w:tr>\n" +
                "    </w:tbl>\n" +
                "  </w:body>\n" +
                "</w:document>";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            zos.write(docXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        WordTemplate template = WordTemplate.load(baos.toByteArray());
        template.setWatermark("CONFIDENTIAL", "C0C0C0", -45, 60);

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();
        customer.put("Name", "Google LLC");
        data.put("Customer", customer);
        data.put("Total", 1500000);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("Description", "Cloud Workspace");
        items.add(item1);
        Map<String, Object> item2 = new HashMap<>();
        item2.put("Description", "Domain Name");
        items.add(item2);
        data.put("Items", items);

        byte[] output = template.export(data);
        assertNotNull(output);
        assertTrue(output.length > 0);

        // Read exported xml
        String exportedXml = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(output))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(e.getName())) {
                    ByteArrayOutputStream eBaos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = zis.read(buf)) != -1) {
                        eBaos.write(buf, 0, r);
                    }
                    exportedXml = eBaos.toString(StandardCharsets.UTF_8);
                }
            }
        }

        assertNotNull(exportedXml);
        assertTrue(exportedXml.contains("GOOGLE LLC"));
        assertTrue(exportedXml.contains("1.500.000"));
        assertTrue(exportedXml.contains("Cloud Workspace"));
        assertTrue(exportedXml.contains("Domain Name"));
    }
}
