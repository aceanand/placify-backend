package com.placify.service;

import com.placify.model.EmailVerificationToken;
import com.placify.model.User;
import com.placify.repository.EmailVerificationTokenRepository;
import com.placify.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int EXPIRATION_MINUTES = 15; // OTP expires in 15 minutes
    private static final int MAX_ATTEMPTS = 3;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Generate email verification OTP for new user
     */
    @Transactional
    public void generateVerificationOtp(User user) {
        // Delete any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());
        
        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Generate token for internal tracking
        String token = UUID.randomUUID().toString();
        
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setOtp(otp);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));
        verificationToken.setUsed(false);
        verificationToken.setAttempts(0);
        
        tokenRepository.save(verificationToken);
        
        logger.info("Email verification OTP generated for user: {}", user.getUsername());
        
        // Send OTP via email
        try {
            emailService.sendEmailVerificationOtp(user.getEmail(), otp, user.getFullName());
            logger.info("Email verification OTP sent to: {}", user.getEmail());
        } catch (EmailService.EmailSendException e) {
            logger.error("Failed to send email verification OTP to: {}", user.getEmail(), e);
            throw new EmailSendException("Failed to send verification email");
        }
    }
    
    /**
     * Verify OTP code
     */
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (!userOpt.isPresent()) {
            logger.warn("OTP verification attempted for non-existent email: {}", email);
            return false;
        }
        
        User user = userOpt.get();
        
        // Find the most recent valid token for this user
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findAll().stream()
            .filter(t -> t.getUser().getId().equals(user.getId()))
            .filter(t -> !t.isUsed())
            .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
            .findFirst();
        
        if (!tokenOpt.isPresent()) {
            logger.warn("No valid OTP token found for email: {}", email);
            return false;
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        
        // Check if max attempts exceeded
        if (verificationToken.getAttempts() >= MAX_ATTEMPTS) {
            logger.warn("Max OTP attempts exceeded for email: {}", email);
            verificationToken.setUsed(true);
            tokenRepository.save(verificationToken);
            return false;
        }
        
        // Increment attempts
        verificationToken.setAttempts(verificationToken.getAttempts() + 1);
        tokenRepository.save(verificationToken);
        
        // Verify OTP
        if (!verificationToken.getOtp().equals(otp)) {
            logger.warn("Invalid OTP attempt for email: {}. Attempts: {}/{}", 
                email, verificationToken.getAttempts(), MAX_ATTEMPTS);
            return false;
        }
        
        // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
        
        // Enable user account
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);
        
        logger.info("Email verified successfully for user: {}", user.getUsername());
        return true;
    }
    
    /**
     * Resend verification OTP
     */
    @Transactional
    public void resendVerificationOtp(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (!userOpt.isPresent()) {
            throw new UserNotFoundException("User not found");
        }
        
        User user = userOpt.get();
        
        if (user.isEmailVerified()) {
            throw new AlreadyVerifiedException("Email already verified");
        }
        
        generateVerificationOtp(user);
    }
    
    /**
     * Clean up expired tokens
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        logger.info("Expired email verification tokens cleaned up");
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
     * Custom exception for already verified email
     */
    public static class AlreadyVerifiedException extends RuntimeException {
        public AlreadyVerifiedException(String message) {
            super(message);
        }
    }
    
    /**
     * Custom exception for email send failure
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message) {
            super(message);
        }
    }
}
