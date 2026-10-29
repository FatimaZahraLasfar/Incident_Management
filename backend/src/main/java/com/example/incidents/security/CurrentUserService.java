package com.example.incidents.security;

import com.example.incidents.entity.User;
import com.example.incidents.exception.UnauthorizedException;
import com.example.incidents.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Récupère l'utilisateur authentifié (SecurityContextHolder → Authentication → principal). */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUserDetails getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new UnauthorizedException("Authentification requise");
        }
        return principal;
    }

    /** À appeler depuis une méthode @Transactional pour obtenir une entité gérée. */
    public User getCurrentUser() {
        return userRepository.findById(getPrincipal().getId())
                .orElseThrow(() -> new UnauthorizedException("Authentification requise"));
    }
}
