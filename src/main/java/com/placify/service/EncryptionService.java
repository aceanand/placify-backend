package com.placify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {
    
    @Value("${email.token.encryption.key}")
    private String encryptionKey;
    
    private static final String ALGORITHM = "AES";
    
    /**
     * Get a properly sized AES key (16 bytes for AES-128)
     */
    private SecretKeySpec getSecretKey() throws Exception {
        // Decode base64 key if it's base64 encoded
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encryptionKey);
        } catch (Exception e) {
            // If not base64, use the string directly
            keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        }
        
        // Use SHA-256 to hash the key and take first 16 bytes for AES-128
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, 16); // Use only first 16 bytes for AES-128
        
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    public String encrypt(String plainText) {
        try {
            SecretKeySpec secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting token: " + e.getMessage(), e);
        }
    }
    
    public String decrypt(String encryptedText) {
        try {
            SecretKeySpec secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting token: " + e.getMessage(), e);
        }
    }
}
