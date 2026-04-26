package com.placify.controller;

import com.placify.model.User;
import com.placify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/hello")
    public String hello() {
        return "Backend is working!";
    }
    
    @PostMapping("/echo")
    public String echo(@RequestBody String message) {
        return "Echo: " + message;
    }
    
    @GetMapping("/reset-passwords")
    public Map<String, String> resetPasswords() {
        Map<String, String> result = new HashMap<>();
        
        // Reset admin password
        userRepository.findByUsername("admin").ifPresent(user -> {
            user.setPassword(passwordEncoder.encode("admin123"));
            userRepository.save(user);
            result.put("admin", "Password reset to: admin123");
        });
        
        // Reset user password
        userRepository.findByUsername("user").ifPresent(user -> {
            user.setPassword(passwordEncoder.encode("user123"));
            userRepository.save(user);
            result.put("user", "Password reset to: user123");
        });
        
        // Reset employee password
        userRepository.findByUsername("employee").ifPresent(user -> {
            user.setPassword(passwordEncoder.encode("employee123"));
            userRepository.save(user);
            result.put("employee", "Password reset to: employee123");
        });
        
        return result;
    }
}
