package com.placify.controller;

import com.placify.dto.MessageResponse;
import com.placify.dto.UpdateProfileRequest;
import com.placify.model.User;
import com.placify.repository.UserRepository;
import com.placify.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String UPLOAD_DIR = "uploads/profile-pictures/";
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(user);
    }
    
    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email is already in use!"));
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        userRepository.save(user);
        logger.info("Profile updated for user: {}", user.getUsername());
        
        return ResponseEntity.ok(user);
    }
    
    /**
     * Upload profile picture
     */
    @PostMapping("/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please select a file"));
            }
            
            // Check file size (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("File size must be less than 5MB"));
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Only image files are allowed"));
            }
            
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = user.getId() + "_" + UUID.randomUUID().toString() + extension;
            
            // Delete old profile picture if exists
            if (user.getProfilePicture() != null) {
                try {
                    Path oldFile = uploadPath.resolve(user.getProfilePicture());
                    Files.deleteIfExists(oldFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete old profile picture: {}", e.getMessage());
                }
            }
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Update user
            user.setProfilePicture(filename);
            userRepository.save(user);
            
            logger.info("Profile picture uploaded for user: {}", user.getUsername());
            
            return ResponseEntity.ok(new MessageResponse("Profile picture uploaded successfully"));
            
        } catch (IOException e) {
            logger.error("Failed to upload profile picture", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Failed to upload profile picture"));
        }
    }
    
    /**
     * Get profile picture
     */
    @GetMapping("/profile-picture/{userId}")
    public ResponseEntity<Resource> getProfilePicture(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getProfilePicture() == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = Paths.get(UPLOAD_DIR).resolve(user.getProfilePicture());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Failed to load profile picture", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete profile picture
     */
    @DeleteMapping("/profile-picture")
    public ResponseEntity<?> deleteProfilePicture(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getProfilePicture() != null) {
                Path filePath = Paths.get(UPLOAD_DIR).resolve(user.getProfilePicture());
                Files.deleteIfExists(filePath);
                
                user.setProfilePicture(null);
                userRepository.save(user);
                
                logger.info("Profile picture deleted for user: {}", user.getUsername());
            }
            
            return ResponseEntity.ok(new MessageResponse("Profile picture deleted successfully"));
            
        } catch (IOException e) {
            logger.error("Failed to delete profile picture", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Failed to delete profile picture"));
        }
    }
}
