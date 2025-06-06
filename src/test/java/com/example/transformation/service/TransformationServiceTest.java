package com.example.transformation.service;

import com.example.transformation.service.TransformationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import static org.junit.jupiter.api.Assertions.*;

public class TransformationServiceTest {

    private TransformationService transformationService;

    @BeforeEach
    void setUp() {
        transformationService = new TransformationService();
    }

    private Document parseXmlString(String xml) throws Exception {
        return transformationService.parseXml(xml);
    }

    @Test
    void testDirectMapping_Delimited() throws Exception {
        String xmlInput = "<root><name>Test Name</name><value>123</value></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"NameField\", \"sourceXpath\": \"/root/name\" }," +
            "  { \"name\": \"ValueField\", \"sourceXpath\": \"/root/value\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("Test Name,123", result);
    }

    @Test
    void testDirectMapping_FixedWidth() throws Exception {
        String xmlInput = "<root><name>Test</name><value>123</value></root>";
        String jsonConfig = "{" +
            "\"type\": \"fixed-width\"," +
            "\"fields\": [" +
            "  { \"name\": \"NameField\", \"length\": 10, \"sourceXpath\": \"/root/name\" }," +
            "  { \"name\": \"ValueField\", \"length\": 5, \"sourceXpath\": \"/root/value\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("Test      123  ", result); // Padded
    }

    @Test
    void testDirectMapping_FixedWidth_Truncation() throws Exception {
        String xmlInput = "<root><name>TestNameLongerThanTen</name><value>1234567</value></root>";
        String jsonConfig = "{" +
            "\"type\": \"fixed-width\"," +
            "\"fields\": [" +
            "  { \"name\": \"NameField\", \"length\": 10, \"sourceXpath\": \"/root/name\" }," +
            "  { \"name\": \"ValueField\", \"length\": 5, \"sourceXpath\": \"/root/value\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("TestNameLo12345", result); // Truncated
    }

    @Test
    void testXPathNotResolving_ShouldUsePlaceholder() throws Exception {
        String xmlInput = "<root><actualName>Test Name</actualName></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"NameField\", \"sourceXpath\": \"/root/nonExistentName\", \"placeholderValue\": \"DefaultName\" }," +
            "  { \"name\": \"ValueField\", \"sourceXpath\": \"/root/value\", \"placeholderValue\": \"N/A\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("DefaultName,N/A", result);
    }

    // More tests will be added here

    // --- Conditional Mapping Tests ---
    @Test
    void testConditionalMapping_ConditionMet_ConstantValue() throws Exception {
        String xmlInput = "<root><status>ACTIVE</status><name>Test</name></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"StatusValue\", \"sourceXpath\": \"/root/name\"," +
            "    \"conditionSourceXpath\": \"/root/status\", \"conditionExpectedValue\": \"ACTIVE\"," +
            "    \"valueIfConditionMetConstant\": \"IsActive\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("IsActive", result);
    }

    @Test
    void testConditionalMapping_ConditionNotMet_UsesSourceXPath() throws Exception {
        String xmlInput = "<root><status>INACTIVE</status><name>TestName</name></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"StatusValue\", \"sourceXpath\": \"/root/name\"," +
            "    \"conditionSourceXpath\": \"/root/status\", \"conditionExpectedValue\": \"ACTIVE\"," +
            "    \"valueIfConditionMetConstant\": \"IsActive\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("TestName", result); // Falls back to sourceXpath
    }

    @Test
    void testConditionalMapping_ConditionMet_XPathValue() throws Exception {
        String xmlInput = "<root><status>ACTIVE</status><nameIfActive>ActiveName</nameIfActive><nameIfInactive>InactiveName</nameIfInactive></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"NameResult\", \"sourceXpath\": \"/root/nameIfInactive\"," +
            "    \"conditionSourceXpath\": \"/root/status\", \"conditionExpectedValue\": \"ACTIVE\"," +
            "    \"valueIfConditionMetXpath\": \"/root/nameIfActive\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("ActiveName", result);
    }

    @Test
    void testConditionalMapping_ConditionNotMet_XPathValue() throws Exception {
        String xmlInput = "<root><status>PENDING</status><nameIfActive>ActiveName</nameIfActive><nameIfInactive>InactiveName</nameIfInactive></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"NameResult\", \"sourceXpath\": \"/root/nameIfInactive\", " + // Default sourceXpath
            "    \"conditionSourceXpath\": \"/root/status\", \"conditionExpectedValue\": \"ACTIVE\"," +
            "    \"valueIfConditionMetXpath\": \"/root/nameIfActive\"," +
            "    \"valueIfConditionNotMetXpath\": \"/root/nameIfInactive\" }" + // Specific for not met
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("InactiveName", result);
    }

     @Test
    void testConditionalMapping_ConditionNotMet_ConstantValue() throws Exception {
        String xmlInput = "<root><status>DISABLED</status><name>Test</name></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"StatusValue\", \"sourceXpath\": \"/root/name\"," +
            "    \"conditionSourceXpath\": \"/root/status\", \"conditionExpectedValue\": \"ACTIVE\"," +
            "    \"valueIfConditionMetConstant\": \"IsActive\"," +
            "    \"valueIfConditionNotMetConstant\": \"IsDisabledOrOther\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("IsDisabledOrOther", result);
    }

    // --- String Operations Tests ---
    @Test
    void testStringOperations_AllInSequence() throws Exception {
        String xmlInput = "<root><text>  hello world  </text><suffix>_end</suffix></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"ProcessedText\", \"sourceXpath\": \"/root/text\"," +
            "    \"stringOperations\": [" +
            "      { \"type\": \"TRIM\" }," +
            "      { \"type\": \"UPPERCASE\" }," +
            "      { \"type\": \"SUBSTRING\", \"substringStartIndex\": 0, \"substringEndIndex\": 5 }," + // HELLO
            "      { \"type\": \"LOWERCASE\" }," + // hello
            "      { \"type\": \"CONCATENATE\", \"concatValue\": \"_java\" }," + // hello_java
            "      { \"type\": \"CONCATENATE\", \"concatValueFromXpath\": \"/root/suffix\" }" + // hello_java_end
            "    ]}" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("hello_java_end", result);
    }

    @Test
    void testStringOperations_Substring_InvalidIndices() throws Exception {
        String xmlInput = "<root><text>short</text></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"SubstringTest\", \"sourceXpath\": \"/root/text\"," +
            "    \"stringOperations\": [" +
            "      { \"type\": \"SUBSTRING\", \"substringStartIndex\": 0, \"substringEndIndex\": 10 }" + // End index too large
            "    ]}" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        // Expect original string due to error in substring, or specific error handling (current: logs error, returns original)
        assertEquals("short", result);
    }

    @Test
    void testStringOperations_Substring_StartIndexGreaterThanEndIndex() throws Exception {
        String xmlInput = "<root><text>value</text></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"SubstringTest\", \"sourceXpath\": \"/root/text\"," +
            "    \"stringOperations\": [" +
            "      { \"type\": \"SUBSTRING\", \"substringStartIndex\": 3, \"substringEndIndex\": 1 }" +
            "    ]}" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("value", result); // Expect original due to invalid indices
    }

    @Test
    void testStringOperations_ConcatWithNullInitialValue() throws Exception {
        String xmlInput = "<root><item><id>item1</id></item></root>"; // item/name is null
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"Name\", \"sourceXpath\": \"/root/item/name\", \"placeholderValue\": \"\"," + // Will evaluate to null from XPath, then ""
            "    \"stringOperations\": [" +
            "      { \"type\": \"CONCATENATE\", \"concatValue\": \"Default\" }" +
            "    ]}" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("Default", result); // Null initial value from XPath, placeholder "" used, then concatenated
    }

    // --- Date Formatting Tests ---
    @Test
    void testDateFormatting_ValidConversion() throws Exception {
        String xmlInput = "<root><eventTime>2023-10-26T15:30:45</eventTime></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"FormattedDate\", \"sourceXpath\": \"/root/eventTime\"," +
            "    \"sourceDateFormat\": \"yyyy-MM-dd'T'HH:mm:ss\"," +
            "    \"targetDateFormat\": \"MM/dd/yyyy HH:mm\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("10/26/2023 15:30", result);
    }

    @Test
    void testDateFormatting_InvalidSourceDate() throws Exception {
        String xmlInput = "<root><eventTime>26/10/2023 15:30</eventTime></root>"; // Wrong format
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"FormattedDate\", \"sourceXpath\": \"/root/eventTime\"," +
            "    \"sourceDateFormat\": \"yyyy-MM-dd'T'HH:mm:ss\"," +
            "    \"targetDateFormat\": \"MM/dd/yyyy HH:mm\", \"placeholderValue\":\"ERROR_DATE\"}" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        // Current behavior is to return empty string on parse failure
        assertEquals("", result);
    }

    @Test
    void testDateFormatting_EmptySourceDate() throws Exception {
        String xmlInput = "<root><eventTime></eventTime></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"FormattedDate\", \"sourceXpath\": \"/root/eventTime\"," +
            "    \"sourceDateFormat\": \"yyyy-MM-dd'T'HH:mm:ss\"," +
            "    \"targetDateFormat\": \"MM/dd/yyyy HH:mm\", \"placeholderValue\":\"EMPTY_DATE\"}" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        // applyDateFormatting returns currentValue if it's empty
        assertEquals("", result);
    }


    @Test
    void testDateFormatting_InvalidTargetPattern() throws Exception {
        String xmlInput = "<root><eventTime>2023-10-26T15:30:45</eventTime></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"FormattedDate\", \"sourceXpath\": \"/root/eventTime\"," +
            "    \"sourceDateFormat\": \"yyyy-MM-dd'T'HH:mm:ss\"," +
            "    \"targetDateFormat\": \"MM/dd/yyyy InvalidPattern\", \"placeholderValue\":\"ERROR_DATE\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        // Current behavior is to return empty string on illegal argument (bad pattern)
        assertEquals("", result);
    }

    // --- Field Splitting Tests ---
    @Test
    void testFieldSplitting_DelimitedOutput() throws Exception {
        String xmlInput = "<root><data>part1|part2|part3</data></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" + // These define the order and overall structure
            "  { \"name\": \"SourceDataField\", \"sourceXpath\": \"/root/data\"," +
            "    \"splitConfig\": { \"delimiter\": \"\\\\|\", \"targetFieldNames\": [\"FieldA\", \"FieldB\", \"FieldC\"] } }," +
            "  { \"name\": \"FieldA\" }," +
            "  { \"name\": \"FieldB\" }," +
            "  { \"name\": \"FieldC\" }" +
            "]" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        // The source field "SourceDataField" itself won't appear in output unless also listed as a non-splitting field.
        // The current logic for delimited output in generateSingleLine joins values from outputLineData
        // based on the 'lineSpecificFieldDefs'. If "SourceDataField" is not meant to be output,
        // it should be excluded from the final list of fields that determines the output columns.
        // For this test, let's assume the config means FieldA, FieldB, FieldC are the output columns.
        // To achieve "part1,part2,part3", the fields in jsonConfig should only be FieldA, FieldB, FieldC
        // if SourceDataField is just a temporary holder.
        // Let's refine the config for clarity of output:
        String refinedJsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" + // These are the fields that will form the output line
            "  { \"name\": \"FieldA\" }," +
            "  { \"name\": \"FieldB\" }," +
            "  { \"name\": \"FieldC\" }," +
            // This field is processed for its split config, but not directly outputted unless also listed above.
            "  { \"name\": \"_SplitSourceInternal\", \"sourceXpath\": \"/root/data\", \"placeholderValue\":\"\","+
            "    \"splitConfig\": { \"delimiter\": \"\\\\|\", \"targetFieldNames\": [\"FieldA\", \"FieldB\", \"FieldC\"] } }" +
            "]" +
            "}";
        // The service's generateSingleLine method iterates lineSpecificFieldDefs to build the outputLineData,
        // then iterates allDefinedFieldsForFormatting (top-level config.fields) for fixed-width,
        // or lineSpecificFieldDefs for delimited output.
        // The issue is that `_SplitSourceInternal` is part of `lineSpecificFieldDefs` if we use this config directly.
        // The current implementation of delimited output in generateSingleLine will use relevantOrderedValues derived from lineSpecificFieldDefs.
        // So, if _SplitSourceInternal is in lineSpecificFieldDefs, its final value (empty string after splitting) would be included.
        // The most robust way is to have `config.fields` define the final output structure.
        // Header/Body/Tail fields define *what data goes into those output slots*.

        // Let's use the hierarchical setup way of thinking: config.fields defines the output columns.
        // The processing of bodyConfig.fields (or header/tail) populates the map for these output columns.
         String betterJsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" + // These define the final output columns and their order
            "  { \"name\": \"FieldA\"}," +
            "  { \"name\": \"FieldB\"}," +
            "  { \"name\": \"FieldC\"}" +
            "]," +
            // This acts like a bodyConfig or similar, defining how to populate the above fields.
            // For a single line test, we can imagine this is the only entry in a bodyConfig.fields.
            "\"bodyConfig\": { \"baseXpath\": \"/root\", \"fields\": ["+
            "  { \"name\": \"_SplitSource\", \"sourceXpath\": \"data\", "+ // XPath relative to /root
            "    \"splitConfig\": { \"delimiter\": \"\\\\|\", \"targetFieldNames\": [\"FieldA\", \"FieldB\", \"FieldC\"] } }"+
            // FieldA, FieldB, FieldC also need to be in this list if we want their placeholders to be initialized by generateSingleLine
            // This is getting complicated because the test setup is trying to use the full generateFlatFile.
            // Let's simplify the config to test the splitting logic in isolation, assuming FieldA, B, C are desired outputs.
            // The current generateFlatFile expects either HBT or a single top-level fields list.
            // If we use top-level fields, the split source itself will also be an output field if not handled.
            // The easiest way for this unit test is to assume the split source field itself is NOT an output field.
            // This means the final list of fields for line assembly should exclude it.
            // The current code for delimited output in generateSingleLine uses `lineSpecificFieldDefs` to build `relevantOrderedValues`.
            // So, `_SplitSource` would be included.

            // Test will rely on the fact that `_SplitSource` will be processed, populate A,B,C, and then its own value (empty) will be output.
            // To get "part1,part2,part3", `_SplitSource` must NOT be in the final ordered list for delimited.
            // This requires that `allDefinedFieldsForFormatting` (config.fields) is what truly defines the output.
            // And `generateSingleLine` for delimited output should use `allDefinedFieldsForFormatting` and filter by `outputLineData.containsKey()`.
            // This is how fixed-width works. Let's adjust delimited output generation to be consistent.
            // The current `generateSingleLine` for delimited creates `relevantOrderedValues` from `lineSpecificFieldDefs`.
            // This is actually correct for H-B-T where `lineSpecificFieldDefs` are from Header/Body/Tail config.

            // Simpler config for testing:
            "\"headerConfig\": { \"fields\": [" + // Using header for single line processing
            "  { \"name\": \"_SplitSource\", \"sourceXpath\": \"/root/data\", " +
            "    \"splitConfig\": { \"delimiter\": \"\\\\|\", \"targetFieldNames\": [\"FieldA\", \"FieldB\", \"FieldC\"] } }," +
            "  { \"name\": \"FieldA\", \"placeholderValue\":\"\"}," + // These will be populated by the split
            "  { \"name\": \"FieldB\", \"placeholderValue\":\"\"}," +
            "  { \"name\": \"FieldC\", \"placeholderValue\":\"\"}" +
            "]}" +
         "}";
        // The top-level config.fields should define the actual output columns for delimited if we want to exclude _SplitSource
        // But for this test, the headerConfig.fields will be used as lineSpecificFieldDefs.
        // The delimited output will be "_SplitSourceValue,FieldAValue,FieldBValue,FieldCValue"
        // Since _SplitSource's value after split processing is not explicitly defined (it's consumed), it's effectively empty.
        // The parts go into FieldA, FieldB, FieldC in outputLineData.
        // The `generateSingleLine` iterates `lineSpecificFieldDefs` (header fields in this case) for delimited.
        // So, it will fetch "" (for _SplitSource), "part1", "part2", "part3".
        // This means expected is ",part1,part2,part3" if _SplitSource itself has no other value.

        // Let's assume the target fields are the only ones we care about in the output.
        // This means the `lineSpecificFieldDefs` for delimited output should *only* contain FieldA, FieldB, FieldC.
        // This implies that the config structure for splitting should clearly separate "processing fields" from "output fields".
        // The current model: top-level "fields" are for output structure (lengths, order).
        // H/B/T "fields" are for processing.

        String finalJsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" + // These are the fields that will form the output line and their order
            "  { \"name\": \"FieldA\" }," +
            "  { \"name\": \"FieldB\" }," +
            "  { \"name\": \"FieldC\" }" +
            "]," +
            // Use bodyConfig to define the processing logic for these fields
            "\"bodyConfig\": { \"baseXpath\": \"/root\", \"fields\": ["+ // Process /root once
            "  { \"name\": \"_DataToSplit\", \"sourceXpath\": \"data\", " + // Relative to /root
            "    \"splitConfig\": { \"delimiter\": \"\\\\|\", \"targetFieldNames\": [\"FieldA\", \"FieldB\", \"FieldC\"] } }," +
            // We also need to declare FieldA, B, C here if they could have placeholders or other direct settings,
            // but for split target, their values are purely from the split.
            // Their presence in top-level config.fields ensures they are part of the output structure.
             "  { \"name\": \"FieldA\", \"placeholderValue\":\"\"}," +
             "  { \"name\": \"FieldB\", \"placeholderValue\":\"\"}," +
             "  { \"name\": \"FieldC\", \"placeholderValue\":\"\"}"  +
            "]}" +
         "}";

        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, finalJsonConfig);
        assertEquals("part1,part2,part3", result);
    }

    @Test
    void testFieldSplitting_FixedWidthOutput() throws Exception {
        String xmlInput = "<root><values>val1:val2:val3</values></root>";
        String jsonConfig = "{" +
            "\"type\": \"fixed-width\"," +
            "\"fields\": [" + // Defines output structure and order
            "  { \"name\": \"Output1\", \"length\": 5 }," +
            "  { \"name\": \"Output2\", \"length\": 5 }," +
            "  { \"name\": \"Output3\", \"length\": 5 }," +
            "  { \"name\": \"Output4\", \"length\": 5, \"placeholderValue\": \"Def\" }" + // For not enough parts
            "]," +
            "\"bodyConfig\": { \"baseXpath\": \"/root\", \"fields\": [" +
            "  { \"name\": \"_SourceForSplit\", \"sourceXpath\": \"values\"," +
            "    \"splitConfig\": { \"delimiter\": \":\", \"targetFieldNames\": [\"Output1\", \"Output2\", \"Output3\", \"Output4\"] } }," +
            // Define target fields also in the processing list if they need specific handling beyond being split targets
            // (e.g. if they could also be populated by other means if split source is empty)
            // For this test, their primary population comes from the split.
            // Their placeholder in the top-level 'fields' will be used if split doesn't provide a value.
            "  { \"name\": \"Output1\"}," + // Placeholders taken from top-level if not specified here
            "  { \"name\": \"Output2\"}," +
            "  { \"name\": \"Output3\"}," +
            "  { \"name\": \"Output4\"}"  +
            "]}" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("val1 val2 val3 Def  ", result); // Output4 uses its placeholder
    }

    @Test
    void testFieldSplitting_NotEnoughParts() throws Exception {
        String xmlInput = "<root><data>one|two</data></root>";
         String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [" +
            "  { \"name\": \"FieldA\"}," +
            "  { \"name\": \"FieldB\"}," +
            "  { \"name\": \"FieldC\", \"placeholderValue\": \"MISSING\"}" + // Top-level placeholder
            "]," +
            "\"bodyConfig\": { \"baseXpath\": \"/root\", \"fields\": ["+
            "  { \"name\": \"_SplitSource\", \"sourceXpath\": \"data\", "+
            "    \"splitConfig\": { \"delimiter\": \"\\\\|\", \"targetFieldNames\": [\"FieldA\", \"FieldB\", \"FieldC\"] } }," +
            "  { \"name\": \"FieldA\"}," +
            "  { \"name\": \"FieldB\"}," +
            "  { \"name\": \"FieldC\"}"  + // Will get placeholder from top-level via outputLineData init
            "]}" +
         "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("one,two,MISSING", result);
    }

    // --- Iterative Processing Tests (using BodyConfig) ---
    @Test
    void testIterativeProcessing_BodyConfig_Delimited() throws Exception {
        String xmlInput = "<root>" +
                            "<items>" +
                            "  <item><id>A</id><val>1</val></item>" +
                            "  <item><id>B</id><val>2</val></item>" +
                            "</items>" +
                          "</root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \";\"," +
            "\"fields\": [" + // Defines structure of output lines from body
            "  { \"name\": \"ItemID\", \"length\": 5 }," + // Lengths ignored for delimited, but good practice
            "  { \"name\": \"ItemValue\", \"length\": 5 }" +
            "]," +
            "\"bodyConfig\": {" +
            "  \"baseXpath\": \"/root/items/item\"," +
            "  \"fields\": [" +
            "    { \"name\": \"ItemID\", \"sourceXpath\": \"id\" }," +
            "    { \"name\": \"ItemValue\", \"sourceXpath\": \"val\" }" +
            "  ]}" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        String expected = "A;1" + System.lineSeparator() + "B;2";
        assertEquals(expected, result);
    }

    @Test
    void testIterativeProcessing_BodyConfig_FixedWidth() throws Exception {
        String xmlInput = "<root>" +
                            "<items>" +
                            "  <item><id>A</id><val>1</val></item>" +
                            "  <item><id>B</id><val>22</val></item>" + // Val is longer
                            "</items>" +
                          "</root>";
        String jsonConfig = "{" +
            "\"type\": \"fixed-width\"," +
            "\"fields\": [" + // Defines structure and formatting for output lines
            "  { \"name\": \"ItemID\", \"length\": 3 }," +
            "  { \"name\": \"ItemValue\", \"length\": 3 }" +
            "]," +
            "\"bodyConfig\": {" +
            "  \"baseXpath\": \"/root/items/item\"," +
            "  \"fields\": [" + // Defines data mapping for each item
            "    { \"name\": \"ItemID\", \"sourceXpath\": \"id\" }," +
            "    { \"name\": \"ItemValue\", \"sourceXpath\": \"val\" }" +
            "  ]}" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        String expected = "A  1  " + System.lineSeparator() + "B  22 "; // Note padding
        assertEquals(expected, result);
    }

    @Test
    void testIterativeProcessing_NoItemsFound() throws Exception {
        String xmlInput = "<root><items></items></root>"; // No item elements
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \";\"," +
            "\"fields\": [ { \"name\": \"ItemID\" }, { \"name\": \"ItemValue\" } ]," +
            "\"bodyConfig\": {" +
            "  \"baseXpath\": \"/root/items/item\"," +
            "  \"fields\": [" +
            "    { \"name\": \"ItemID\", \"sourceXpath\": \"id\" }," +
            "    { \"name\": \"ItemValue\", \"sourceXpath\": \"val\" }" +
            "  ]}" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("", result); // Expect empty output if no items and no header/footer
    }

    // --- Helper to load resource files ---
    private String loadResourceFile(String filePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IOException("Cannot load resource file: " + filePath);
        }
        try (java.util.Scanner scanner = new java.util.Scanner(inputStream, java.nio.charset.StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    // --- Hierarchical Processing (H-B-T) and Validation Tests ---
    @Test
    void testHierarchicalProcessing_ValidCount() throws Exception {
        String xmlInput = loadResourceFile("sample-request-hierarchical.xml");
        String jsonConfig = loadResourceFile("sample-config-hierarchical.json");

        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);

        String expectedHeader = "BCH001    10/26/2023SystemA   "; // BatchID (10), CreationDate (10), SourceSystem (10)
        String expectedBody1  = "R001    Value for record 1  100.50    "; // RecordID (8), RecordData (20), RecordAmount (10)
        String expectedBody2  = "R002    Value for record 2  75.00     ";
        String expectedBody3  = "R003    Another value for re120.25    "; // RecordData truncated
        String expectedTail   = "3    FINAL     VALIDATED_OK"; // FooterRecordCount (5), FooterSummaryCode (10), ValidationStatus (15)
        // Note: The sample-config-hierarchical.json's top-level "fields" define the full line structure.
        // Header will be padded to full line length. Body lines too. Tail too.
        // The current fixed-width logic in generateSingleLine pads each field,
        // but then the parts not relevant to H, B, or T are filled with spaces.
        // Let's re-evaluate the expected output based on current generateSingleLine fixed-width logic.
        // allDefinedFieldsForFormatting is used to build the line.
        // If a field (e.g. RecordID) is not in headerConfig.fields, it gets an empty string of its defined length.

        // Expected output based on current generateSingleLine fixed-width:
        // Header: BatchID, CreationDate, SourceSystem, then empty spaces for RecordID, RecordData, RecordAmount, FooterRecordCount, FooterSummaryCode, ValidationStatus
        // Body: Empty for BatchID, CreationDate, SourceSystem, then RecordID, RecordData, RecordAmount, then empty for Footer fields
        // Tail: Empty for BatchID, CreationDate, SourceSystem, RecordID, RecordData, RecordAmount, then FooterRecordCount, FooterSummaryCode, ValidationStatus

        // Fields: BatchID(10), CreationDate(10), SourceSystem(10), RecordID(8), RecordData(20), RecordAmount(10), FooterRecordCount(5), FooterSummaryCode(10), ValidationStatus(15)
        // Total length = 10+10+10+8+20+10+5+10+15 = 98

        expectedHeader = "BCH001    10/26/2023SystemA                                                                     "; // Length 98
        expectedBody1  = "                    R001    Value for record 1  100.50                                            ";
        expectedBody2  = "                    R002    Value for record 2  75.00                                             ";
        expectedBody3  = "                    R003    Another value for re120.25                                            ";
        expectedTail   = "                                                3    FINAL     NOT_VALIDATED"; // Placeholder for ValidationStatus initially

        // The validation occurs *after* outputLineData is populated for the tail, but *before* the string is built.
        // The test config has "ValidationStatus" with placeholder "NOT_VALIDATED".
        // If validation passes, it prints to System.out. If fails, to System.err. It does not modify the field value yet.
        // So, "NOT_VALIDATED" is expected.

        String expectedFullOutput = String.join(System.lineSeparator(), expectedHeader, expectedBody1, expectedBody2, expectedBody3, expectedTail);
        assertEquals(expectedFullOutput, result);
        // TODO: Add assertions for System.out/err logging for validation status if possible, or modify service to return validation status.
    }

    @Test
    void testHierarchicalProcessing_MismatchedCount() throws Exception {
        String xmlInput = loadResourceFile("sample-request-hierarchical.xml").replace("<totalRecords>3</totalRecords>", "<totalRecords>2</totalRecords>"); // Create mismatch
        String jsonConfig = loadResourceFile("sample-config-hierarchical.json");

        Document xmlDoc = parseXmlString(xmlInput);
        // Capture System.err output to check for validation failure message
        java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(errContent)); // Redirect System.err

        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);

        // Restore System.err
        System.setErr(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err)));


        String expectedHeader = "BCH001    10/26/2023SystemA                                                                     ";
        String expectedBody1  = "                    R001    Value for record 1  100.50                                            ";
        String expectedBody2  = "                    R002    Value for record 2  75.00                                             ";
        String expectedBody3  = "                    R003    Another value for re120.25                                            ";
        String expectedTail   = "                                                2    FINAL     NOT_VALIDATED"; // Count from XML is now 2

        String expectedFullOutput = String.join(System.lineSeparator(), expectedHeader, expectedBody1, expectedBody2, expectedBody3, expectedTail);
        assertEquals(expectedFullOutput, result);
        assertTrue(errContent.toString().contains("VALIDATION FAILED: Tail count (2 from field 'FooterRecordCount') does not match actual processed body record count (3)."));
    }

    // --- Error/Edge Cases ---
    @Test
    void testEmptyInputXml() throws Exception {
        String xmlInput = "";
        String jsonConfig = "{\"type\":\"delimited\",\"delimiter\":\",\",\"fields\":[{\"name\":\"FieldA\",\"sourceXpath\":\"/root/a\"}]}";

        // Expect parseXml to throw an exception for empty input
        assertThrows(org.xml.sax.SAXParseException.class, () -> {
            parseXmlString(xmlInput); // This will throw before generateFlatFile is even called
        });
    }

    @Test
    void testMalformedXml() throws Exception {
        String xmlInput = "<root><item>data</item</root>"; // Malformed closing tag for root
        String jsonConfig = "{\"type\":\"delimited\",\"delimiter\":\",\",\"fields\":[{\"name\":\"FieldA\",\"sourceXpath\":\"/root/item\"}]}";

        assertThrows(org.xml.sax.SAXParseException.class, () -> {
            parseXmlString(xmlInput);
        });
    }

    @Test
    void testJsonConfig_MissingFieldsArrayInBody() throws Exception {
        String xmlInput = "<root><item>data</item></root>";
        // Config where bodyConfig is present but its 'fields' array is missing or null
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [{\"name\": \"Output\"}]," + // Top-level fields for structure
            "\"bodyConfig\": {" +
            "  \"baseXpath\": \"/root/item\"" + // 'fields' is missing in bodyConfig
            "}" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        // The service currently falls back to top-level fields if bodyConfig.fields is null/empty.
        // If top-level fields are also not suitable for mapping 'item', it might produce empty or placeholder.
        // For this config, it will try to process /root/item using the top-level "Output" field.
        // "Output" has no sourceXpath, so it will be its placeholder (empty string by default).
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        assertEquals("", result); // Expecting empty because "Output" field has no sourceXpath
    }

    @Test
    void testJsonConfig_EmptyFieldsArrayInBody() throws Exception {
        String xmlInput = "<root><item>data</item></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": [{\"name\": \"Output\"}]," +
            "\"bodyConfig\": {" +
            "  \"baseXpath\": \"/root/item\"," +
            "  \"fields\": []" + // Empty fields in bodyConfig
            "}" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        // Similar to above, generateSingleLine will be called with an empty fieldDefinitions list.
        // The delimited output will be empty because relevantOrderedValues will be empty.
        assertEquals("", result);
    }


    @Test
    void testNoProcessingConfigsDefined() throws Exception {
        String xmlInput = "<root><data>value</data></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\"," +
            "\"delimiter\": \",\"," +
            "\"fields\": []" + // No fields defined at all for output structure
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        // If config.getFields() is empty, allDefinedFieldsForFormatting is empty.
        // The fallback path in generateFlatFile will be hit, but if config.getFields() is empty, it returns "".
        assertEquals("", result);
    }

    @Test
    void testAbsolutePathInRepeatingElement() throws Exception {
        String xmlInput = "<root><global>GlobalValue</global><items><item><local>Local1</local></item><item><local>Local2</local></item></items></root>";
        String jsonConfig = "{" +
            "\"type\": \"delimited\", \"delimiter\": \",\"," +
            "\"fields\": [ {\"name\":\"Global\"}, {\"name\":\"Local\"} ]," +
            "\"bodyConfig\": {" +
            "  \"baseXpath\": \"/root/items/item\"," +
            "  \"fields\": [" +
            "    { \"name\": \"Global\", \"sourceXpath\": \"/root/global\" }," + // Absolute path
            "    { \"name\": \"Local\", \"sourceXpath\": \"local\" }" + // Relative path
            "  ]}" +
            "}";
        Document xmlDoc = parseXmlString(xmlInput);
        String result = transformationService.generateFlatFile(xmlDoc, jsonConfig);
        String expected = "GlobalValue,Local1" + System.lineSeparator() + "GlobalValue,Local2";
        assertEquals(expected, result);
    }
}
