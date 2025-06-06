package com.example.transformation.model;

import java.util.List;

public class RepeatingElementConfig {
    private String baseXpath; // XPath to select the list of repeating parent nodes
    private List<FieldConfig> fields; // Field definitions, XPaths here are relative to a node selected by baseXpath

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
}
