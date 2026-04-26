package com.placify.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "skill_keywords")
public class SkillKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRole jobRole;
    
    @Column(nullable = false)
    private String keyword;
    
    @Column(nullable = false)
    private String category; // Core Languages, Frameworks, Databases, etc.
    
    @Column(name = "priority_level")
    private Integer priorityLevel = 1; // 1=Critical, 2=Important, 3=Nice to have
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
