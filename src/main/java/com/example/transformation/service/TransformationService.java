package com.example.transformation.service;

import com.example.transformation.model.FlatFileConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap; // For outputLineData
import java.util.List;
import java.util.Map; // For outputLineData
import java.util.stream.Collectors;

// Assuming StringOperationConfig and SplitConfig are imported correctly
import com.example.transformation.model.StringOperationConfig;
import com.example.transformation.model.SplitConfig;


@Service
public class TransformationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    public Document parseXml(String xmlData) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlData)));
    }

    private String evaluateXpath(XPath xpath, Document xmlDoc, String expression) throws XPathExpressionException {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        String result = (String) xpath.evaluate(expression, xmlDoc, XPathConstants.STRING);
        return (result != null && !result.isEmpty()) ? result : null;
    }

    private String applyStringOperations(String currentValue, List<StringOperationConfig> operations, XPath xpath, Document xmlDoc) throws XPathExpressionException {
        if (operations == null) { // Allow operations on null/empty initial value if CONCATENATE is first
            return currentValue;
        }
        String result = currentValue;
        for (StringOperationConfig op : operations) {
            // Allow CONCATENATE to initialize a null result
            if (result == null && !"CONCATENATE".equalsIgnoreCase(op.getType())) {
                continue; // Most operations cannot act on null
            }
             if (result != null && result.isEmpty() && !"CONCATENATE".equalsIgnoreCase(op.getType())) {
                // Allow CONCATENATE to act on empty string, other ops might be skipped or handled by specific logic
                 if (!op.getType().equalsIgnoreCase("TRIM") && !op.getType().equalsIgnoreCase("UPPERCASE") && !op.getType().equalsIgnoreCase("LOWERCASE")) {
                    // For SUBSTRING on empty string, it will likely fail or produce empty, which is fine.
                    // TRIM, UPPERCASE, LOWERCASE on empty string are no-ops and safe.
                 }
            }

            try {
                switch (op.getType().toUpperCase()) {
                    case "TRIM":
                        result = (result == null) ? null : result.trim();
                        break;
                    case "UPPERCASE":
                        result = (result == null) ? null : result.toUpperCase();
                        break;
                    case "LOWERCASE":
                        result = (result == null) ? null : result.toLowerCase();
                        break;
                    case "SUBSTRING":
                        if (result != null && result.length() >= op.getSubstringEndIndex() && op.getSubstringStartIndex() < op.getSubstringEndIndex() && op.getSubstringStartIndex() >= 0) {
                            result = result.substring(op.getSubstringStartIndex(), op.getSubstringEndIndex());
                        } else if (result != null) { // only print error if result was not null
                            System.err.println("Invalid substring indices for value '" + result + "': Start " + op.getSubstringStartIndex() + ", End " + op.getSubstringEndIndex() + ". Value unchanged.");
                        }
                        break;
                    case "CONCATENATE":
                        String concatVal = "";
                        if (op.getConcatValue() != null) {
                            concatVal = op.getConcatValue();
                        } else if (op.getConcatValueFromXpath() != null) {
                            String dynamicConcatVal = evaluateXpath(xpath, xmlDoc, op.getConcatValueFromXpath());
                            if (dynamicConcatVal != null) {
                                concatVal = dynamicConcatVal;
                            }
                        }
                        result = (result == null) ? concatVal : result + concatVal;
                        break;
                    default:
                        System.err.println("Unsupported string operation type: " + op.getType());
                }
            } catch (Exception e) {
                System.err.println("Error during string operation " + op.getType() + " on value '" + result + "': " + e.getMessage());
            }
        }
        return result;
    }

    private String applyDateFormatting(String currentValue, String sourceFormat, String targetFormat) {
        if (currentValue == null || currentValue.isEmpty() || sourceFormat == null || targetFormat == null ||
            sourceFormat.isEmpty() || targetFormat.isEmpty()) {
            return currentValue; // Not enough info to format, or value is empty/null
        }

        try {
            // Attempt to parse the date using common date/datetime parsers if specific type isn't fixed
            DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(sourceFormat);
            TemporalAccessor temporalAccessor = sourceFormatter.parse(currentValue);
            DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(targetFormat);
            return targetFormatter.format(temporalAccessor);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date string '" + currentValue + "' with source format '" + sourceFormat + "': " + e.getMessage());
            return ""; // Graceful handling: return empty string or original on failure based on requirements
        } catch (IllegalArgumentException e) {
            System.err.println("Error with date format pattern. Source: '" + sourceFormat + "', Target: '" + targetFormat + "': " + e.getMessage());
            return "";
        }
    }

    public String generateFlatFile(Document xmlDoc, String jsonConfigString) throws IOException, XPathExpressionException {
        FlatFileConfig config = objectMapper.readValue(jsonConfigString, FlatFileConfig.class);
        XPath xpath = xPathFactory.newXPath();
        Map<String, String> outputLineData = new HashMap<>();

        if (config.getFields() == null || config.getFields().isEmpty()) {
            return "";
        }

        // Initialize all potential output fields defined in the config to empty string or placeholder
        // This ensures that if a split operation doesn't populate all its target fields, they still exist in the map for ordering.
        for (FlatFileConfig.FieldConfig field : config.getFields()) {
            // If a field is ONLY a target of a split, it might not have its own full config.
            // The primary FieldConfig list (config.getFields()) defines the final output structure and order.
            // We assume targetFieldNames from a split will match a 'name' in this primary list.
             outputLineData.put(field.getName(), field.getPlaceholderValue() != null ? field.getPlaceholderValue() : "");
        }


        // Phase 1: Process all field configurations, including splits
        for (FlatFileConfig.FieldConfig field : config.getFields()) {
            String initialFieldValue = field.getPlaceholderValue() != null ? field.getPlaceholderValue() : "";

            // Conditional Logic to get initial value (applies to both split sources and regular fields)
            boolean conditionMet = false;
            if (field.getConditionSourceXpath() != null && !field.getConditionSourceXpath().trim().isEmpty() &&
                field.getConditionExpectedValue() != null) {
                try {
                    String conditionActualValue = evaluateXpath(xpath, xmlDoc, field.getConditionSourceXpath());
                    if (conditionActualValue != null && conditionActualValue.equals(field.getConditionExpectedValue())) {
                        conditionMet = true;
                    }
                } catch (XPathExpressionException e) {
                    System.err.println("Error evaluating condition XPath for field " + field.getName() + ": " + e.getMessage());
                }
            }

            if (conditionMet) {
                if (field.getValueIfConditionMetConstant() != null) {
                    initialFieldValue = field.getValueIfConditionMetConstant();
                } else {
                    initialFieldValue = evaluateXpath(xpath, xmlDoc, field.getValueIfConditionMetXpath());
                }
            } else {
                if (field.getValueIfConditionNotMetConstant() != null) {
                    initialFieldValue = field.getValueIfConditionNotMetConstant();
                } else if (field.getValueIfConditionNotMetXpath() != null) {
                    initialFieldValue = evaluateXpath(xpath, xmlDoc, field.getValueIfConditionNotMetXpath());
                } else { // Fallback to the general sourceXpath
                    initialFieldValue = evaluateXpath(xpath, xmlDoc, field.getSourceXpath());
                }
            }
             // If after all attempts, initialFieldValue is null, use placeholder.
            if (initialFieldValue == null) {
                initialFieldValue = field.getPlaceholderValue() != null ? field.getPlaceholderValue() : "";
            }

            // Apply string operations to the initial field value (used for both regular fields and as source for split)
            String processedValue = applyStringOperations(initialFieldValue, field.getStringOperations(), xpath, xmlDoc);

            // Handle Splitting if configured
            if (field.getSplitConfig() != null && field.getSplitConfig().getDelimiter() != null && field.getSplitConfig().getTargetFieldNames() != null) {
                SplitConfig splitConfig = field.getSplitConfig();
                String[] parts = (processedValue == null ? new String[0] : processedValue.split(splitConfig.getDelimiter(), -1)); // Use -1 to keep trailing empty strings

                for (int i = 0; i < splitConfig.getTargetFieldNames().size(); i++) {
                    String targetFieldName = splitConfig.getTargetFieldNames().get(i);
                    if (i < parts.length) {
                        // Apply date formatting to each part *if* the target field has date config.
                        // This requires looking up the FieldConfig for the targetFieldName.
                        FlatFileConfig.FieldConfig targetFieldConf = config.getFields().stream().filter(f -> f.getName().equals(targetFieldName)).findFirst().orElse(null);
                        String partValue = parts[i];
                        if (targetFieldConf != null) {
                             // Important: String ops for target fields of a split are NOT applied here.
                             // If individual parts need string ops, they should be separate FieldConfig entries.
                             // Date formatting IS applied here as it's specific to the final representation.
                            partValue = applyDateFormatting(partValue, targetFieldConf.getSourceDateFormat(), targetFieldConf.getTargetDateFormat());
                        }
                        outputLineData.put(targetFieldName, partValue == null ? "" : partValue);
                    } else {
                        // Not enough parts, use placeholder or empty for remaining target fields
                        System.err.println("Not enough parts from split for field " + field.getName() + " to populate target " + targetFieldName);
                        // outputLineData.put(targetFieldName, ""); // Already initialized to placeholder or empty
                    }
                }
            } else {
                // Not a split field, or split not properly configured. Apply date formatting to the processed value.
                String finalValue = applyDateFormatting(processedValue, field.getSourceDateFormat(), field.getTargetDateFormat());
                outputLineData.put(field.getName(), finalValue == null ? "" : finalValue);
            }
        }

        // Phase 2: Assemble the output line from outputLineData based on the order in config.getFields()
        StringBuilder flatFileBuilder = new StringBuilder();
        List<String> orderedValues = new ArrayList<>();

        for (FlatFileConfig.FieldConfig field : config.getFields()) {
            // Retrieve the final value from the map. It should have been populated by either direct processing or split target.
            orderedValues.add(outputLineData.getOrDefault(field.getName(), "")); // Default to empty if somehow missing
        }

        if ("fixed-width".equalsIgnoreCase(config.getType())) {
            for (int i = 0; i < config.getFields().size(); i++) {
                FlatFileConfig.FieldConfig field = config.getFields().get(i);
                String value = orderedValues.get(i);
                if (value.length() > field.getLength()) {
                    value = value.substring(0, field.getLength());
                } else {
                    value = String.format("%-" + field.getLength() + "s", value);
                }
                flatFileBuilder.append(value);
            }
        } else if ("delimited".equalsIgnoreCase(config.getType())) {
            String delimiter = config.getDelimiter() != null ? config.getDelimiter() : ",";
            flatFileBuilder.append(String.join(delimiter, orderedValues));
        } else {
            throw new IllegalArgumentException("Unsupported flat file type: " + config.getType());
        }
        return flatFileBuilder.toString();
    }
}
