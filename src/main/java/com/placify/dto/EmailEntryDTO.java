package com.placify.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailEntryDTO {
    private Long id;
    private String name;
    private Double amount;
    private String department;
    private String senderEmail;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
}
