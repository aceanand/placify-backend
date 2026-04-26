package com.placify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OutlookService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutlookService.class);
    private static final String GRAPH_API_ENDPOINT = "https://graph.microsoft.com/v1.0";
    
    public List<Map<String, Object>> fetchEmails(String accessToken, int maxResults) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = GRAPH_API_ENDPOINT + "/me/messages?$top=" + maxResults + 
                        "&$select=id,subject,from,receivedDateTime,bodyPreview,body,isRead,hasAttachments" +
                        "&$orderby=receivedDateTime desc";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> messages = (List<Map<String, Object>>) responseBody.get("value");
            
            List<Map<String, Object>> emails = new ArrayList<>();
            
            for (Map<String, Object> message : messages) {
                Map<String, Object> emailData = parseOutlookMessage(message);
                emails.add(emailData);
            }
            
            return emails;
        } catch (Exception e) {
            logger.error("Error fetching Outlook messages", e);
            throw new RuntimeException("Error fetching Outlook messages", e);
        }
    }
    
    public String getUserEmail(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                GRAPH_API_ENDPOINT + "/me", HttpMethod.GET, entity, Map.class
            );
            
            Map<String, Object> user = response.getBody();
            return (String) user.get("mail");
        } catch (Exception e) {
            logger.error("Error fetching user email", e);
            return null;
        }
    }
    
    private Map<String, Object> parseOutlookMessage(Map<String, Object> message) {
        Map<String, Object> emailData = new HashMap<>();
        
        emailData.put("messageId", message.get("id"));
        emailData.put("threadId", message.get("conversationId"));
        emailData.put("subject", message.get("subject"));
        
        // Parse sender
        Map<String, Object> from = (Map<String, Object>) message.get("from");
        if (from != null) {
            Map<String, Object> emailAddress = (Map<String, Object>) from.get("emailAddress");
            if (emailAddress != null) {
                emailData.put("senderEmail", emailAddress.get("address"));
                emailData.put("senderName", emailAddress.get("name"));
            }
        }
        
        // Parse date
        String receivedDateTime = (String) message.get("receivedDateTime");
        emailData.put("receivedDate", parseOutlookDate(receivedDateTime));
        
        // Parse body
        Map<String, Object> body = (Map<String, Object>) message.get("body");
        if (body != null) {
            String contentType = (String) body.get("contentType");
            String content = (String) body.get("content");
            
            if ("html".equalsIgnoreCase(contentType)) {
                emailData.put("bodyHtml", content);
                emailData.put("bodyText", stripHtml(content));
            } else {
                emailData.put("bodyText", content);
                emailData.put("bodyHtml", null);
            }
        }
        
        // Preview
        emailData.put("preview", message.get("bodyPreview"));
        
        // Read status
        emailData.put("isRead", message.get("isRead"));
        
        // Attachments
        emailData.put("hasAttachments", message.get("hasAttachments"));
        
        return emailData;
    }
    
    private LocalDateTime parseOutlookDate(String dateStr) {
        try {
            // Outlook date format: "2026-04-25T10:30:00Z"
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            logger.error("Error parsing date: " + dateStr, e);
            return LocalDateTime.now();
        }
    }
    
    private String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("<[^>]*>", "");
    }
}
