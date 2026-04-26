package com.placify.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parsed_jobs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_received_at", columnList = "received_at")
})
public class ParsedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Source info
    @Column(nullable = false)
    private String source; // "LinkedIn", "Naukri", "Indeed", etc.

    @Column(name = "source_color")
    private String sourceColor;

    @Column(name = "source_icon")
    private String sourceIcon;

    // Job details
    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_rating")
    private String companyRating;

    @Column
    private String location;

    @Column(name = "experience_required")
    private String experienceRequired;

    @Column(name = "salary_range")
    private String salaryRange;

    @Column(name = "work_mode")
    private String workMode; // "In office", "Remote", "Hybrid"

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(name = "apply_link", columnDefinition = "TEXT")
    private String applyLink;

    @Column(name = "job_description", columnDefinition = "LONGTEXT")
    private String jobDescription;

    // Email metadata
    @Column(name = "email_message_id")
    private Long emailMessageId;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceColor() { return sourceColor; }
    public void setSourceColor(String sourceColor) { this.sourceColor = sourceColor; }
    public String getSourceIcon() { return sourceIcon; }
    public void setSourceIcon(String sourceIcon) { this.sourceIcon = sourceIcon; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyRating() { return companyRating; }
    public void setCompanyRating(String companyRating) { this.companyRating = companyRating; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getExperienceRequired() { return experienceRequired; }
    public void setExperienceRequired(String experienceRequired) { this.experienceRequired = experienceRequired; }
    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getApplyLink() { return applyLink; }
    public void setApplyLink(String applyLink) { this.applyLink = applyLink; }
    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }
    public Long getEmailMessageId() { return emailMessageId; }
    public void setEmailMessageId(Long emailMessageId) { this.emailMessageId = emailMessageId; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
