package com.placify.controller;

import com.placify.dto.EmailDetailDTO;
import com.placify.dto.EmailMessageDTO;
import com.placify.dto.MessageResponse;
import com.placify.model.EmailAccount;
import com.placify.model.EmailMessage;
import com.placify.model.User;
import com.placify.repository.EmailAccountRepository;
import com.placify.repository.EmailMessageRepository;
import com.placify.repository.UserRepository;
import com.placify.service.EmailSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class EmailMessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailMessageController.class);
    
    @Autowired
    private EmailMessageRepository emailMessageRepository;
    
    @Autowired
    private EmailAccountRepository emailAccountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailSyncService emailSyncService;
    
    /**
     * Get email messages with pagination and optional source filtering
     */
    @GetMapping
    public ResponseEntity<?> getMessages(
            @RequestParam Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String source,
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
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("receivedDate").descending());
            
            Page<EmailMessage> messagePage;
            
            // Filter by source if provided
            if (source != null && !source.isEmpty()) {
                String sourcePattern = "%" + source.toLowerCase() + "%";
                if (unreadOnly != null && unreadOnly) {
                    messagePage = emailMessageRepository.findByAccountAndIsReadAndSenderEmailContainingIgnoreCase(
                        account, false, source, pageable);
                } else {
                    messagePage = emailMessageRepository.findByAccountAndSenderEmailContainingIgnoreCase(
                        account, source, pageable);
                }
            } else {
                if (unreadOnly != null && unreadOnly) {
                    messagePage = emailMessageRepository.findByAccountAndIsRead(account, false, pageable);
                } else {
                    messagePage = emailMessageRepository.findByAccount(account, pageable);
                }
            }
            
            var messages = messagePage.getContent().stream()
                .map(msg -> new EmailMessageDTO(
                    msg.getId(),
                    msg.getMessageId(),
                    msg.getSenderEmail(),
                    msg.getSenderName(),
                    msg.getSubject(),
                    msg.getPreview(),
                    msg.getReceivedDate(),
                    msg.getIsRead(),
                    msg.getHasAttachments()
                ))
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("messages", messages);
            response.put("currentPage", messagePage.getNumber());
            response.put("totalPages", messagePage.getTotalPages());
            response.put("totalItems", messagePage.getTotalElements());
            response.put("hasNext", messagePage.hasNext());
            response.put("hasPrevious", messagePage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching messages", e);
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error fetching messages: " + e.getMessage()));
        }
    }
    
    /**
     * Get email detail
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessageDetail(
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            EmailMessage message = emailMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
            
            // Verify message belongs to user
            if (!message.getAccount().getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new MessageResponse("Access denied"));
            }
            
            EmailDetailDTO detail = new EmailDetailDTO();
            detail.setId(message.getId());
            detail.setMessageId(message.getMessageId());
            detail.setSenderEmail(message.getSenderEmail());
            detail.setSenderName(message.getSenderName());
            detail.setSubject(message.getSubject());
            detail.setBodyHtml(message.getBodyHtml());
            detail.setBodyText(message.getBodyText());
            detail.setReceivedDate(message.getReceivedDate());
            detail.setIsRead(message.getIsRead());
            detail.setHasAttachments(message.getHasAttachments());
            
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            logger.error("Error fetching message detail", e);
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error fetching message: " + e.getMessage()));
        }
    }
    
    /**
     * Mark message as read/unread
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long messageId,
            @RequestParam Boolean isRead,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            EmailMessage message = emailMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
            
            // Verify message belongs to user
            if (!message.getAccount().getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new MessageResponse("Access denied"));
            }
            
            message.setIsRead(isRead);
            emailMessageRepository.save(message);
            
            return ResponseEntity.ok(new MessageResponse("Message marked as " + (isRead ? "read" : "unread")));
        } catch (Exception e) {
            logger.error("Error updating message", e);
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error updating message: " + e.getMessage()));
        }
    }
    
    /**
     * Sync account emails
     */
    @PostMapping("/sync/{accountId}")
    public ResponseEntity<?> syncAccount(
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
            
            int syncedCount = emailSyncService.syncAccount(accountId);
            
            return ResponseEntity.ok(new MessageResponse("Synced " + syncedCount + " emails successfully"));
        } catch (Exception e) {
            logger.error("Error syncing account", e);
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Error syncing account: " + e.getMessage()));
        }
    }
}
