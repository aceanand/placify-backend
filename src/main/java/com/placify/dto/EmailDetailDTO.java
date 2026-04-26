package com.placify.dto;

import java.time.LocalDateTime;

public class EmailDetailDTO {
    private Long id;
    private String messageId;
    private String senderEmail;
    private String senderName;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private LocalDateTime receivedDate;
    private Boolean isRead;
    private Boolean hasAttachments;
    
    public EmailDetailDTO() {}
    
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
    
    public String getBodyHtml() {
        return bodyHtml;
    }
    
    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }
    
    public String getBodyText() {
        return bodyText;
    }
    
    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
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
