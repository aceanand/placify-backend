package com.placify.controller;

import com.placify.dto.EmailSourceDTO;
import com.placify.dto.MessageResponse;
import com.placify.model.EmailSource;
import com.placify.repository.EmailSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/email-sources")
@CrossOrigin(origins = "http://localhost:5173")
public class EmailSourceController {
    
    @Autowired
    private EmailSourceRepository emailSourceRepository;
    
    /**
     * Get all email sources (for users)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<EmailSourceDTO>> getAllSources() {
        List<EmailSource> sources = emailSourceRepository.findAllByOrderByNameAsc();
        List<EmailSourceDTO> dtos = sources.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get active email sources only
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<EmailSourceDTO>> getActiveSources() {
        List<EmailSource> sources = emailSourceRepository.findByIsActiveTrue();
        List<EmailSourceDTO> dtos = sources.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Create new email source (admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSource(@RequestBody EmailSourceDTO dto) {
        try {
            EmailSource source = new EmailSource();
            source.setName(dto.getName());
            source.setDomain(dto.getDomain());
            source.setEmailPattern(dto.getEmailPattern());
            source.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
            source.setIcon(dto.getIcon());
            source.setColor(dto.getColor());
            source.setCreatedAt(LocalDateTime.now());
            source.setUpdatedAt(LocalDateTime.now());
            
            EmailSource saved = emailSourceRepository.save(source);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error creating email source: " + e.getMessage()));
        }
    }
    
    /**
     * Update email source (admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSource(@PathVariable Long id, @RequestBody EmailSourceDTO dto) {
        try {
            EmailSource source = emailSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email source not found"));
            
            source.setName(dto.getName());
            source.setDomain(dto.getDomain());
            source.setEmailPattern(dto.getEmailPattern());
            source.setIsActive(dto.getIsActive());
            source.setIcon(dto.getIcon());
            source.setColor(dto.getColor());
            source.setUpdatedAt(LocalDateTime.now());
            
            EmailSource saved = emailSourceRepository.save(source);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error updating email source: " + e.getMessage()));
        }
    }
    
    /**
     * Toggle email source active status (admin only)
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleSource(@PathVariable Long id) {
        try {
            EmailSource source = emailSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email source not found"));
            
            source.setIsActive(!source.getIsActive());
            source.setUpdatedAt(LocalDateTime.now());
            
            EmailSource saved = emailSourceRepository.save(source);
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error toggling email source: " + e.getMessage()));
        }
    }
    
    /**
     * Delete email source (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSource(@PathVariable Long id) {
        try {
            emailSourceRepository.deleteById(id);
            return ResponseEntity.ok(new MessageResponse("Email source deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error deleting email source: " + e.getMessage()));
        }
    }
    
    private EmailSourceDTO convertToDTO(EmailSource source) {
        return new EmailSourceDTO(
            source.getId(),
            source.getName(),
            source.getDomain(),
            source.getEmailPattern(),
            source.getIsActive(),
            source.getIcon(),
            source.getColor(),
            source.getCreatedAt(),
            source.getUpdatedAt()
        );
    }
}
