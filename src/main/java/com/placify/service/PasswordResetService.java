package com.placify.service;

import com.placify.model.PasswordResetToken;
import com.placify.model.User;
import com.placify.repository.PasswordResetTokenRepository;
import com.placify.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int EXPIRATION_MINUTES = 10; // OTP expires in 10 minutes
    private static final int MAX_ATTEMPTS = 3;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Generate password reset OTP for user
     * @param email User's email address
     */
    @Transactional
    public void generateResetOtp(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (!userOpt.isPresent()) {
            logger.warn("Password reset requested for non-existent email: {}", email);
            // Don't throw exception - for security, don't reveal if email exists
            return;
        }
        
        User user = userOpt.get();
        
        // Delete any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());
        
        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Generate token for internal tracking
        String token = UUID.randomUUID().toString();
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setOtp(otp);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));
        resetToken.setUsed(false);
        resetToken.setAttempts(0);
        
        tokenRepository.save(resetToken);
        
        logger.info("Password reset OTP generated for user: {}", user.getUsername());
        
        // Send OTP via email
        try {
            emailService.sendPasswordResetOtp(email, otp);
            logger.info("Password reset OTP email sent to: {}", email);
        } catch (EmailService.EmailSendException e) {
            logger.error("Failed to send password reset OTP email to: {}", email, e);
            // Don't throw exception - OTP is still valid, user can try again
        }
    }
    
    /**
     * Verify OTP code
     * @param email User's email
     * @param otp OTP code to verify
     * @return true if OTP is valid
     */
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (!userOpt.isPresent()) {
            logger.warn("OTP verification attempted for non-existent email: {}", email);
            return false;
        }
        
        User user = userOpt.get();
        
        // Find the most recent token for this user
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(
            tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
                .findFirst()
                .map(PasswordResetToken::getToken)
                .orElse("")
        );
        
        if (!tokenOpt.isPresent()) {
            logger.warn("No valid OTP token found for email: {}", email);
            return false;
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        // Check if max attempts exceeded
        if (resetToken.getAttempts() >= MAX_ATTEMPTS) {
            logger.warn("Max OTP attempts exceeded for email: {}", email);
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
            return false;
        }
        
        // Increment attempts
        resetToken.setAttempts(resetToken.getAttempts() + 1);
        tokenRepository.save(resetToken);
        
        // Verify OTP
        if (!resetToken.getOtp().equals(otp)) {
            logger.warn("Invalid OTP attempt for email: {}. Attempts: {}/{}", 
                email, resetToken.getAttempts(), MAX_ATTEMPTS);
            return false;
        }
        
        logger.info("OTP verified successfully for email: {}", email);
        return true;
    }
    
    /**
     * Reset password using OTP
     * @param email User's email
     * @param otp OTP code
     * @param newPassword New password
     * @throws InvalidOtpException if OTP is invalid
     */
    @Transactional
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (!userOpt.isPresent()) {
            throw new InvalidOtpException("Invalid email or OTP");
        }
        
        User user = userOpt.get();
        
        // Find the token
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findAll().stream()
            .filter(t -> t.getUser().getId().equals(user.getId()))
            .filter(t -> !t.isUsed())
            .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
            .filter(t -> t.getOtp().equals(otp))
            .findFirst();
        
        if (!tokenOpt.isPresent()) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        // Check attempts
        if (resetToken.getAttempts() >= MAX_ATTEMPTS) {
            throw new InvalidOtpException("Maximum OTP attempts exceeded");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        logger.info("Password reset successful for user: {}", user.getUsername());
    }
    
    /**
     * Validate reset token
     * @param token Reset token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);
        
        if (!resetTokenOpt.isPresent()) {
            return false;
        }
        
        PasswordResetToken resetToken = resetTokenOpt.get();
        
        // Check if token is expired
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Expired reset token used: {}", token);
            return false;
        }
        
        // Check if token already used
        if (resetToken.isUsed()) {
            logger.warn("Already used reset token: {}", token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Reset user password using token
     * @param token Reset token
     * @param newPassword New password
     * @throws InvalidTokenException if token invalid or expired
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(token);
        
        if (!resetTokenOpt.isPresent()) {
            throw new InvalidTokenException("Invalid reset token");
        }
        
        PasswordResetToken resetToken = resetTokenOpt.get();
        
        // Validate token
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset token has expired");
        }
        
        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Reset token has already been used");
        }
        
        // Update user password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        logger.info("Password reset successful for user: {}", user.getUsername());
    }
    
    /**
     * Clean up expired tokens
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        logger.info("Expired password reset tokens cleaned up");
    }
    
    /**
     * Custom exception for user not found
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
    
    /**
     * Custom exception for invalid OTP
     */
    public static class InvalidOtpException extends RuntimeException {
        public InvalidOtpException(String message) {
            super(message);
        }
    }
    
    /**
     * Custom exception for invalid token
     */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
