package com.example.transformation.model;

public class StringOperationConfig {
    private String type; // e.g., "CONCATENATE", "TRIM", "UPPERCASE", "LOWERCASE", "SUBSTRING"
    private String concatValue; // For CONCATENATE (constant)
    private String concatValueFromXpath; // For CONCATENATE (dynamic)
    private int substringStartIndex; // For SUBSTRING
    private int substringEndIndex; // For SUBSTRING

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConcatValue() {
        return concatValue;
    }

    public void setConcatValue(String concatValue) {
        this.concatValue = concatValue;
    }

    public String getConcatValueFromXpath() {
        return concatValueFromXpath;
    }

    public void setConcatValueFromXpath(String concatValueFromXpath) {
        this.concatValueFromXpath = concatValueFromXpath;
    }

    public int getSubstringStartIndex() {
        return substringStartIndex;
    }

    public void setSubstringStartIndex(int substringStartIndex) {
        this.substringStartIndex = substringStartIndex;
    }

    public int getSubstringEndIndex() {
        return substringEndIndex;
    }

    public void setSubstringEndIndex(int substringEndIndex) {
        this.substringEndIndex = substringEndIndex;
    }
}
