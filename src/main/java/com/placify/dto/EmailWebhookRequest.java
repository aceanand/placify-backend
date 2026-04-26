package com.placify.dto;

import lombok.Data;

@Data
public class EmailWebhookRequest {
    private String from;
    private String subject;
    private String body;
    private String receivedAt; // ISO 8601 format
}
