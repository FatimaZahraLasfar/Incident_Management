package com.example.incidents.service;

import com.example.incidents.dto.UserResponse;
import com.example.incidents.dto.auth.AuthResponse;
import com.example.incidents.dto.auth.LoginRequest;
import com.example.incidents.dto.auth.RegisterRequest;
import com.example.incidents.entity.RefreshToken;
import com.example.incidents.entity.User;
import com.example.incidents.enums.Role;
import com.example.incidents.exception.ConflictException;
import com.example.incidents.exception.ForbiddenException;
import com.example.incidents.exception.UnauthorizedException;
import com.example.incidents.mapper.EntityMapper;
import com.example.incidents.repository.UserRepository;
import com.example.incidents.security.JwtService;
import com.example.incidents.util.EmailUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    /** Hash factice comparé quand l'email est inconnu, pour que le temps de réponse ne révèle pas l'existence d'un compte. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-equalization");
    }

    /** Le rôle est toujours EMPLOYEE : il ne peut pas être choisi par l'utilisateur. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = EmailUtils.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email déjà utilisé");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.EMPLOYEE);
        user.setEnabled(true);
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email déjà utilisé");
        }
        return EntityMapper.toUserResponse(user);
    }

    /**
     * Identifiants incorrects (email inconnu ou mot de passe faux) : 401 avec un message identique.
     * Compte désactivé : 403, révélé seulement si le mot de passe est correct.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(EmailUtils.normalize(request.email())).orElse(null);
        boolean passwordMatches = passwordEncoder.matches(request.password(),
                user != null ? user.getPassword() : dummyHash);
        if (user == null || !passwordMatches) {
            throw new UnauthorizedException("Email ou mot de passe incorrect");
        }
        if (!user.isEnabled()) {
            throw new ForbiddenException("Compte désactivé. Contactez un administrateur.");
        }
        return issueTokens(user);
    }

    /**
     * Rotation : chaque refresh token n'est utilisable qu'une fois. Si un token déjà utilisé est présenté
     * (vol possible), tous les refresh tokens de l'utilisateur sont révoqués.
     * noRollbackFor : la révocation doit être enregistrée même si on répond 401.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenService.findByRaw(rawRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalide"));
        User user = stored.getUser();
        if (stored.isRevoked()) {
            refreshTokenService.revokeAll(user.getId());
            throw new UnauthorizedException("Refresh token invalide ou déjà utilisé");
        }
        if (stored.isExpired()) {
            throw new UnauthorizedException("Session expirée, veuillez vous reconnecter");
        }
        if (!user.isEnabled()) {
            refreshTokenService.revokeAll(user.getId());
            throw new UnauthorizedException("Compte désactivé");
        }
        stored.setRevoked(true);
        return issueTokens(user);
    }

    /** Déconnexion : révoque le refresh token (idempotent). L'access token expire naturellement. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    /** Génère un access token JWT et un nouveau refresh token pour l'utilisateur. */
    @Transactional
    public AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds(),
                user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
