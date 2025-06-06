package com.example.transformation.model;

import java.util.List;

public class BodyConfig {
    private String baseXpath; // XPath to select the list of repeating body elements
    private List<FieldConfig> fields; // Field definitions for each body element
    private String countFieldAlias; // Alias to store the count of processed body items

    public String getBaseXpath() {
        return baseXpath;
    }

    public void setBaseXpath(String baseXpath) {
        this.baseXpath = baseXpath;
    }

    public List<FieldConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }

    public String getCountFieldAlias() {
        return countFieldAlias;
    }

    public void setCountFieldAlias(String countFieldAlias) {
        this.countFieldAlias = countFieldAlias;
    }
}
