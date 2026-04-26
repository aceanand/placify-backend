package com.placify.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("user123: " + encoder.encode("user123"));
        System.out.println("employee123: " + encoder.encode("employee123"));
    }
}
