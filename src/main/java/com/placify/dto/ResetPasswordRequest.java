package com.placify.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;
    
    @NotBlank
    @Size(min = 6, max = 40)
    private String newPassword;
}
