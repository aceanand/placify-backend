package com.placify.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "interview_questions",
    indexes = {
        @Index(name = "idx_role_round", columnList = "job_role,round_type"),
        @Index(name = "idx_round", columnList = "round_type")
    }
)
public class InterviewQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_role", nullable = false)
    private String jobRole;

    // HR, TECHNICAL, MACHINE_CODING, SYSTEM_DESIGN, BEHAVIORAL, MANAGERIAL
    @Column(name = "round_type", nullable = false)
    private String roundType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "LONGTEXT")
    private String answer;

    // EASY, MEDIUM, HARD
    private String difficulty;

    // kept for backward compat
    private String type;
}
