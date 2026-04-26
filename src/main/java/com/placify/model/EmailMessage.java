package com.placify.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_messages",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "message_id"}),
       indexes = {
           @Index(name = "idx_account_date", columnList = "account_id,received_date"),
           @Index(name = "idx_account_read", columnList = "account_id,is_read")
       })
public class EmailMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private EmailAccount account;
    
    @Column(name = "message_id", nullable = false)
    private String messageId; // Provider's message ID
    
    @Column(name = "thread_id")
    private String threadId;
    
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;
    
    @Column(name = "sender_name")
    private String senderName;
    
    @Column(columnDefinition = "TEXT")
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String preview; // First 200 chars
    
    @Column(name = "body_html", columnDefinition = "LONGTEXT")
    private String bodyHtml;
    
    @Column(name = "body_text", columnDefinition = "LONGTEXT")
    private String bodyText;
    
    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "has_attachments")
    private Boolean hasAttachments = false;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailAccount getAccount() {
        return account;
    }
    
    public void setAccount(EmailAccount account) {
        this.account = account;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getThreadId() {
        return threadId;
    }
    
    public void setThreadId(String threadId) {
        this.threadId = threadId;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
