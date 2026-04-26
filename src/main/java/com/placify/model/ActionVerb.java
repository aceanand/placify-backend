package com.placify.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "action_verbs")
public class ActionVerb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String verb;
    
    @Column(name = "impact_level")
    private String impactLevel; // HIGH, MEDIUM, LOW
    
    @Column(name = "impact_score")
    private Double impactScore; // 1.0, 0.5, 0.2
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
