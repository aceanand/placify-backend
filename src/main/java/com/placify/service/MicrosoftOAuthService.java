package com.placify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MicrosoftOAuthService {
    
    @Value("${microsoft.oauth.client-id}")
    private String clientId;
    
    @Value("${microsoft.oauth.client-secret}")
    private String clientSecret;
    
    @Value("${microsoft.oauth.redirect-uri}")
    private String redirectUri;
    
    @Value("${microsoft.oauth.scope}")
    private String scope;
    
    private static final String AUTHORIZATION_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    
    public String getAuthorizationUrl(String state) {
        return AUTHORIZATION_ENDPOINT +
            "?client_id=" + clientId +
            "&response_type=code" +
            "&redirect_uri=" + redirectUri +
            "&response_mode=query" +
            "&scope=" + scope +
            "&state=" + state;
    }
    
    public Map<String, Object> exchangeCodeForTokens(String code) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                TOKEN_ENDPOINT, request, Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> tokens = new HashMap<>();
            tokens.put("accessToken", responseBody.get("access_token"));
            tokens.put("refreshToken", responseBody.get("refresh_token"));
            tokens.put("expiresInSeconds", responseBody.get("expires_in"));
            
            return tokens;
        } catch (Exception e) {
            throw new RuntimeException("Error exchanging code for tokens", e);
        }
    }
    
    public String refreshAccessToken(String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                TOKEN_ENDPOINT, request, Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            return (String) responseBody.get("access_token");
        } catch (Exception e) {
            throw new RuntimeException("Error refreshing access token", e);
        }
    }
}
