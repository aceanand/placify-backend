package com.placify.dto;

public class OAuthUrlResponse {
    private String authUrl;
    private String state;
    
    public OAuthUrlResponse() {}
    
    public OAuthUrlResponse(String authUrl, String state) {
        this.authUrl = authUrl;
        this.state = state;
    }
    
    public String getAuthUrl() {
        return authUrl;
    }
    
    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
}
