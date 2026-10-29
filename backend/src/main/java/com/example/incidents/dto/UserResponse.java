package com.example.incidents.dto;

import com.example.incidents.enums.Role;

import java.time.Instant;

public record UserResponse(Long id, String fullName, String email, Role role, boolean enabled, Instant createdAt) {
}
