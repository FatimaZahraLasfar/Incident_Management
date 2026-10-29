package com.example.incidents.dto;

/** Représentation minimale d'un utilisateur dans un incident (pas d'email ni de rôle). */
public record UserSummary(Long id, String fullName) {
}
