# WordTemplater-java

[![Java CI](https://github.com/luuducly/WordTemplater-java/actions/workflows/ci.yml/badge.svg)](https://github.com/luuducly/WordTemplater-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-v1.0.0-blue.svg?style=flat-square)](https://central.sonatype.com/)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg?style=flat-square)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

**WordTemplater-java** is a high-performance, robust Java document template engine for Microsoft Word (`.docx`) files.

Design your templates visually in Microsoft Word using standard **MergeFields** or `«placeholder»` tags, and populate them with Java objects, POJOs, maps, or Jackson JsonNodes in milliseconds.

---

## Key Features

- ⚡ **High Performance & Low Memory**: Streams and parses `.docx` OpenXML packages directly via standard DOM & ZIP handling.
- 🧭 **Deep Object Navigation**: Access nested fields (`customer.address.city`), POJO getters, Map keys, and List indices (`items[0].name`) case-insensitively.
- 🔁 **Nested Loops & Hierarchical Blocks**: Repeat paragraphs and complex nested blocks (`«loop(items)»`...`«endloop»`).
- 📊 **Table Row Cloning & Alternating Shading**: Clone table rows with built-in zebra striping (`odd_color`, `even_color`).
- 🔢 **Loop Metadata**: Automatic contextual variables: `_index` (1-based), `_first`, `_last`, `_even`, `_odd`.
- 🛡️ **Dynamic Watermarking**: Add customizable angled text watermarks to document headers.
- 🧩 **Extensible Evaluator Architecture**: Create and register custom evaluators and formatters with simple functional interfaces.

---

## Multi-Language Ecosystem

WordTemplater is available across multiple programming languages with matching template syntax and feature parity:

| Language | Repository | Package / Install |
| :--- | :--- | :--- |
| **Java** *(This repository)* | [WordTemplater-java](https://github.com/luuducly/WordTemplater-java) | `com.github.luuducly:wordtemplater` |
| **C# (.NET)** | [WordTemplater](https://github.com/luuducly/WordTemplater) | `dotnet add package WordTemplater` |
| **TypeScript / JavaScript** | [WordTemplater-ts](https://github.com/luuducly/WordTemplater-ts) | `npm install wordtemplater-ts` |
| **Python** | [WordTemplater-python](https://github.com/luuducly/WordTemplater-python) | `pip install wordtemplater` |

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.github.luuducly</groupId>
    <artifactId>wordtemplater</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github.luuducly:wordtemplater:1.0.0'
```

---

## Quick Start

```java
import com.github.luuducly.wordtemplater.WordTemplate;
import com.github.luuducly.wordtemplater.ExportOptions;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        // 1. Prepare data (POJO, Map, or JsonNode)
        Map<String, Object> data = new HashMap<>();
        data.put("invoiceNumber", "INV-2026-001");
        
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Acme Corporation");
        customer.put("address", Map.of("city", "San Francisco", "country", "USA"));
        data.put("customer", customer);

        List<Map<String, Object>> items = List.of(
            Map.of("description", "Cloud Infrastructure", "qty", 1, "price", 1200.00),
            Map.of("description", "SSL Certificate", "qty", 2, "price", 25.00)
        );
        data.put("items", items);

        // 2. Load template
        WordTemplate template = WordTemplate.load("templates/Invoice.docx");

        // 3. Configure options (watermark, zebra striping)
        ExportOptions options = new ExportOptions();
        options.setWatermarkText("CONFIDENTIAL");
        options.setWatermarkColor("DC2626"); // Red
        options.setWatermarkAngle(-45);
        options.setWatermarkFontSize(72);
        options.setTableZebraStriping(true);
        options.setZebraOddColor("F8FAFC");
        options.setZebraEvenColor("FFFFFF");

        // 4. Export
        template.export(data, "output/Invoice.docx", options);
    }
}
```

---

## Template Syntax & Built-in Evaluators

| Evaluator Syntax | Description | Example Input | Example Output |
| :--- | :--- | :--- | :--- |
| `«upper(name)»` | Converts text to uppercase | `"acme corp"` | `ACME CORP` |
| `«lower(code)»` | Converts text to lowercase | `"PROD-XYZ"` | `prod-xyz` |
| `«trim(note)»` | Removes whitespace | `"  hello  "` | `hello` |
| `«sub(code, 0, 4)»` | Substring `(start, length)` | `"WORDTEMPLATER"` | `WORD` |
| `«left(code, 3)»` | First `N` characters | `"INV-9988"` | `INV` |
| `«right(code, 4)»` | Last `N` characters | `"INV-9988"` | `9988` |
| `«replace(text, '-', '/')»` | Replaces occurrences | `"2026-10-15"` | `2026/10/15` |
| `«padleft(num, 6, '0')»` | Left-pads string to width | `42` | `000042` |
| `«padright(code, 8, '_')»` | Right-pads string to width | `"A1"` | `A1______` |
| `«currency(amount, 'en-US')»` | Currency with locale format | `1500000` | `$1,500,000.00` |
| `«currency(amount, 'vi-VN')»` | Vietnamese Dong currency | `1500000` | `1.500.000 ₫` |
| `«number(count)»` | Number with thousand separator | `1234567` | `1,234,567` |
| `«percentage(rate)»` | Number formatted as percentage | `0.85` | `85.00%` |
| `«date(createdAt, 'dd/MM/yyyy')»` | Formats date with pattern | `Date` / `Temporal` | `15/10/2026` |
| `«default(phone, 'N/A')»` | Fallback if null or empty | `null` | `N/A` |
| `«if(isVip, 'Gold', 'Standard')»` | Conditional text based on boolean | `true` | `Gold` |

---

## Repeating Tables & Zebra Shading

To repeat rows inside Word tables:

| No. | Description | Qty | Unit Price |
| :--- | :--- | :--- | :--- |
| `«table(items)»«_index»` | `«description»` | `«qty»` | `«currency(price)»«endtable»` |

In-tag shading parameter:
```text
«table(items, 'F8FAFC', 'FFFFFF')»
```

---

## Custom Evaluator Example

```java
import com.github.luuducly.wordtemplater.IEvaluator;
import com.github.luuducly.wordtemplater.WordTemplate;
import java.util.List;

public class SlugifyEvaluator implements IEvaluator {
    @Override
    public Object evaluate(Object value, List<String> parameters) {
        if (value == null) return "";
        return value.toString().toLowerCase().trim().replaceAll("\\s+", "-");
    }
}

// Registration:
template.registerEvaluator("slugify", new SlugifyEvaluator());
```

In Word template:
```text
«slugify(productTitle)»
```

---

## License

MIT License. Copyright (c) 2026 Lưu Đức Ly.
