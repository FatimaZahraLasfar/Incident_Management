package com.example.incidents.dto.auth;

import com.example.incidents.enums.Role;

/**
 * Réponse de login / refresh. "token", "role" et "fullName" sont exigés par le cahier des charges ;
 * "token" est l'access token JWT, "expiresIn" sa durée de vie en secondes.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        long expiresIn,
        Long id,
        String fullName,
        String email,
        Role role) {
}
