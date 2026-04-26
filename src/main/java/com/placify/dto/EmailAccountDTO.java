package com.placify.dto;

import java.time.LocalDateTime;

public class EmailAccountDTO {
    private Long id;
    private String provider;
    private String emailAddress;
    private Boolean isActive;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    
    public EmailAccountDTO() {}
    
    public EmailAccountDTO(Long id, String provider, String emailAddress, Boolean isActive, 
                          LocalDateTime lastSyncAt, LocalDateTime createdAt) {
        this.id = id;
        this.provider = provider;
        this.emailAddress = emailAddress;
        this.isActive = isActive;
        this.lastSyncAt = lastSyncAt;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getEmailAddress() {
        return emailAddress;
    }
    
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }
    
    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
