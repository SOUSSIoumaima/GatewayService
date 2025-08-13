package com.hsurveys.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/user-service")
    @PostMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        logger.warn("User service is unavailable - returning fallback response");
        return createFallbackResponse("User Service");
    }

    @GetMapping("/organization-service")
    @PostMapping("/organization-service")
    public ResponseEntity<Map<String, Object>> organizationServiceFallback() {
        logger.warn("Organization service is unavailable - returning fallback response");
        return createFallbackResponse("Organization Service");
    }

    @GetMapping("/survey-service")
    @PostMapping("/survey-service")
    public ResponseEntity<Map<String, Object>> surveyServiceFallback() {
        logger.warn("Survey service is unavailable - returning fallback response");
        return createFallbackResponse("Survey Service");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName) {
        Map<String, Object> response = Map.of(
            "error", "Service Unavailable",
            "message", serviceName + " is temporarily unavailable. Please try again later.",
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
} 