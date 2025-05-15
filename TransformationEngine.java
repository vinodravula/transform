package com.example.chatbot.may14;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.sql.*;
import java.util.*;

public class TransformationEngine {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String input = new String(java.nio.file.Files.readAllBytes(new File("src/main/resources/input.xml").toPath()));
        String metadataJson = new String(java.nio.file.Files.readAllBytes(new File("src/main/resources/metadata.json").toPath()));

        JsonNode inputNode = isXml(input) ? new XmlMapper().readTree(input.getBytes()) : mapper.readTree(input);
        JsonNode metadata = mapper.readTree(metadataJson);
        String iteratorPath = metadata.has("iteratorPath") ? metadata.get("iteratorPath").asText() : "$";
        ArrayNode mappings = (ArrayNode) metadata.get("mappings");

        List<LinkedHashMap<String, String>> rows = new ArrayList<>();

        List<LinkedHashMap<String, Object>> records = JsonPath.read(inputNode.toString(), iteratorPath);
        for (LinkedHashMap<String, Object> record : records) {
            ObjectNode transformed = mapper.createObjectNode();
            JsonNode recordNode = mapper.convertValue(record, JsonNode.class);

            for (JsonNode rule : mappings) {
                // Skip if condition fails
                if (rule.has("condition") && !evaluateCondition(rule.get("condition").asText(), recordNode)) continue;

                if (rule.has("inputPath")) {
                    Object value = safeJsonPathRead(recordNode.toString(), rule.get("inputPath").asText());
                    setOutputPath(transformed, rule.get("outputPath").asText(), value);
                } else if (rule.has("inputPaths")) {
                    StringBuilder combined = new StringBuilder();
                    for (JsonNode p : (ArrayNode) rule.get("inputPaths")) {
                        Object v = safeJsonPathRead(recordNode.toString(), p.asText());
                        combined.append(v).append(" ");
                    }
                    setOutputPath(transformed, rule.get("outputPath").asText(), combined.toString().trim());
                } else if (rule.has("constant")) {
                    setOutputPath(transformed, rule.get("outputPath").asText(), rule.get("constant").asText());
                } else if (rule.has("dbLookup")) {
                    JsonNode dbRule = rule.get("dbLookup");
                    String param = safeJsonPathRead(recordNode.toString(), dbRule.get("paramPath").asText()).toString();
                    String result = fetchFromDB(dbRule.get("query").asText(), param);
                    setOutputPath(transformed, dbRule.get("outputPath").asText(), result);
                }
            }

            LinkedHashMap<String, String> row = new LinkedHashMap<>();
            flattenJson(transformed, "", row);
            rows.add(row);
        }

        writeDelimited(rows, metadata.get("delimiter").asText("|"));
    }

    private static boolean isXml(String input) {
        return input.trim().startsWith("<");
    }

    private static Object safeJsonPathRead(String json, String path) {
        try {
            // Normalize path to ensure it starts with $
            if (!path.startsWith("$")) path = "$." + path;
            return JsonPath.read(json, path);
        } catch (Exception e) {
            return "";
        }
    }

    private static void setOutputPath(ObjectNode output, String path, Object value) {
        String[] parts = path.split("\\.");
        ObjectNode current = output;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !(current.get(parts[i]) instanceof ObjectNode)) {
                current.set(parts[i], mapper.createObjectNode());
            }
            current = (ObjectNode) current.get(parts[i]);
        }

        if (value instanceof List) {
            ArrayNode array = mapper.createArrayNode();
            for (Object v : (List<?>) value) array.add(v.toString());
            current.set(parts[parts.length - 1], array);
        } else {
            current.put(parts[parts.length - 1], value != null ? value.toString() : "");
        }
    }

    private static boolean evaluateCondition(String expression, JsonNode input) {
        try {
            String[] parts = expression.split("==");
            String leftPath = parts[0].trim();
            String rightVal = parts[1].trim().replaceAll("['\"]", "");
            Object left = safeJsonPathRead(input.toString(), leftPath);
            return left.toString().equals(rightVal);
        } catch (Exception e) {
            return false;
        }
    }

    private static String fetchFromDB(String query, String param) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/yourdb";
        String user = "youruser";
        String pass = "yourpass";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, param);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (Exception e) {
            return "DB_ERR";
        }
        return "";
    }

    private static void flattenJson(JsonNode node, String prefix, Map<String, String> out) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry ->
                    flattenJson(entry.getValue(), prefix + entry.getKey() + ".", out));
        } else if (node.isArray()) {
            out.put(prefix.substring(0, prefix.length() - 1), node.toString());
        } else {
            out.put(prefix.substring(0, prefix.length() - 1), node.asText());
        }
    }

    private static void writeDelimited(List<LinkedHashMap<String, String>> rows, String delimiterChar) throws IOException {
        if (rows.isEmpty()) return;
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader(rows.get(0).keySet().toArray(new String[0]))
                .withDelimiter(delimiterChar.charAt(0)));

        for (Map<String, String> row : rows) {
            printer.printRecord(row.values());
        }

        printer.flush();
        System.out.println(writer.toString());
    }
}
