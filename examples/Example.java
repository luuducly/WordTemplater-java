package com.github.luuducly.wordtemplater.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luuducly.wordtemplater.WordTemplate;

import java.io.File;
import java.io.FileOutputStream;

public class Example {
    public static void main(String[] args) throws Exception {
        File templateFile = new File("examples/templates/Template.docx");
        File dataFile = new File("examples/Data.json");
        File outputFile = new File("examples/Output.docx");

        if (!templateFile.exists() || !dataFile.exists()) {
            System.out.println("Template or Data file not found.");
            return;
        }

        System.out.println("Loading template: " + templateFile.getAbsolutePath());
        WordTemplate template = WordTemplate.load(templateFile);

        template.setWatermark("CONFIDENTIAL", "C0C0C0", -45, 72);
        template.enableTableZebraStriping("F9FAFB", "FFFFFF");

        ObjectMapper mapper = new ObjectMapper();
        Object data = mapper.readValue(dataFile, Object.class);

        System.out.println("Exporting document with data...");
        byte[] docxBytes = template.export(data);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(docxBytes);
        }

        System.out.println("Successfully generated: " + outputFile.getAbsolutePath());
        System.out.println("File size: " + docxBytes.length + " bytes");
    }
}
