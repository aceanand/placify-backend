package com.placify.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParsedJobDTO {
    private Long id;
    private String source;
    private String sourceColor;
    private String sourceIcon;
    private String jobTitle;
    private String companyName;
    private String companyRating;
    private String location;
    private String experienceRequired;
    private String salaryRange;
    private String workMode;
    private String skills;
    private String applyLink;
    private String jobDescription;
    private String senderEmail;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
}
