package com.placify.service;

import com.placify.dto.EmailWebhookRequest;
import com.placify.dto.ParsedEmailData;
import com.placify.dto.ProcessingResult;
import com.placify.model.EmailEntry;
import com.placify.model.User;
import com.placify.repository.EmailEntryRepository;
import com.placify.service.EmailParserService.EmailParsingException;
import com.placify.service.EmailParserService.ValidationException;
import com.placify.service.UserMatcherService.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class EmailProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailProcessingService.class);
    
    @Autowired
    private UserMatcherService userMatcherService;
    
    @Autowired
    private EmailParserService emailParserService;
    
    @Autowired
    private EmailEntryRepository emailEntryRepository;
    
    /**
     * Process incoming email webhook
     * @param request Email webhook request containing sender, body, timestamp
     * @return ProcessingResult with success status and message
     */
    @Transactional
    public ProcessingResult processIncomingEmail(EmailWebhookRequest request) {
        // Generate unique request ID for logging correlation
        String requestId = "req_" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        
        try {
            logger.info("Received email from: {}", request.getFrom());
            
            // Extract sender email, body, and timestamp
            String senderEmail = request.getFrom();
            String emailBody = request.getBody();
            LocalDateTime receivedAt = parseTimestamp(request.getReceivedAt());
            
            // Find user by email
            User user;
            try {
                user = userMatcherService.findUserByEmail(senderEmail);
                logger.info("User matched: userId={}", user.getId());
            } catch (UserNotFoundException e) {
                logger.warn("User not found for email: {}, timestamp: {}", senderEmail, receivedAt);
                return ProcessingResult.error(e.getMessage(), "USER_NOT_FOUND");
            }
            
            // Parse email body
            ParsedEmailData parsedData;
            try {
                parsedData = emailParserService.parseEmailBody(emailBody);
                logger.info("Parsed fields: name={}, amount={}, department={}", 
                    parsedData.getName(), parsedData.getAmount(), parsedData.getDepartment());
            } catch (EmailParsingException e) {
                logger.warn("Email parsing error: {}, email body: {}", e.getMessage(), emailBody);
                return ProcessingResult.error(e.getMessage(), "PARSING_ERROR");
            } catch (ValidationException e) {
                logger.warn("Validation error: {}", e.getMessage());
                return ProcessingResult.error(e.getMessage(), "VALIDATION_ERROR");
            }
            
            // Create EmailEntry entity
            EmailEntry emailEntry = new EmailEntry();
            emailEntry.setUser(user);
            emailEntry.setSenderEmail(senderEmail);
            emailEntry.setName(parsedData.getName());
            emailEntry.setAmount(parsedData.getAmount());
            emailEntry.setDepartment(parsedData.getDepartment());
            emailEntry.setReceivedAt(receivedAt);
            emailEntry.setCreatedAt(LocalDateTime.now());
            
            // Save to database
            try {
                EmailEntry saved = emailEntryRepository.save(emailEntry);
                logger.info("Email entry created: entryId={}", saved.getId());
                return ProcessingResult.success("Email processed successfully");
            } catch (Exception e) {
                logger.error("Database error: {}", e.getMessage(), e);
                throw e; // Trigger transaction rollback
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error processing email: {}", e.getMessage(), e);
            return ProcessingResult.error("Internal server error", "INTERNAL_ERROR");
        } finally {
            MDC.remove("requestId");
        }
    }
    
    /**
     * Parse ISO 8601 timestamp string to LocalDateTime
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // Handle ISO 8601 format with 'Z' timezone
            if (timestamp.endsWith("Z")) {
                timestamp = timestamp.substring(0, timestamp.length() - 1);
            }
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp: {}, using current time", timestamp);
            return LocalDateTime.now();
        }
    }
}
