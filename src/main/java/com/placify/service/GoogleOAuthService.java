package com.placify.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleOAuthService {
    
    @Value("${google.oauth.client-id}")
    private String clientId;
    
    @Value("${google.oauth.client-secret}")
    private String clientSecret;
    
    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;
    
    @Value("${google.oauth.scope}")
    private String scope;
    
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    public String getAuthorizationUrl(String state) {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret,
                Arrays.asList(scope.split(" "))
            )
            .setAccessType("offline")
            .build();
            
            return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(state)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Google OAuth URL", e);
        }
    }
    
    public Map<String, Object> exchangeCodeForTokens(String code) {
        try {
            GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                HTTP_TRANSPORT, JSON_FACTORY,
                clientId, clientSecret, code, redirectUri
            ).execute();
            
            Map<String, Object> tokens = new HashMap<>();
            tokens.put("accessToken", response.getAccessToken());
            tokens.put("refreshToken", response.getRefreshToken());
            tokens.put("expiresInSeconds", response.getExpiresInSeconds());
            
            return tokens;
        } catch (IOException e) {
            throw new RuntimeException("Error exchanging code for tokens", e);
        }
    }
    
    public String refreshAccessToken(String refreshToken) {
        try {
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                HTTP_TRANSPORT, JSON_FACTORY,
                refreshToken, clientId, clientSecret
            ).execute();
            
            return response.getAccessToken();
        } catch (IOException e) {
            throw new RuntimeException("Error refreshing access token", e);
        }
    }
}
