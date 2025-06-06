package com.example.transformation.model;

import java.util.List;
import java.util.Map;

public class FlatFileConfig {
    private String type; // "fixed-width" or "delimited"
    private String delimiter; // e.g., ",", "|", etc. (only for delimited)

    // Top-level fields: Used if no H-B-T structure is defined, or for overall file properties.
    // If BodyConfig is used, these top-level fields are generally for defining the *order* and *fixed-width lengths*
    // for fields that appear in header, body, or tail lines. Their XPaths might be ignored if HBT is active.
    private List<FieldConfig> fields;

    private HeaderConfig headerConfig;
    private BodyConfig bodyConfig;
    private TailConfig tailConfig;
    // Removed: private RepeatingElementConfig repeatingElementConfig; (Replaced by BodyConfig)


    public static class FieldConfig {
        private String name;
        private int length; // Only for fixed-width
        private String sourceXpath; // Default XPath if no condition or condition not met (and no specific not-met value)
        private String placeholderValue; // Default/placeholder if XPath fails or not provided

        // Conditional Mapping Fields
        private String conditionSourceXpath;
        private String conditionExpectedValue;
        private String valueIfConditionMetXpath;
        private String valueIfConditionMetConstant;
        private String valueIfConditionNotMetXpath; // Fallback if condition not met
        private String valueIfConditionNotMetConstant; // Fallback constant if condition not met

        private List<StringOperationConfig> stringOperations; // List of string operations

        // Date Formatting Fields
        private String sourceDateFormat;
        private String targetDateFormat;

        // One-to-many (split) configuration
        private SplitConfig splitConfig;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public String getSourceXpath() {
            return sourceXpath;
        }

        public void setSourceXpath(String sourceXpath) {
            this.sourceXpath = sourceXpath;
        }

        public String getPlaceholderValue() {
            return placeholderValue;
        }

        public void setPlaceholderValue(String placeholderValue) {
            this.placeholderValue = placeholderValue;
        }

        // Getters and Setters for conditional fields

        public String getConditionSourceXpath() {
            return conditionSourceXpath;
        }

        public void setConditionSourceXpath(String conditionSourceXpath) {
            this.conditionSourceXpath = conditionSourceXpath;
        }

        public String getConditionExpectedValue() {
            return conditionExpectedValue;
        }

        public void setConditionExpectedValue(String conditionExpectedValue) {
            this.conditionExpectedValue = conditionExpectedValue;
        }

        public String getValueIfConditionMetXpath() {
            return valueIfConditionMetXpath;
        }

        public void setValueIfConditionMetXpath(String valueIfConditionMetXpath) {
            this.valueIfConditionMetXpath = valueIfConditionMetXpath;
        }

        public String getValueIfConditionMetConstant() {
            return valueIfConditionMetConstant;
        }

        public void setValueIfConditionMetConstant(String valueIfConditionMetConstant) {
            this.valueIfConditionMetConstant = valueIfConditionMetConstant;
        }

        public String getValueIfConditionNotMetXpath() {
            return valueIfConditionNotMetXpath;
        }

        public void setValueIfConditionNotMetXpath(String valueIfConditionNotMetXpath) {
            this.valueIfConditionNotMetXpath = valueIfConditionNotMetXpath;
        }

        public String getValueIfConditionNotMetConstant() {
            return valueIfConditionNotMetConstant;
        }

        public void setValueIfConditionNotMetConstant(String valueIfConditionNotMetConstant) {
            this.valueIfConditionNotMetConstant = valueIfConditionNotMetConstant;
        }

        public List<StringOperationConfig> getStringOperations() {
            return stringOperations;
        }

        public void setStringOperations(List<StringOperationConfig> stringOperations) {
            this.stringOperations = stringOperations;
        }

        public String getSourceDateFormat() {
            return sourceDateFormat;
        }

        public void setSourceDateFormat(String sourceDateFormat) {
            this.sourceDateFormat = sourceDateFormat;
        }

        public String getTargetDateFormat() {
            return targetDateFormat;
        }

        public void setTargetDateFormat(String targetDateFormat) {
            this.targetDateFormat = targetDateFormat;
        }

        public SplitConfig getSplitConfig() {
            return splitConfig;
        }

        public void setSplitConfig(SplitConfig splitConfig) {
            this.splitConfig = splitConfig;
        }
    }

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
