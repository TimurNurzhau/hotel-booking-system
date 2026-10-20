package com.example.bookingservice.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String password;
    private String role;
}