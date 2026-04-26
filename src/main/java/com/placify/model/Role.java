package com.placify.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleName name;
    
    private String description;
    
    public enum RoleName {
        ROLE_ADMIN,
        ROLE_USER,
        ROLE_EMPLOYEE
    }
}
