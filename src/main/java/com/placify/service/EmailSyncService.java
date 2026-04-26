package com.placify.service;

import com.placify.model.EmailAccount;
import com.placify.model.EmailMessage;
import com.placify.repository.EmailAccountRepository;
import com.placify.repository.EmailMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EmailSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailSyncService.class);
    
    @Autowired
    private EmailAccountRepository emailAccountRepository;
    
    @Autowired
    private EmailMessageRepository emailMessageRepository;
    
    @Autowired
    private GmailService gmailService;
    
    @Autowired
    private OutlookService outlookService;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private GoogleOAuthService googleOAuthService;
    
    @Autowired
    private MicrosoftOAuthService microsoftOAuthService;
    
    @Value("${email.sync.max-messages:100}")
    private int maxMessages;
    
    @Value("${email.sync.enabled:true}")
    private boolean syncEnabled;
    
    /**
     * Sync a specific email account
     */
    @Transactional
    public int syncAccount(Long accountId) {
        EmailAccount account = emailAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getIsActive()) {
            logger.warn("Account is inactive: {}", account.getEmailAddress());
            return 0;
        }
        
        try {
            // Check if token needs refresh
            if (account.getTokenExpiresAt() != null && 
                account.getTokenExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
                refreshToken(account);
            }
            
            // Decrypt access token
            String accessToken = encryptionService.decrypt(account.getAccessToken());
            
            // Fetch emails based on provider
            List<Map<String, Object>> emails;
            if (account.getProvider() == EmailAccount.EmailProvider.GMAIL) {
                emails = gmailService.fetchEmails(accessToken, maxMessages);
            } else {
                emails = outlookService.fetchEmails(accessToken, maxMessages);
            }
            
            // Save emails to database
            int savedCount = 0;
            for (Map<String, Object> emailData : emails) {
                try {
                    saveEmail(account, emailData);
                    savedCount++;
                } catch (Exception e) {
                    logger.error("Error saving email: " + emailData.get("messageId"), e);
                }
            }
            
            // Update last sync time
            account.setLastSyncAt(LocalDateTime.now());
            emailAccountRepository.save(account);
            
            logger.info("Synced {} emails for account: {}", savedCount, account.getEmailAddress());
            
            return savedCount;
        } catch (Exception e) {
            logger.error("Error syncing account: " + account.getEmailAddress(), e);
            throw new RuntimeException("Error syncing account", e);
        }
    }
    
    /**
     * Sync all active accounts (scheduled job)
     */
    @Scheduled(fixedDelayString = "${email.sync.interval:900000}") // 15 minutes
    public void syncAllAccounts() {
        if (!syncEnabled) {
            return;
        }
        
        logger.info("Starting scheduled email sync for all accounts");
        
        List<EmailAccount> activeAccounts = emailAccountRepository.findByIsActive(true);
        
        for (EmailAccount account : activeAccounts) {
            try {
                syncAccount(account.getId());
            } catch (Exception e) {
                logger.error("Error syncing account: " + account.getEmailAddress(), e);
            }
        }
        
        logger.info("Completed scheduled email sync");
    }
    
    private void refreshToken(EmailAccount account) {
        try {
            String refreshToken = encryptionService.decrypt(account.getRefreshToken());
            String newAccessToken;
            
            if (account.getProvider() == EmailAccount.EmailProvider.GMAIL) {
                newAccessToken = googleOAuthService.refreshAccessToken(refreshToken);
            } else {
                newAccessToken = microsoftOAuthService.refreshAccessToken(refreshToken);
            }
            
            account.setAccessToken(encryptionService.encrypt(newAccessToken));
            account.setTokenExpiresAt(LocalDateTime.now().plusHours(1));
            emailAccountRepository.save(account);
            
            logger.info("Refreshed token for account: {}", account.getEmailAddress());
        } catch (Exception e) {
            logger.error("Error refreshing token for account: " + account.getEmailAddress(), e);
            account.setIsActive(false);
            emailAccountRepository.save(account);
        }
    }
    
    private void saveEmail(EmailAccount account, Map<String, Object> emailData) {
        String messageId = (String) emailData.get("messageId");
        
        // Check if email already exists
        if (emailMessageRepository.findByAccountAndMessageId(account, messageId).isPresent()) {
            return; // Skip duplicate
        }
        
        EmailMessage message = new EmailMessage();
        message.setAccount(account);
        message.setMessageId(messageId);
        message.setThreadId((String) emailData.get("threadId"));
        message.setSenderEmail((String) emailData.get("senderEmail"));
        message.setSenderName((String) emailData.get("senderName"));
        message.setSubject((String) emailData.get("subject"));
        message.setPreview((String) emailData.get("preview"));
        message.setBodyHtml((String) emailData.get("bodyHtml"));
        message.setBodyText((String) emailData.get("bodyText"));
        message.setReceivedDate((LocalDateTime) emailData.get("receivedDate"));
        message.setIsRead((Boolean) emailData.get("isRead"));
        message.setHasAttachments((Boolean) emailData.get("hasAttachments"));
        
        emailMessageRepository.save(message);
    }
}
