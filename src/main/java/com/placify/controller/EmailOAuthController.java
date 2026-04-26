package com.placify.controller;

import com.placify.dto.EmailAccountDTO;
import com.placify.dto.MessageResponse;
import com.placify.dto.OAuthUrlResponse;
import com.placify.model.EmailAccount;
import com.placify.model.User;
import com.placify.repository.EmailAccountRepository;
import com.placify.repository.UserRepository;
import com.placify.security.JwtTokenProvider;
import com.placify.service.EncryptionService;
import com.placify.service.GmailService;
import com.placify.service.GoogleOAuthService;
import com.placify.service.MicrosoftOAuthService;
import com.placify.service.OutlookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/email/oauth")
@CrossOrigin(origins = "http://localhost:5173")
public class EmailOAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailOAuthController.class);
    
    @Autowired
    private GoogleOAuthService googleOAuthService;
    
    @Autowired
    private MicrosoftOAuthService microsoftOAuthService;
    
    @Autowired
    private GmailService gmailService;
    
    @Autowired
    private OutlookService outlookService;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private EmailAccountRepository emailAccountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private com.placify.repository.EmailMessageRepository emailMessageRepository;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    /**
     * Get OAuth authorization URL
     */
    @GetMapping("/url")
    public ResponseEntity<?> getOAuthUrl(
            @RequestParam String provider,
            @RequestParam String userId
    ) {
        try {
            String state = userId + ":" + provider + ":" + UUID.randomUUID().toString();
            String authUrl;
            
            if ("GMAIL".equalsIgnoreCase(provider)) {
                authUrl = googleOAuthService.getAuthorizationUrl(state);
            } else if ("OUTLOOK".equalsIgnoreCase(provider)) {
                authUrl = microsoftOAuthService.getAuthorizationUrl(state);
            } else {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid provider. Use GMAIL or OUTLOOK"));
            }
            
            return ResponseEntity.ok(new OAuthUrlResponse(authUrl, state));
        } catch (Exception e) {
            logger.error("Error generating OAuth URL", e);
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error generating OAuth URL: " + e.getMessage()));
        }
    }
    
    /**
     * OAuth callback endpoint
     */
    @GetMapping("/callback")
    public void handleOAuthCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response
    ) throws IOException {
        try {
            // Parse state: userId:provider:uuid
            String[] stateParts = state.split(":");
            if (stateParts.length < 2) {
                response.sendRedirect(frontendUrl + "/email-inbox?error=invalid_state");
                return;
            }
            
            Long userId = Long.parseLong(stateParts[0]);
            String provider = stateParts[1];
            
            // Get user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Exchange code for tokens
            Map<String, Object> tokens;
            if ("GMAIL".equalsIgnoreCase(provider)) {
                tokens = googleOAuthService.exchangeCodeForTokens(code);
            } else if ("OUTLOOK".equalsIgnoreCase(provider)) {
                tokens = microsoftOAuthService.exchangeCodeForTokens(code);
            } else {
                response.sendRedirect(frontendUrl + "/email-inbox?error=invalid_provider");
                return;
            }
            
            String accessToken = (String) tokens.get("accessToken");
            String refreshToken = (String) tokens.get("refreshToken");
            Long expiresInLong = (Long) tokens.get("expiresInSeconds");
            int expiresIn = expiresInLong != null ? expiresInLong.intValue() : 3600;
            
            // Get user's email address from provider API
            String emailAddress;
            if ("GMAIL".equalsIgnoreCase(provider)) {
                emailAddress = gmailService.getUserEmail(accessToken);
            } else {
                emailAddress = outlookService.getUserEmail(accessToken);
            }
            
            if (emailAddress == null) {
                response.sendRedirect(frontendUrl + "/email-inbox?error=failed_to_get_email");
                return;
            }
            
            // Check if account already exists
            EmailAccount account = emailAccountRepository
                .findByUserAndEmailAddress(user, emailAddress)
                .orElse(new EmailAccount());
            
            // Save account
            account.setUser(user);
            account.setProvider(EmailAccount.EmailProvider.valueOf(provider.toUpperCase()));
            account.setEmailAddress(emailAddress);
            account.setAccessToken(encryptionService.encrypt(accessToken));
            if (refreshToken != null) {
                account.setRefreshToken(encryptionService.encrypt(refreshToken));
            }
            account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            account.setIsActive(true);
            
            emailAccountRepository.save(account);
            
            logger.info("Email account connected successfully for user: {}", user.getUsername());
            
            // Redirect to frontend with success
            response.sendRedirect(frontendUrl + "/email-inbox?success=true&provider=" + provider);
            
        } catch (Exception e) {
            logger.error("Error handling OAuth callback", e);
            response.sendRedirect(frontendUrl + "/email-inbox?error=" + e.getMessage());
        }
    }
    
    /**
     * Get connected email accounts
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> getConnectedAccounts(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            var accounts = emailAccountRepository.findByUser(user).stream()
                .map(account -> new EmailAccountDTO(
                    account.getId(),
                    account.getProvider().name(),
                    account.getEmailAddress(),
                    account.getIsActive(),
                    account.getLastSyncAt(),
                    account.getCreatedAt()
                ))
                .toList();
            
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            logger.error("Error fetching connected accounts", e);
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error fetching accounts: " + e.getMessage()));
        }
    }
    
    /**
     * Disconnect email account
     */
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<?> disconnectAccount(
            @PathVariable Long accountId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            EmailAccount account = emailAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
            
            // Verify account belongs to user
            if (!account.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new MessageResponse("Access denied"));
            }
            
            // Delete all messages for this account first (FK constraint)
            // Use a more efficient approach with batch deletion
            emailMessageRepository.deleteByAccount(account);
            
            emailAccountRepository.delete(account);
            
            logger.info("Email account disconnected: {}", account.getEmailAddress());
            
            return ResponseEntity.ok(new MessageResponse("Email account disconnected successfully"));
        } catch (Exception e) {
            logger.error("Error disconnecting account", e);
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error disconnecting account: " + e.getMessage()));
        }
    }
}
