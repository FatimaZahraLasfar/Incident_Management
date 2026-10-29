package com.example.incidents.service;

import com.example.incidents.config.JwtProperties;
import com.example.incidents.entity.RefreshToken;
import com.example.incidents.entity.User;
import com.example.incidents.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Refresh tokens opaques (aléatoires, 384 bits). Seul le hash SHA-256 est stocké en base :
 * une fuite de la table ne permet pas de réutiliser les tokens.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Crée un refresh token pour l'utilisateur et retourne sa valeur en clair (communiquée une seule fois). */
    @Transactional
    public String issue(User user) {
        repository.deleteExpiredByUserId(user.getId(), Instant.now());
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS);
        repository.save(new RefreshToken(hash(raw), user, expiresAt));
        return raw;
    }

    public Optional<RefreshToken> findByRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTokenHashWithUser(hash(raw));
    }

    @Transactional
    public void revoke(String raw) {
        findByRaw(raw).ifPresent(token -> token.setRevoked(true));
    }

    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllByUserId(userId);
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
