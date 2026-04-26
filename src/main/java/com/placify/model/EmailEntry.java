package com.placify.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "email_entries")
public class EmailEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String senderEmail;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private String department;
    
    @Column(nullable = false)
    private LocalDateTime receivedAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
