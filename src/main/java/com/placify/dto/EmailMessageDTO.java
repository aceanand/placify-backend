package com.placify.dto;

import java.time.LocalDateTime;

public class EmailMessageDTO {
    private Long id;
    private String messageId;
    private String senderEmail;
    private String senderName;
    private String subject;
    private String preview;
    private LocalDateTime receivedDate;
    private Boolean isRead;
    private Boolean hasAttachments;
    
    public EmailMessageDTO() {}
    
    public EmailMessageDTO(Long id, String messageId, String senderEmail, String senderName,
                          String subject, String preview, LocalDateTime receivedDate,
                          Boolean isRead, Boolean hasAttachments) {
        this.id = id;
        this.messageId = messageId;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.subject = subject;
        this.preview = preview;
        this.receivedDate = receivedDate;
        this.isRead = isRead;
        this.hasAttachments = hasAttachments;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getPreview() {
        return preview;
    }
    
    public void setPreview(String preview) {
        this.preview = preview;
    }
    
    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }
    
    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public Boolean getHasAttachments() {
        return hasAttachments;
    }
    
    public void setHasAttachments(Boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }
}
