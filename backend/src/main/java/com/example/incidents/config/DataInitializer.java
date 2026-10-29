package com.example.incidents.config;

import com.example.incidents.entity.User;
import com.example.incidents.enums.Role;
import com.example.incidents.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crée les comptes de démonstration si la table users est vide (désactivable avec SEED_ENABLED=false).
 * Les comptes TECHNICIAN et ADMIN ne peuvent pas être créés via /auth/register : ils viennent d'ici,
 * ou d'une promotion par un ADMIN (PUT /api/admin/users/{id}/role).
 * Mot de passe de démonstration commun, à ne jamais utiliser en production.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    static final String DEMO_PASSWORD = "Password@123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        create("Admin Système", "admin@example.com", Role.ADMIN);
        create("Karim Alaoui", "tech1@example.com", Role.TECHNICIAN);
        create("Sara Idrissi", "tech2@example.com", Role.TECHNICIAN);
        create("Ahmed Benali", "employee1@example.com", Role.EMPLOYEE);
        create("Salma Zahidi", "employee2@example.com", Role.EMPLOYEE);
        log.info("Comptes de démonstration créés (mot de passe commun : {})", DEMO_PASSWORD);
    }

    private void create(String fullName, String email, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }
}
