package com.placify.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ResetPasswordWithOtpRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;
    
    @NotBlank
    @Size(min = 6, max = 40)
    private String newPassword;
}
