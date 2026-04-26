package com.placify.repository;

import com.placify.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByOtpAndUserId(String otp, Long userId);
    
    void deleteByUserId(Long userId);
    
    void deleteByExpiryDateBefore(LocalDateTime date);
}
