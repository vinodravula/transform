package com.example.transformation.controller;

import com.example.transformation.service.TransformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
// import java.util.List; // No longer List<String> from parseXml

@RestController
public class TransformationController {

    private final TransformationService transformationService;

    @Autowired
    public TransformationController(TransformationService transformationService) {
        this.transformationService = transformationService;
    }

    @PostMapping(value = "/transform", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> transformData(
            @RequestParam("xmlData") String xmlData,
            @RequestParam("jsonConfig") String jsonConfig) {

        try {
            // Step 1: Parse XML into a DOM Document
            Document xmlDoc = transformationService.parseXml(xmlData);

            // Step 2: Generate flat file string using the DOM Document and JSON config
            String flatFileContent = transformationService.generateFlatFile(xmlDoc, jsonConfig);

            return ResponseEntity.ok(flatFileContent);

        } catch (ParserConfigurationException | SAXException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error parsing XML: " + e.getMessage());
        } catch (XPathExpressionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error during XML transformation (XPath): " + e.getMessage());
        } catch (IOException e) {
            // This can be from XML parsing (InputSource) or JSON parsing (objectMapper.readValue)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing input data (XML or JSON): " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // This can be from unsupported flat file type or other issues in generateFlatFile
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Catch-all for any other unexpected errors
            e.printStackTrace(); // Good practice to log the stack trace for unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
