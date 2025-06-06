package com.example.transformation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({SAXException.class, ParserConfigurationException.class})
    public ResponseEntity<String> handleXmlParsingException(Exception e) {
        logger.warn("XML parsing error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .contentType(MediaType.TEXT_PLAIN)
                             .body("Error parsing XML input: " + e.getMessage());
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<String> handleJsonProcessingException(JsonProcessingException e) {
        logger.warn("JSON configuration processing error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .contentType(MediaType.TEXT_PLAIN)
                             .body("Error processing JSON configuration: " + e.getMessage());
    }

    @ExceptionHandler(XPathExpressionException.class)
    public ResponseEntity<String> handleXPathException(XPathExpressionException e) {
        logger.warn("XPath evaluation error during transformation: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .contentType(MediaType.TEXT_PLAIN)
                             .body("Error in XML transformation due to XPath: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid argument or configuration: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .contentType(MediaType.TEXT_PLAIN)
                             .body("Invalid argument or configuration: " + e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException e) {
        logger.error("I/O error during transformation: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .contentType(MediaType.TEXT_PLAIN)
                             .body("I/O error during transformation: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        logger.error("An unexpected error occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .contentType(MediaType.TEXT_PLAIN)
                             .body("An unexpected error occurred during transformation: " + e.getMessage());
    }
}
