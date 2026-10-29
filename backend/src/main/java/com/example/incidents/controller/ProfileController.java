package com.example.incidents.controller;

import com.example.incidents.dto.UserResponse;
import com.example.incidents.dto.auth.AuthResponse;
import com.example.incidents.dto.profile.PasswordChangeRequest;
import com.example.incidents.dto.profile.ProfileUpdateRequest;
import com.example.incidents.dto.profile.ProfileUpdateResponse;
import com.example.incidents.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Accessible à tout utilisateur authentifié (règle d'URL /api/** : authenticated). */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserResponse getProfile() {
        return profileService.getProfile();
    }

    @PutMapping
    public ProfileUpdateResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(request);
    }

    @PutMapping("/password")
    public AuthResponse changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        return profileService.changePassword(request);
    }
}
