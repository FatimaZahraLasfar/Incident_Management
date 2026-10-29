package com.example.incidents.service;

import com.example.incidents.dto.UserResponse;
import com.example.incidents.dto.auth.AuthResponse;
import com.example.incidents.dto.profile.PasswordChangeRequest;
import com.example.incidents.dto.profile.ProfileUpdateRequest;
import com.example.incidents.dto.profile.ProfileUpdateResponse;
import com.example.incidents.entity.User;
import com.example.incidents.exception.BadRequestException;
import com.example.incidents.exception.ConflictException;
import com.example.incidents.mapper.EntityMapper;
import com.example.incidents.repository.UserRepository;
import com.example.incidents.security.CurrentUserService;
import com.example.incidents.security.JwtService;
import com.example.incidents.util.EmailUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public ProfileService(CurrentUserService currentUserService, UserRepository userRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService,
                          AuthService authService, RefreshTokenService refreshTokenService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile() {
        return EntityMapper.toUserResponse(currentUserService.getCurrentUser());
    }

    /**
     * Le sujet du JWT est l'email : s'il change, l'ancien token ne désigne plus personne.
     * On renvoie donc un nouvel access token à jour.
     */
    @Transactional
    public ProfileUpdateResponse updateProfile(ProfileUpdateRequest request) {
        User me = currentUserService.getCurrentUser();
        String email = EmailUtils.normalize(request.email());
        if (userRepository.existsByEmailAndIdNot(email, me.getId())) {
            throw new ConflictException("Email déjà utilisé");
        }
        me.setFullName(request.fullName().trim());
        me.setEmail(email);
        try {
            me = userRepository.saveAndFlush(me);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email déjà utilisé");
        }
        return new ProfileUpdateResponse(EntityMapper.toUserResponse(me), jwtService.generateAccessToken(me));
    }

    /**
     * Vérifie l'ancien mot de passe (400 s'il est faux : un 401 déconnecterait l'utilisateur côté React),
     * révoque toutes les sessions (refresh tokens) puis ouvre une nouvelle session.
     */
    @Transactional
    public AuthResponse changePassword(PasswordChangeRequest request) {
        User me = currentUserService.getCurrentUser();
        if (!passwordEncoder.matches(request.oldPassword(), me.getPassword())) {
            throw new BadRequestException("Ancien mot de passe incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("La confirmation ne correspond pas au nouveau mot de passe");
        }
        if (passwordEncoder.matches(request.newPassword(), me.getPassword())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        me.setPassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(me.getId());
        return authService.issueTokens(me);
    }
}
