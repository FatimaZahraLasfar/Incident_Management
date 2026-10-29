package com.example.incidents.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "Le refresh token est obligatoire") String refreshToken) {
}
