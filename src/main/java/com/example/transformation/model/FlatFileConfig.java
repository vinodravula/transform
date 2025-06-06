package com.example.transformation.model;

import java.util.List;
// Removed: import java.util.Map; as it's not used directly in this class after FieldConfig extraction

public class FlatFileConfig {
    private String type; // "fixed-width" or "delimited"
    private String delimiter; // e.g., ",", "|", etc. (only for delimited)

    // Top-level fields: Used if no H-B-T structure is defined, or for overall file properties.
    // If BodyConfig is used, these top-level fields are generally for defining the *order* and *fixed-width lengths*
    // for fields that appear in header, body, or tail lines. Their XPaths might be ignored if HBT is active.
    private List<FieldConfig> fields; // FieldConfig is now an external class

    private HeaderConfig headerConfig;
    private BodyConfig bodyConfig;
    private TailConfig tailConfig;

    // FieldConfig inner class has been removed from here

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public List<FieldConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }

    public HeaderConfig getHeaderConfig() {
        return headerConfig;
    }

    public void setHeaderConfig(HeaderConfig headerConfig) {
        this.headerConfig = headerConfig;
    }

    public BodyConfig getBodyConfig() {
        return bodyConfig;
    }

    public void setBodyConfig(BodyConfig bodyConfig) {
        this.bodyConfig = bodyConfig;
    }

    public TailConfig getTailConfig() {
        return tailConfig;
    }

    public void setTailConfig(TailConfig tailConfig) {
        this.tailConfig = tailConfig;
    }
}
