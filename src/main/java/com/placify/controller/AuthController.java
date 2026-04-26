package com.placify.controller;

import com.placify.dto.*;
import com.placify.model.Role;
import com.placify.model.User;
import com.placify.repository.RoleRepository;
import com.placify.repository.UserRepository;
import com.placify.security.JwtTokenProvider;
import com.placify.security.UserPrincipal;
import com.placify.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @Autowired
    private com.placify.service.EmailVerificationService emailVerificationService;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Check if user exists and is enabled
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseGet(() -> userRepository.findByEmail(loginRequest.getUsername()).orElse(null));
            
            if (user != null && !user.isEnabled()) {
                if (!user.isEmailVerified()) {
                    return ResponseEntity.status(403).body(new MessageResponse(
                        "Please verify your email address before logging in. Check your inbox for the verification code."
                    ));
                } else {
                    return ResponseEntity.status(403).body(new MessageResponse(
                        "Your account has been disabled. Please contact support."
                    ));
                }
            }
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(new JwtResponse(jwt, 
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getEmail(),
                    roles));
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(403).body(new MessageResponse(
                "Your account is disabled. Please verify your email address."
            ));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body(new MessageResponse(
                "Invalid username or password"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new MessageResponse(
                "An error occurred during login. Please try again."
            ));
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Username is already taken!"));
        }
        
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already in use!"));
        }
        
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setFullName(signupRequest.getFullName());
        user.setPhone(signupRequest.getPhone());
        user.setEnabled(false); // Disabled until email verified
        user.setEmailVerified(false);
        
        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();
        
        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Role not found"));
                        roles.add(adminRole);
                        break;
                    case "employee":
                        Role empRole = roleRepository.findByName(Role.RoleName.ROLE_EMPLOYEE)
                                .orElseThrow(() -> new RuntimeException("Role not found"));
                        roles.add(empRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Role not found"));
                        roles.add(userRole);
                }
            });
        }
        
        user.setRoles(roles);
        userRepository.save(user);
        
        // Send email verification OTP
        try {
            emailVerificationService.generateVerificationOtp(user);
        } catch (Exception e) {
            // If email fails, still return success but log error
            // User can request resend later
        }
        
        return ResponseEntity.ok(new MessageResponse(
            "User registered successfully! Please check your email for verification code."
        ));
    }
    
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody com.placify.dto.VerifyOtpRequest request) {
        boolean isValid = emailVerificationService.verifyOtp(request.getEmail(), request.getOtp());
        
        if (isValid) {
            return ResponseEntity.ok(new MessageResponse("Email verified successfully! You can now login."));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid or expired OTP"));
        }
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody com.placify.dto.ForgotPasswordRequest request) {
        try {
            emailVerificationService.resendVerificationOtp(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Verification code sent to your email"));
        } catch (com.placify.service.EmailVerificationService.UserNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("User not found"));
        } catch (com.placify.service.EmailVerificationService.AlreadyVerifiedException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email already verified"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Failed to send verification code"));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.generateResetOtp(request.getEmail());
        
        // Always return success message for security (don't reveal if email exists)
        return ResponseEntity.ok(new MessageResponse(
            "If an account exists with this email, an OTP has been sent."
        ));
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = passwordResetService.verifyOtp(request.getEmail(), request.getOtp());
        
        if (isValid) {
            return ResponseEntity.ok(new MessageResponse("OTP verified successfully"));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid or expired OTP"));
        }
    }
    
    @PostMapping("/reset-password-with-otp")
    public ResponseEntity<?> resetPasswordWithOtp(@Valid @RequestBody ResetPasswordWithOtpRequest request) {
        try {
            passwordResetService.resetPasswordWithOtp(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset successfully!"));
        } catch (PasswordResetService.InvalidOtpException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset successfully!"));
        } catch (PasswordResetService.InvalidTokenException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordResetService.validateToken(token);
        if (isValid) {
            return ResponseEntity.ok(new MessageResponse("Token is valid"));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Token is invalid or expired"));
        }
    }
}
