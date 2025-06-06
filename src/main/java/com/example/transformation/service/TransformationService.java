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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Model imports
import com.example.transformation.model.FlatFileConfig;
import com.example.transformation.model.HeaderConfig;
import com.example.transformation.model.BodyConfig;
import com.example.transformation.model.TailConfig;
import com.example.transformation.model.ValidationConfig;
// Removed: import com.example.transformation.model.RepeatingElementConfig;
import com.example.transformation.model.StringOperationConfig;
import com.example.transformation.model.SplitConfig;
import com.example.transformation.model.FieldConfig; // Explicit import for clarity

// W3C DOM imports for Node and NodeList
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// SLF4J imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class TransformationService {

    private static final Logger logger = LoggerFactory.getLogger(TransformationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentBuilderFactory documentBuilderFactory;
    private final XPathFactory xPathFactory;

    public TransformationService() {
        logger.info("Initializing TransformationService");
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setXIncludeAware(false);
            documentBuilderFactory.setExpandEntityReferences(false);
            logger.debug("DocumentBuilderFactory configured for secure parsing.");
        } catch (ParserConfigurationException e) {
            logger.error("Failed to configure DocumentBuilderFactory for secure parsing", e);
            throw new RuntimeException("Failed to configure DocumentBuilderFactory for secure parsing", e);
        }
        xPathFactory = XPathFactory.newInstance();
    }

    /**
     * Parses an XML string into a W3C Document object.
     *
     * @param xmlData The XML data as a string.
     * @return The parsed W3C Document.
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created which satisfies the configuration requested.
     * @throws SAXException If any SAX errors occur during parsing.
     * @throws IOException If any I/O errors occur.
     */
    public Document parseXml(String xmlData) throws ParserConfigurationException, SAXException, IOException {
        logger.debug("Attempting to parse XML data. Length: {}", xmlData != null ? xmlData.length() : "null");
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));
        logger.debug("XML data parsed successfully.");
        return doc;
    }

    // Overloaded evaluateXpath to accept a Node as context
    private String evaluateXpath(XPath xpath, Object contextNode, String expression) throws XPathExpressionException {
        if (expression == null || expression.trim().isEmpty() || contextNode == null) {
            return null;
        }
        // Ensure the contextNode is either Document or Node for XPath evaluation
        if (!(contextNode instanceof Document || contextNode instanceof Node)) {
            throw new IllegalArgumentException("XPath context must be a Document or Node.");
        }
        String result = (String) xpath.evaluate(expression, contextNode, XPathConstants.STRING);
        return (result != null && !result.isEmpty()) ? result : null;
    }

    private String applyStringOperations(String currentValue, List<StringOperationConfig> operations, XPath xpath, Object contextNode) throws XPathExpressionException {
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
                        } else if (result != null) {
                            logger.warn("Invalid substring indices for value '{}': Start {}, End {}. Value unchanged.", result, op.getSubstringStartIndex(), op.getSubstringEndIndex());
                        }
                        break;
                    case "CONCATENATE":
                        String concatVal = "";
                        if (op.getConcatValue() != null) {
                            concatVal = op.getConcatValue();
                        } else if (op.getConcatValueFromXpath() != null) {
                            // Pass the current contextNode for concatValueFromXpath
                            String dynamicConcatVal = evaluateXpath(xpath, contextNode, op.getConcatValueFromXpath());
                            if (dynamicConcatVal != null) {
                                concatVal = dynamicConcatVal;
                            }
                        }
                        result = (result == null) ? concatVal : result + concatVal;
                        break;
                    default:
                        logger.warn("Unsupported string operation type: {}", op.getType());
                }
            } catch (Exception e) {
                logger.error("Error during string operation {} on value '{}'", op.getType(), result, e);
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
            DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(sourceFormat);
            TemporalAccessor temporalAccessor = sourceFormatter.parse(currentValue);
            DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(targetFormat);
            return targetFormatter.format(temporalAccessor);
        } catch (DateTimeParseException e) {
            logger.warn("Error parsing date string '{}' with source format '{}': {}", currentValue, sourceFormat, e.getMessage());
            return "";
        } catch (IllegalArgumentException e) {
            logger.error("Error with date format pattern. Source: '{}', Target: '{}'", sourceFormat, targetFormat, e);
            return "";
        }
    }

    /**
     * Generates a flat file string from a parsed XML document based on a JSON configuration.
     * Supports header, body (iterative), and tail sections, as well as various field transformations
     * including conditional mapping, string operations, date formatting, and field splitting.
     * Also performs validation of body record count against a count specified in the tail.
     *
     * @param xmlDoc The parsed W3C Document representing the input XML.
     * @param jsonConfigString A string containing the JSON configuration for the transformation.
     * @return A string representing the generated flat file. Multiple lines are separated by {@code System.lineSeparator()}.
     * @throws IOException If there's an error reading the JSON configuration string.
     * @throws XPathExpressionException If there's an error during XPath evaluation.
     * @throws IllegalArgumentException If the configuration contains unsupported types or invalid settings.
     */
    public String generateFlatFile(Document xmlDoc, String jsonConfigString) throws IOException, XPathExpressionException {
        logger.info("Starting flat file generation process.");
        long startTime = System.currentTimeMillis();

        FlatFileConfig config = objectMapper.readValue(jsonConfigString, FlatFileConfig.class);
        if (logger.isDebugEnabled()) {
            // Be cautious about logging entire config if it can contain sensitive details or is very large.
            // Consider serializing to string with a pretty printer or summarizing.
            logger.debug("Using JSON configuration: {}", jsonConfigString);
        }

        XPath xpath = xPathFactory.newXPath();
        List<String> allOutputLines = new ArrayList<>();
        Map<String, Object> processingContext = new HashMap<>();

        List<FieldConfig> allDefinedFieldsForFormatting = config.getFields() != null ? config.getFields() : new ArrayList<>();

        int headerLines = 0;
        int bodyLines = 0;
        int tailLines = 0;

        if (config.getHeaderConfig() != null && config.getHeaderConfig().getFields() != null && !config.getHeaderConfig().getFields().isEmpty()) {
            logger.debug("Processing header...");
            HeaderConfig headerConfig = config.getHeaderConfig();
            allOutputLines.add(generateSingleLine(xmlDoc, headerConfig.getFields(), allDefinedFieldsForFormatting, config, xpath, xmlDoc, processingContext, false));
            headerLines++;
        }

        if (config.getBodyConfig() != null && config.getBodyConfig().getBaseXpath() != null && config.getBodyConfig().getFields() != null && !config.getBodyConfig().getFields().isEmpty()) {
            BodyConfig bodyConfig = config.getBodyConfig();
            logger.debug("Processing body with baseXpath: {}", bodyConfig.getBaseXpath());
            NodeList nodeList = (NodeList) xpath.evaluate(bodyConfig.getBaseXpath(), xmlDoc, XPathConstants.NODESET);
            int actualBodyItemCount = (nodeList != null) ? nodeList.getLength() : 0;
            logger.debug("Found {} body elements.", actualBodyItemCount);

            if (bodyConfig.getCountFieldAlias() != null && !bodyConfig.getCountFieldAlias().trim().isEmpty()) {
                processingContext.put(bodyConfig.getCountFieldAlias(), actualBodyItemCount);
                logger.debug("Stored body count {} with alias '{}'", actualBodyItemCount, bodyConfig.getCountFieldAlias());
            }

            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node repeatingNode = nodeList.item(i);
                    allOutputLines.add(generateSingleLine(repeatingNode, bodyConfig.getFields(), allDefinedFieldsForFormatting, config, xpath, xmlDoc, processingContext, false));
                    bodyLines++;
                }
            }
        }

        if (config.getTailConfig() != null && config.getTailConfig().getFields() != null && !config.getTailConfig().getFields().isEmpty()) {
            logger.debug("Processing tail...");
            TailConfig tailConfig = config.getTailConfig();
            allOutputLines.add(generateSingleLine(xmlDoc, tailConfig.getFields(), allDefinedFieldsForFormatting, config, xpath, xmlDoc, processingContext, true));
            tailLines++;
        }

        if (config.getHeaderConfig() == null && config.getBodyConfig() == null && config.getTailConfig() == null &&
            config.getFields() != null && !config.getFields().isEmpty()) {
            logger.info("No H-B-T structure defined. Processing using top-level fields configuration.");
             allOutputLines.add(generateSingleLine(xmlDoc, config.getFields(), allDefinedFieldsForFormatting, config, xpath, xmlDoc, processingContext, false));
             // This single line could be an "implicit body" line.
             if (allOutputLines.size() == 1 && allOutputLines.get(0).isEmpty() && config.getFields().stream().allMatch(f -> f.getPlaceholderValue() == null || f.getPlaceholderValue().isEmpty())) {
                 // If the single line is empty and all placeholders were empty/null, treat as no actual data lines.
             } else {
                 bodyLines = allOutputLines.size(); // Or more complex logic if top-level fields can be iterative themselves
             }
        }

        long endTime = System.currentTimeMillis();
        logger.info("Flat file generation completed in {} ms. Header lines: {}, Body lines: {}, Tail lines: {}", (endTime - startTime), headerLines, bodyLines, tailLines);
        return String.join(System.lineSeparator(), allOutputLines);
    }

    /**
     * Generates a single line of the flat file.
     * This is a core helper method that processes a list of field definitions against a given XML context node (which can be the full document or a specific repeating element).
     * It handles data extraction via XPath, conditional logic, string operations, date formatting, and field splitting.
     * For fixed-width files, it uses the allDefinedFieldsForFormatting to ensure consistent line structure and padding.
     *
     * @param contextNode The XML node to be used as context for relative XPath evaluations (can be Document or Node).
     * @param lineSpecificFieldDefs The list of {@link FieldConfig} specific to the current line being generated (e.g., header fields, body fields for one iteration, or tail fields).
     * @param allDefinedFieldsForFormatting A list of all unique {@link FieldConfig}s defined in the main configuration's "fields" section; used for determining overall field order and fixed-width properties.
     * @param globalConfig The global {@link FlatFileConfig} containing file type, delimiter, and H-B-T configurations.
     * @param xpath The reusable {@link XPath} object.
     * @param fullXmlDoc The complete input XML document, used as context for absolute XPath expressions.
     * @param processingContext A map to store and retrieve values between processing stages (e.g., body count for tail validation).
     * @param isTailProcessing A boolean flag indicating if the current line being generated is the tail, to trigger validation logic.
     * @return A string representing the single generated flat file line.
     * @throws XPathExpressionException If an XPath evaluation fails.
     */
    private String generateSingleLine(Object contextNode, List<FieldConfig> lineSpecificFieldDefs, List<FieldConfig> allDefinedFieldsForFormatting, FlatFileConfig globalConfig, XPath xpath, Document fullXmlDoc, Map<String, Object> processingContext, boolean isTailProcessing) throws XPathExpressionException {
        logger.debug("Generating single line. Context node type: {}, Number of specific field definitions: {}", contextNode.getClass().getSimpleName(), lineSpecificFieldDefs.size());
        Map<String, String> outputLineData = new HashMap<>();

        for (FieldConfig fieldDef : lineSpecificFieldDefs) {
            outputLineData.put(fieldDef.getName(), fieldDef.getPlaceholderValue() != null ? fieldDef.getPlaceholderValue() : "");
        }

        for (FieldConfig fieldDef : lineSpecificFieldDefs) {
            String initialFieldValue = "";
            // Determine context for XPath: relative paths use contextNode, absolute paths use fullXmlDoc
            // This was simplified: individual XPaths (source, condition, met, notmet) will re-evaluate their context.

            boolean conditionMet = false;
            if (fieldDef.getConditionSourceXpath() != null && !fieldDef.getConditionSourceXpath().trim().isEmpty() &&
                fieldDef.getConditionExpectedValue() != null) {
                try {
                    Object conditionContext = fieldDef.getConditionSourceXpath().startsWith("/") ? fullXmlDoc : contextNode;
                    String conditionActualValue = evaluateXpath(xpath, conditionContext, fieldDef.getConditionSourceXpath());
                    if (conditionActualValue != null && conditionActualValue.equals(fieldDef.getConditionExpectedValue())) {
                        conditionMet = true;
                    }
                } catch (XPathExpressionException e) {
                    logger.warn("Error evaluating condition XPath for field {}: {}", fieldDef.getName(), e.getMessage());
                }
            }

            if (conditionMet) {
                if (fieldDef.getValueIfConditionMetConstant() != null) {
                    initialFieldValue = fieldDef.getValueIfConditionMetConstant();
                } else {
                    Object valueContext = fieldDef.getValueIfConditionMetXpath() != null && fieldDef.getValueIfConditionMetXpath().startsWith("/") ? fullXmlDoc : contextNode;
                    initialFieldValue = evaluateXpath(xpath, valueContext, fieldDef.getValueIfConditionMetXpath());
                }
            } else {
                if (fieldDef.getValueIfConditionNotMetConstant() != null) {
                    initialFieldValue = fieldDef.getValueIfConditionNotMetConstant();
                } else if (fieldDef.getValueIfConditionNotMetXpath() != null) {
                    Object valueContext = fieldDef.getValueIfConditionNotMetXpath().startsWith("/") ? fullXmlDoc : contextNode;
                    initialFieldValue = evaluateXpath(xpath, valueContext, fieldDef.getValueIfConditionNotMetXpath());
                } else {
                    Object valueContext = fieldDef.getSourceXpath() != null && fieldDef.getSourceXpath().startsWith("/") ? fullXmlDoc : contextNode;
                    initialFieldValue = evaluateXpath(xpath, valueContext, fieldDef.getSourceXpath());
                }
            }
            if (initialFieldValue == null) {
                initialFieldValue = fieldDef.getPlaceholderValue() != null ? fieldDef.getPlaceholderValue() : "";
            }

            String processedValue = applyStringOperations(initialFieldValue, fieldDef.getStringOperations(), xpath, contextNode);

            if (fieldDef.getSplitConfig() != null && fieldDef.getSplitConfig().getDelimiter() != null && fieldDef.getSplitConfig().getTargetFieldNames() != null) {
                SplitConfig splitConfig = fieldDef.getSplitConfig();
                String[] parts = (processedValue == null ? new String[0] : processedValue.split(splitConfig.getDelimiter(), -1));
                for (int i = 0; i < splitConfig.getTargetFieldNames().size(); i++) {
                    String targetFieldName = splitConfig.getTargetFieldNames().get(i);
                    FieldConfig targetFieldOverallConf = allDefinedFieldsForFormatting.stream().filter(f -> f.getName().equals(targetFieldName)).findFirst().orElse(null);
                    String partValue = (i < parts.length) ? parts[i] : (targetFieldOverallConf != null && targetFieldOverallConf.getPlaceholderValue() != null ? targetFieldOverallConf.getPlaceholderValue() : "");

                    if (targetFieldOverallConf != null) {
                         partValue = applyDateFormatting(partValue, targetFieldOverallConf.getSourceDateFormat(), targetFieldOverallConf.getTargetDateFormat());
                    } else {
                        // If target field from split is not in allDefinedFieldsForFormatting, it cannot be formatted for fixed-width correctly.
                        // This case implies a configuration error where a split target field isn't declared globally.
                         logger.warn("Split target field '{}' is not defined in the global field formatting list. Its length cannot be determined for fixed-width.", targetFieldName);
                    }
                    outputLineData.put(targetFieldName, partValue == null ? "" : partValue);
                }
            } else {
                String finalValue = applyDateFormatting(processedValue, fieldDef.getSourceDateFormat(), fieldDef.getTargetDateFormat());
                outputLineData.put(fieldDef.getName(), finalValue == null ? "" : finalValue);
            }
        }

        if (isTailProcessing && globalConfig.getTailConfig() != null && globalConfig.getTailConfig().getValidationConfig() != null) {
            ValidationConfig valConfig = globalConfig.getTailConfig().getValidationConfig();
            if (valConfig.getCountSourceField() != null && valConfig.getExpectedCountAlias() != null) {
                String countFromTailDataString = outputLineData.get(valConfig.getCountSourceField());
                Object actualCountObj = processingContext.get(valConfig.getExpectedCountAlias());

                if (countFromTailDataString != null && actualCountObj instanceof Integer) {
                    try {
                        int countFromTail = Integer.parseInt(countFromTailDataString.trim());
                        int actualProcessedCount = (Integer) actualCountObj;
                        if (countFromTail != actualProcessedCount) {
                            logger.warn("VALIDATION FAILED: Tail count ({} from field '{}') does not match actual processed body record count ({}).",
                                       countFromTail, valConfig.getCountSourceField(), actualProcessedCount);
                        } else {
                            logger.info("VALIDATION PASSED: Tail count matches actual processed body record count ({}).", actualProcessedCount);
                        }
                    } catch (NumberFormatException e) {
                        logger.error("VALIDATION ERROR: Could not parse tail count value '{}' from field '{}' to integer.", countFromTailDataString, valConfig.getCountSourceField(), e);
                    }
                } else {
                    logger.warn("VALIDATION SKIPPED: Missing tail count data ('{}' resolved to '{}') or actual processed count (alias '{}' resolved to '{}') for validation.",
                                valConfig.getCountSourceField(), countFromTailDataString, valConfig.getExpectedCountAlias(), actualCountObj);
                }
            }
        }
        StringBuilder lineBuilder = new StringBuilder();
        List<String> orderedValues = new ArrayList<>();
        // Use allDefinedFieldsForFormatting for order and length, but values from lineSpecificFieldDefs processed into outputLineData
        for (FieldConfig fmtFieldDef : allDefinedFieldsForFormatting) {
            // Check if this field was actually part of the current line's definition (header, body, or tail specific)
            boolean partOfCurrentLine = lineSpecificFieldDefs.stream().anyMatch(lsfd -> lsfd.getName().equals(fmtFieldDef.getName()));
            if (partOfCurrentLine) {
                 orderedValues.add(outputLineData.getOrDefault(fmtFieldDef.getName(), ""));
            } else {
                // If a field in the global definition is not in the current section (e.g. a body field when processing header)
                // add an empty placeholder of correct length for fixed-width. For delimited, this might be an empty string.
                orderedValues.add("");
            }
        }

        // Adjusting fixed-width part to use allDefinedFieldsForFormatting for lengths and full line structure
        if ("fixed-width".equalsIgnoreCase(globalConfig.getType())) {
            for (int i = 0; i < allDefinedFieldsForFormatting.size(); i++) {
                FieldConfig fmtFieldDef = allDefinedFieldsForFormatting.get(i);
                String value = orderedValues.get(i); // This now corresponds to the order of allDefinedFieldsForFormatting

                if (value.length() > fmtFieldDef.getLength()) {
                    value = value.substring(0, fmtFieldDef.getLength());
                } else {
                    value = String.format("%-" + fmtFieldDef.getLength() + "s", value);
                }
                lineBuilder.append(value);
            }
        } else if ("delimited".equalsIgnoreCase(globalConfig.getType())) {
            // For delimited, only join values that were actually processed for this line type
            List<String> relevantOrderedValues = new ArrayList<>();
            for(FieldConfig lineSpecificDef : lineSpecificFieldDefs){
                relevantOrderedValues.add(outputLineData.getOrDefault(lineSpecificDef.getName(), ""));
            }
            String delimiter = globalConfig.getDelimiter() != null ? globalConfig.getDelimiter() : ",";
            lineBuilder.append(String.join(delimiter, relevantOrderedValues));
        } else {
            throw new IllegalArgumentException("Unsupported flat file type: " + globalConfig.getType());
        }
        return lineBuilder.toString();
    }
}
