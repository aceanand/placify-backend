package com.placify.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String industry;
    private String location;
    private String website;
    private String logo;          // emoji or icon
    private String color;         // brand color hex
    private String size;          // "10K-50K employees"
    private String founded;       // "1998"
    private String rating;        // "4.2"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String hiringStrategy;

    @Column(columnDefinition = "TEXT")
    private String interviewProcess;   // JSON array of rounds

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;     // comma-separated

    @Column(columnDefinition = "TEXT")
    private String techStack;          // comma-separated

    @Column(columnDefinition = "TEXT")
    private String interviewTips;      // JSON array of tips

    @Column(columnDefinition = "TEXT")
    private String salaryRange;        // "₹12L – ₹45L"

    @Column(columnDefinition = "TEXT")
    private String benefits;           // comma-separated

    private String difficulty;         // EASY | MEDIUM | HARD
    private String workCulture;        // Remote | Hybrid | In-office
}
