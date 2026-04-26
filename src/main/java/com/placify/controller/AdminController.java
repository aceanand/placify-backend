package com.placify.controller;

import com.placify.dto.MessageResponse;
import com.placify.model.Role;
import com.placify.model.User;
import com.placify.repository.RoleRepository;
import com.placify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody Set<String> roleNames) {
        return userRepository.findById(id)
                .map(user -> {
                    Set<Role> roles = user.getRoles();
                    roles.clear();
                    
                    roleNames.forEach(roleName -> {
                        Role.RoleName enumRole;
                        switch (roleName.toLowerCase()) {
                            case "admin":
                                enumRole = Role.RoleName.ROLE_ADMIN;
                                break;
                            case "employee":
                                enumRole = Role.RoleName.ROLE_EMPLOYEE;
                                break;
                            default:
                                enumRole = Role.RoleName.ROLE_USER;
                        }
                        
                        roleRepository.findByName(enumRole).ifPresent(roles::add);
                    });
                    
                    user.setRoles(roles);
                    userRepository.save(user);
                    return ResponseEntity.ok(new MessageResponse("User roles updated successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
