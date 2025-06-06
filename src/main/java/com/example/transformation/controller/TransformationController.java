package com.example.transformation.controller;

import com.example.transformation.service.TransformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import com.example.transformation.service.TransformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
// Specific exception for JSON processing
import com.fasterxml.jackson.core.JsonProcessingException;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

@RestController
@Tag(name = "Transformation API", description = "Endpoints for XML to Flat File Transformation")
public class TransformationController {

    private static final Logger logger = LoggerFactory.getLogger(TransformationController.class);
    private final TransformationService transformationService;

    @Autowired
    public TransformationController(TransformationService transformationService) {
        this.transformationService = transformationService;
    }

    @Operation(summary = "Transform XML to Flat File",
            description = "Accepts XML data and a JSON configuration, then transforms the XML into a flat file format (fixed-width or delimited) based on the provided configuration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transformation successful",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "field1,field2\\nvalue1,value2"))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input XML, JSON configuration, or parameters",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Unexpected error during transformation",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @PostMapping(value = "/transform", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> transformData(
            @Parameter(description = "The XML data string to be transformed.", required = true, example = "<root><item>value</item></root>")
            @RequestParam("xmlData") String xmlData,
            @Parameter(description = "The JSON configuration string defining the transformation rules.", required = true, example = "{\"type\":\"delimited\",\"delimiter\":\",\",\"fields\":[{\"name\":\"OutField\",\"sourceXpath\":\"/root/item\"}]}")
            @RequestParam("jsonConfig") String jsonConfig) throws Exception { // Allow exceptions to propagate

        // Basic input validation can remain, or be handled by validation annotations/filters
        if (xmlData == null || xmlData.trim().isEmpty()) {
            logger.warn("Validation error: XML data is empty.");
            // This specific return can be kept or let GlobalExceptionHandler handle a custom ValidationException
            return ResponseEntity.badRequest().body("XML data must not be empty.");
        }
        if (jsonConfig == null || jsonConfig.trim().isEmpty()) {
            logger.warn("Validation error: JSON configuration is empty.");
            return ResponseEntity.badRequest().body("JSON configuration must not be empty.");
        }

        logger.info("Received transformation request. XML data length: approx {}, JSON config length: approx {}", xmlData.length(), jsonConfig.length());

        // All exceptions from service layer will now be caught by GlobalExceptionHandler
        Document xmlDoc = transformationService.parseXml(xmlData);
        String flatFileContent = transformationService.generateFlatFile(xmlDoc, jsonConfig);

        logger.info("Transformation successful. Output content length: {}", flatFileContent.length());
        return ResponseEntity.ok(flatFileContent);
    }
}
