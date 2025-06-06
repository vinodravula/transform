package com.example.transformation.model;

import java.util.List;

public class SplitConfig {
    private String delimiter; // Regex or simple string for splitting
    private List<String> targetFieldNames; // Logical names of fields to populate from split parts

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public List<String> getTargetFieldNames() {
        return targetFieldNames;
    }

    public void setTargetFieldNames(List<String> targetFieldNames) {
        this.targetFieldNames = targetFieldNames;
    }
}
