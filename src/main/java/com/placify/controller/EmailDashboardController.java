package com.placify.controller;

import com.placify.dto.EmailEntryDTO;
import com.placify.model.EmailEntry;
import com.placify.repository.EmailEntryRepository;
import com.placify.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-entries")
@CrossOrigin(origins = "http://localhost:5173")
public class EmailDashboardController {
    
    @Autowired
    private EmailEntryRepository emailEntryRepository;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Page<EmailEntryDTO>> getUserEmailEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        // Extract user ID from JWT token
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();
        
        // Create pageable
        Pageable pageable = PageRequest.of(page, size);
        
        // Query email entries for user
        Page<EmailEntry> entries = emailEntryRepository.findByUserIdOrderByReceivedAtDesc(userId, pageable);
        
        // Convert to DTOs
        Page<EmailEntryDTO> dtos = entries.map(this::convertToDTO);
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Convert EmailEntry entity to DTO
     */
    private EmailEntryDTO convertToDTO(EmailEntry entry) {
        return new EmailEntryDTO(
            entry.getId(),
            entry.getName(),
            entry.getAmount(),
            entry.getDepartment(),
            entry.getSenderEmail(),
            entry.getReceivedAt(),
            entry.getCreatedAt()
        );
    }
}
