package com.placify.service;

import com.placify.dto.ParsedEmailData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParserService {
    
    /**
     * Parse structured fields from email body
     * @param emailBody Raw email body text
     * @return ParsedEmailData containing extracted fields
     * @throws EmailParsingException if required fields missing or invalid format
     */
    public ParsedEmailData parseEmailBody(String emailBody) {
        if (emailBody == null || emailBody.trim().isEmpty()) {
            throw new EmailParsingException("Email body is empty");
        }
        
        ParsedEmailData data = new ParsedEmailData();
        List<String> missingFields = new ArrayList<>();
        
        // Parse fields using case-insensitive regex
        String nameValue = extractField(emailBody, "name");
        String amountValue = extractField(emailBody, "amount");
        String departmentValue = extractField(emailBody, "department");
        
        // Check for missing fields
        if (nameValue == null) {
            missingFields.add("name");
        } else {
            data.setName(nameValue.trim());
        }
        
        if (amountValue == null) {
            missingFields.add("amount");
        } else {
            data.setAmount(parseAmount(amountValue.trim()));
        }
        
        if (departmentValue == null) {
            missingFields.add("department");
        } else {
            data.setDepartment(departmentValue.trim());
        }
        
        // Throw exception if any fields are missing
        if (!missingFields.isEmpty()) {
            throw new EmailParsingException("Missing required fields: " + String.join(", ", missingFields));
        }
        
        // Validate fields
        validateFields(data);
        
        return data;
    }
    
    /**
     * Extract field value from email body using case-insensitive matching
     */
    private String extractField(String emailBody, String fieldName) {
        Pattern pattern = Pattern.compile("(?i)^\\s*" + fieldName + "\\s*:\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(emailBody);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Parse amount string to Double
     */
    private Double parseAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid amount format: " + amountStr);
        }
    }
    
    /**
     * Validate parsed field values
     * @param data Parsed email data
     * @throws ValidationException if field values invalid
     */
    public void validateFields(ParsedEmailData data) {
        List<String> invalidFields = new ArrayList<>();
        
        // Validate name is non-empty
        if (data.getName() == null || data.getName().trim().isEmpty()) {
            invalidFields.add("name");
        }
        
        // Validate amount is numeric and positive
        if (data.getAmount() == null || data.getAmount() <= 0) {
            invalidFields.add("amount");
        }
        
        // Validate department is non-empty
        if (data.getDepartment() == null || data.getDepartment().trim().isEmpty()) {
            invalidFields.add("department");
        }
        
        if (!invalidFields.isEmpty()) {
            throw new ValidationException("Invalid field values: " + String.join(", ", invalidFields));
        }
    }
    
    /**
     * Custom exception for email parsing errors
     */
    public static class EmailParsingException extends RuntimeException {
        public EmailParsingException(String message) {
            super(message);
        }
    }
    
    /**
     * Custom exception for field validation errors
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
