package com.example.incidents.service;

import com.example.incidents.dto.UserResponse;
import com.example.incidents.entity.User;
import com.example.incidents.enums.Role;
import com.example.incidents.exception.BadRequestException;
import com.example.incidents.exception.ResourceNotFoundException;
import com.example.incidents.mapper.EntityMapper;
import com.example.incidents.repository.UserRepository;
import com.example.incidents.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final RefreshTokenService refreshTokenService;

    public UserAdminService(UserRepository userRepository, CurrentUserService currentUserService,
                            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByIdAsc().stream().map(EntityMapper::toUserResponse).toList();
    }

    /** Un compte désactivé ne peut plus se connecter et ses sessions de rafraîchissement sont révoquées. */
    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User target = findUser(userId);
        if (!enabled && target.getId().equals(currentUserService.getPrincipal().getId())) {
            throw new BadRequestException("Vous ne pouvez pas désactiver votre propre compte");
        }
        target.setEnabled(enabled);
        if (!enabled) {
            refreshTokenService.revokeAll(target.getId());
        }
        return EntityMapper.toUserResponse(target);
    }

    /** Seul moyen de créer un TECHNICIAN ou un ADMIN à partir d'un compte existant. */
    @Transactional
    public UserResponse changeRole(Long userId, Role role) {
        User target = findUser(userId);
        if (target.getId().equals(currentUserService.getPrincipal().getId())) {
            throw new BadRequestException("Vous ne pouvez pas modifier votre propre rôle");
        }
        target.setRole(role);
        return EntityMapper.toUserResponse(target);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
