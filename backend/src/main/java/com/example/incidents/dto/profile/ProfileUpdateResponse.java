package com.example.incidents.dto.profile;

import com.example.incidents.dto.UserResponse;

/** "token" est un nouvel access token (son sujet est l'email, qui a pu changer). */
public record ProfileUpdateResponse(UserResponse user, String token) {
}
