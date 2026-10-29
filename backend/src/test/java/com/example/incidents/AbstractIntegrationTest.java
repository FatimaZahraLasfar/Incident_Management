package com.example.incidents;

import com.example.incidents.config.JwtProperties;
import com.example.incidents.entity.User;
import com.example.incidents.enums.Role;
import com.example.incidents.repository.IncidentCommentRepository;
import com.example.incidents.repository.IncidentHistoryRepository;
import com.example.incidents.repository.IncidentRepository;
import com.example.incidents.repository.RefreshTokenRepository;
import com.example.incidents.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base des tests d'intégration : contexte Spring complet, base H2 en mémoire vidée avant chaque test. */
@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.jwt.secret=test-secret-for-junit-tests-0123456789-abcdef",
        "app.jwt.access-token-minutes=5"
})
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Password@123";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected IncidentRepository incidentRepository;
    @Autowired protected IncidentHistoryRepository historyRepository;
    @Autowired protected IncidentCommentRepository commentRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtProperties jwtProperties;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        historyRepository.deleteAll();
        commentRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ------------------------------------------------------------------ données

    protected User createUser(String fullName, String email, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    protected Map<String, Object> validIncident(String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "Description de l'incident " + title);
        body.put("category", "MALWARE");
        body.put("priority", "HIGH");
        return body;
    }

    // ------------------------------------------------------------------ requêtes

    protected String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected JsonNode loginAs(String email) throws Exception {
        return read(mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()));
    }

    protected String tokenFor(String email) throws Exception {
        return loginAs(email).get("token").asText();
    }

    protected ResultActions getAs(String token, String url, Object... vars) throws Exception {
        return mvc.perform(get(url, vars).header("Authorization", "Bearer " + token));
    }

    protected ResultActions postAs(String token, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(post(url, vars)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    protected ResultActions putJson(String token, String url, Object body, Object... vars) throws Exception {
        return mvc.perform(put(url, vars)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    protected ResultActions putAs(String token, String url, Object... vars) throws Exception {
        return mvc.perform(put(url, vars).header("Authorization", "Bearer " + token));
    }

    protected long createIncident(String token, String title) throws Exception {
        JsonNode created = read(postAs(token, "/api/incidents", validIncident(title)).andExpect(status().isCreated()));
        return created.get("id").asLong();
    }

    protected ResultActions assign(String adminToken, long incidentId, long technicianId) throws Exception {
        return putAs(adminToken, "/api/incidents/{id}/assign/{tid}", incidentId, technicianId);
    }

    protected ResultActions setStatus(String token, long incidentId, String status) throws Exception {
        return putJson(token, "/api/incidents/{id}/status", Map.of("status", status), incidentId);
    }

    /** Construit un JWT signé avec la clé de test (ou une autre) et des dates choisies, pour les cas limites. */
    protected String buildToken(String email, String role, Instant issuedAt, Instant expiresAt, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
