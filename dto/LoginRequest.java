package com.software.logistic.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String role;
    private String account;
    private String password;
}