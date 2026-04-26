package com.placify.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class GmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);
    private static final String APPLICATION_NAME = "Placify Email Integration";
    
    public List<Map<String, Object>> fetchEmails(String accessToken, int maxResults) {
        try {
            Gmail service = getGmailService(accessToken);
            
            // List messages
            ListMessagesResponse response = service.users().messages()
                .list("me")
                .setMaxResults((long) maxResults)
                .setQ("in:inbox")
                .execute();
            
            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> emails = new ArrayList<>();
            
            for (Message message : messages) {
                try {
                    Message fullMessage = service.users().messages()
                        .get("me", message.getId())
                        .setFormat("full")
                        .execute();
                    
                    Map<String, Object> emailData = parseGmailMessage(fullMessage);
                    emails.add(emailData);
                } catch (Exception e) {
                    logger.error("Error fetching message: " + message.getId(), e);
                }
            }
            
            return emails;
        } catch (Exception e) {
            logger.error("Error fetching Gmail messages", e);
            throw new RuntimeException("Error fetching Gmail messages", e);
        }
    }
    
    public String getUserEmail(String accessToken) {
        try {
            Gmail service = getGmailService(accessToken);
            return service.users().getProfile("me").execute().getEmailAddress();
        } catch (Exception e) {
            logger.error("Error fetching user email", e);
            return null;
        }
    }
    
    private Gmail getGmailService(String accessToken) throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        
        GoogleCredentials credentials = GoogleCredentials.create(
            new AccessToken(accessToken, null)
        );
        
        return new Gmail.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
            .setApplicationName(APPLICATION_NAME)
            .build();
    }
    
    private Map<String, Object> parseGmailMessage(Message message) {
        Map<String, Object> emailData = new HashMap<>();
        
        emailData.put("messageId", message.getId());
        emailData.put("threadId", message.getThreadId());
        
        // Parse headers
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            String name = header.getName();
            String value = header.getValue();
            
            switch (name.toLowerCase()) {
                case "from":
                    emailData.put("senderEmail", extractEmail(value));
                    emailData.put("senderName", extractName(value));
                    break;
                case "subject":
                    emailData.put("subject", value);
                    break;
                case "date":
                    emailData.put("receivedDate", parseDate(value));
                    break;
            }
        }
        
        // Parse body
        String bodyText = getMessageBody(message.getPayload(), "text/plain");
        String bodyHtml = getMessageBody(message.getPayload(), "text/html");
        
        emailData.put("bodyText", bodyText);
        emailData.put("bodyHtml", bodyHtml);
        emailData.put("preview", generatePreview(bodyText, bodyHtml));
        
        // Check for attachments
        emailData.put("hasAttachments", hasAttachments(message.getPayload()));
        
        // Check if read
        List<String> labelIds = message.getLabelIds();
        emailData.put("isRead", labelIds != null && !labelIds.contains("UNREAD"));
        
        return emailData;
    }
    
    private String getMessageBody(MessagePart part, String mimeType) {
        if (part.getMimeType() != null && part.getMimeType().equals(mimeType)) {
            if (part.getBody() != null && part.getBody().getData() != null) {
                return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
            }
        }
        
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                String body = getMessageBody(subPart, mimeType);
                if (body != null) {
                    return body;
                }
            }
        }
        
        return null;
    }
    
    private boolean hasAttachments(MessagePart part) {
        if (part.getFilename() != null && !part.getFilename().isEmpty()) {
            return true;
        }
        
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                if (hasAttachments(subPart)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private String extractEmail(String from) {
        if (from.contains("<") && from.contains(">")) {
            int start = from.indexOf("<") + 1;
            int end = from.indexOf(">");
            return from.substring(start, end);
        }
        return from;
    }
    
    private String extractName(String from) {
        if (from.contains("<")) {
            return from.substring(0, from.indexOf("<")).trim().replaceAll("\"", "");
        }
        return null;
    }
    
    private LocalDateTime parseDate(String dateStr) {
        try {
            // Gmail date format: "Thu, 25 Apr 2026 10:30:00 +0530"
            // Simplified parsing - in production use proper date parser
            return LocalDateTime.now(); // Placeholder
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    private String generatePreview(String bodyText, String bodyHtml) {
        String content = bodyText != null ? bodyText : bodyHtml;
        if (content == null) {
            return "";
        }
        
        // Remove HTML tags if present
        content = content.replaceAll("<[^>]*>", "");
        
        // Get first 200 characters
        if (content.length() > 200) {
            return content.substring(0, 200) + "...";
        }
        return content;
    }
}
