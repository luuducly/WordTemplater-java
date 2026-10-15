package com.github.luuducly.wordtemplater;

public class ExportOptions {
    private String watermarkText;
    private String watermarkColor = "C0C0C0";
    private double watermarkAngle = -45;
    private double watermarkFontSize = 72;
    private String watermarkFontName = "Calibri";
    private String tableOddRowColor;
    private String tableEvenRowColor;

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    public String getWatermarkColor() {
        return watermarkColor;
    }

    public void setWatermarkColor(String watermarkColor) {
        this.watermarkColor = watermarkColor;
    }

    public double getWatermarkAngle() {
        return watermarkAngle;
    }

    public void setWatermarkAngle(double watermarkAngle) {
        this.watermarkAngle = watermarkAngle;
    }

    public double getWatermarkFontSize() {
        return watermarkFontSize;
    }

    public void setWatermarkFontSize(double watermarkFontSize) {
        this.watermarkFontSize = watermarkFontSize;
    }

    public String getWatermarkFontName() {
        return watermarkFontName;
    }

    public void setWatermarkFontName(String watermarkFontName) {
        this.watermarkFontName = watermarkFontName;
    }

    public String getTableOddRowColor() {
        return tableOddRowColor;
    }

    public void setTableOddRowColor(String tableOddRowColor) {
        this.tableOddRowColor = tableOddRowColor;
    }

    public String getTableEvenRowColor() {
        return tableEvenRowColor;
    }

    public void setTableEvenRowColor(String tableEvenRowColor) {
        this.tableEvenRowColor = tableEvenRowColor;
    }
}
