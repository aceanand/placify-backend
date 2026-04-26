package com.placify.service;

import com.placify.model.User;
import com.placify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserMatcherService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Find user by email address
     * @param email Sender's email address
     * @return User entity if found and enabled
     * @throws UserNotFoundException if no matching user or user disabled
     */
    public User findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Email address is empty");
        }
        
        // Case-insensitive email lookup
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UserNotFoundException("No registered user found with email: " + email));
        
        // Verify user is enabled
        if (!user.isEnabled()) {
            throw new UserNotFoundException("User account is disabled: " + email);
        }
        
        return user;
    }
    
    /**
     * Check if email matches registered user
     * @param email Email address to check
     * @return true if user exists and enabled
     */
    public boolean isRegisteredUser(String email) {
        try {
            findUserByEmail(email);
            return true;
        } catch (UserNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Custom exception for user not found errors
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
