package com.placify.config;

import com.placify.model.Role;
import com.placify.model.User;
import com.placify.repository.RoleRepository;
import com.placify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Initialize roles only if they don't exist
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName(Role.RoleName.ROLE_ADMIN);
            adminRole.setDescription("Administrator role with full access");
            roleRepository.save(adminRole);
            
            Role userRole = new Role();
            userRole.setName(Role.RoleName.ROLE_USER);
            userRole.setDescription("Regular user role");
            roleRepository.save(userRole);
            
            Role employeeRole = new Role();
            employeeRole.setName(Role.RoleName.ROLE_EMPLOYEE);
            employeeRole.setDescription("Employee role with extended access");
            roleRepository.save(employeeRole);
            
            System.out.println("Roles initialized successfully!");
        }
        
        // Create default admin user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@placify.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setPhone("1234567890");
            admin.setEnabled(true);
            admin.setEmailVerified(true);
            
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(roleRepository.findByName(Role.RoleName.ROLE_ADMIN).get());
            adminRoles.add(roleRepository.findByName(Role.RoleName.ROLE_USER).get());
            admin.setRoles(adminRoles);
            
            userRepository.save(admin);
            System.out.println("Default admin user created: admin/admin123");
        }
        
        // Create default test user if it doesn't exist
        if (!userRepository.existsByUsername("testuser")) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("test@placify.com");
            testUser.setPassword(passwordEncoder.encode("password123"));
            testUser.setFullName("Test User");
            testUser.setPhone("1234567890");
            testUser.setEnabled(true);
            testUser.setEmailVerified(true);
            
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(roleRepository.findByName(Role.RoleName.ROLE_USER).get());
            testUser.setRoles(userRoles);
            
            userRepository.save(testUser);
            System.out.println("Default test user created: testuser/password123");
        }
        
        // Update admin email if admin user exists
        userRepository.findByUsername("admin").ifPresent(admin -> {
            admin.setEmail("ashwinianand920@gmail.com");
            admin.setEnabled(true);
            admin.setEmailVerified(true);
            userRepository.save(admin);
            System.out.println("Admin email updated to: ashwinianand920@gmail.com");
        });
        
        // Enable and verify all existing users (for migration)
        userRepository.findAll().forEach(user -> {
            if (!user.isEnabled() || !user.isEmailVerified()) {
                user.setEnabled(true);
                user.setEmailVerified(true);
                userRepository.save(user);
            }
        });
        
        System.out.println("DataInitializer: Initialization complete");
        System.out.println("Total users in database: " + userRepository.count());
    }
}
