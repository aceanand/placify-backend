package com.placify.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "ats_configuration")
public class AtsConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;
    
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "value_type")
    private String valueType; // INTEGER, DOUBLE, STRING, BOOLEAN, JSON
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
