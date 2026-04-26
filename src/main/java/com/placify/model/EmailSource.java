package com.placify.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "email_sources")
public class EmailSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name; // e.g., "LinkedIn", "Naukri", "Superset"
    
    @Column(nullable = false)
    private String domain; // e.g., "linkedin.com", "naukri.com"
    
    @Column(nullable = false)
    private String emailPattern; // e.g., "@linkedin.com", "@naukri.com"
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private String icon; // emoji or icon class
    
    @Column(nullable = false)
    private String color; // hex color for UI
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
