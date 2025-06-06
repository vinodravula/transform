package com.example.transformation.model;

import java.util.List;

public class TailConfig {
    private List<FieldConfig> fields;
    private ValidationConfig validationConfig; // For validating body count against a tail field

    public List<FieldConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }

    public ValidationConfig getValidationConfig() {
        return validationConfig;
    }

    public void setValidationConfig(ValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
    }
}
