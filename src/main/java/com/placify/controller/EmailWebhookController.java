package com.placify.controller;

import com.placify.dto.EmailWebhookRequest;
import com.placify.dto.ProcessingResult;
import com.placify.service.EmailProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "http://localhost:5173")
public class EmailWebhookController {
    
    @Autowired
    private EmailProcessingService emailProcessingService;
    
    @PostMapping("/webhook")
    public ResponseEntity<?> receiveEmail(@RequestBody EmailWebhookRequest request) {
        ProcessingResult result = emailProcessingService.processIncomingEmail(request);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        
        // Map error codes to HTTP status codes
        switch (result.getErrorCode()) {
            case "USER_NOT_FOUND":
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            case "PARSING_ERROR":
            case "VALIDATION_ERROR":
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            case "INTERNAL_ERROR":
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}
