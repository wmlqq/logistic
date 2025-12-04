package com.software.logistic.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String role;
    private String username;
    private String email;
    private String phone;
    private String password;
}