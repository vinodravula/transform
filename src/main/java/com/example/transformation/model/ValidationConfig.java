package com.example.transformation.model;

public class ValidationConfig {
    private String countSourceField; // Name of the field in TailConfig.fields that gets its value from XML and represents the count
    private String expectedCountAlias; // Alias from processingContext (e.g., BodyConfig.countFieldAlias) for the actual count

    public String getCountSourceField() {
        return countSourceField;
    }

    public void setCountSourceField(String countSourceField) {
        this.countSourceField = countSourceField;
    }

    public String getExpectedCountAlias() {
        return expectedCountAlias;
    }

    public void setExpectedCountAlias(String expectedCountAlias) {
        this.expectedCountAlias = expectedCountAlias;
    }
}
