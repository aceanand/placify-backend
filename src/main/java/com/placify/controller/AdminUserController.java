package com.placify.controller;

import com.placify.dto.MessageResponse;
import com.placify.model.User;
import com.placify.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all users with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> userPage = userRepository.findAll(pageable);
        
        // Convert to DTO to avoid exposing password
        List<Map<String, Object>> userDTOs = userPage.getContent().stream().map(user -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", user.getId());
            dto.put("username", user.getUsername());
            dto.put("email", user.getEmail());
            dto.put("fullName", user.getFullName());
            dto.put("phone", user.getPhone());
            dto.put("enabled", user.isEnabled());
            dto.put("emailVerified", user.isEmailVerified());
            dto.put("createdAt", user.getCreatedAt());
            dto.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", userDTOs);
        response.put("currentPage", userPage.getNumber());
        response.put("totalPages", userPage.getTotalPages());
        response.put("totalItems", userPage.getTotalElements());
        response.put("hasNext", userPage.hasNext());
        response.put("hasPrevious", userPage.hasPrevious());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Toggle user enabled status
     */
    @PutMapping("/{userId}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Toggle enabled status
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        
        logger.info("User {} status changed to: {}", user.getUsername(), 
            user.isEnabled() ? "ENABLED" : "DISABLED");
        
        return ResponseEntity.ok(new MessageResponse(
            "User " + (user.isEnabled() ? "activated" : "deactivated") + " successfully"
        ));
    }
    
    /**
     * Enable user
     */
    @PutMapping("/{userId}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(true);
        userRepository.save(user);
        
        logger.info("User {} enabled by admin", user.getUsername());
        
        return ResponseEntity.ok(new MessageResponse("User activated successfully"));
    }
    
    /**
     * Disable user
     */
    @PutMapping("/{userId}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEnabled(false);
        userRepository.save(user);
        
        logger.info("User {} disabled by admin", user.getUsername());
        
        return ResponseEntity.ok(new MessageResponse("User deactivated successfully"));
    }
    
    /**
     * Delete user
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent deleting yourself
        // This would need current user context, simplified for now
        
        userRepository.delete(user);
        
        logger.info("User {} deleted by admin", user.getUsername());
        
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }
    
    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(User::isEnabled)
                .count();
        long verifiedUsers = userRepository.findAll().stream()
                .filter(User::isEmailVerified)
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("inactiveUsers", totalUsers - activeUsers);
        stats.put("verifiedUsers", verifiedUsers);
        stats.put("unverifiedUsers", totalUsers - verifiedUsers);
        
        return ResponseEntity.ok(stats);
    }
}
