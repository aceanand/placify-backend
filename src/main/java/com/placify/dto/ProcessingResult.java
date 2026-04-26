package com.placify.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessingResult {
    private boolean success;
    private String message;
    private String errorCode;
    
    public static ProcessingResult success(String message) {
        return new ProcessingResult(true, message, null);
    }
    
    public static ProcessingResult error(String message, String errorCode) {
        return new ProcessingResult(false, message, errorCode);
    }
}
