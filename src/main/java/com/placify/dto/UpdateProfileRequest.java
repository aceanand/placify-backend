package com.placify.dto;

import lombok.Data;
import javax.validation.constraints.Email;

@Data
public class UpdateProfileRequest {
    private String fullName;
    
    @Email
    private String email;
    
    private String phone;
}
