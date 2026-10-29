package com.example.incidents;

import com.example.incidents.entity.User;
import com.example.incidents.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private com.example.incidents.security.JwtService jwtService;

    private Map<String, Object> registration(String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "Ahmed Benali");
        body.put("email", email);
        body.put("password", PASSWORD);
        return body;
    }

    @Test
    void register_assignsEmployeeRoleAndHashesPassword() throws Exception {
        Map<String, Object> body = registration("ahmed@example.com");
        body.put("role", "ADMIN"); // tentative d'élévation de privilège : doit être ignorée

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.email").value("ahmed@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmail("ahmed@example.com").orElseThrow();
        assertEquals(Role.EMPLOYEE, saved.getRole());
        assertTrue(saved.isEnabled());
        assertNotEquals(PASSWORD, saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2"), "le mot de passe doit être un hash BCrypt");
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
    }

    @Test
    void case8_registerWithExistingEmail_returns409() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registration("ahmed@example.com"))))
                .andExpect(status().isCreated());

        // même email avec une casse différente
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registration("Ahmed@Example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email déjà utilisé"));
    }

    @Test
    void register_withInvalidData_returns400WithFieldErrors() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "");
        body.put("email", "pas-un-email");
        body.put("password", "court");

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Données invalides"))
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_returnsTokenRoleAndFullName_andTokenContainsRequiredClaims() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);

        JsonNode login = loginAs("ahmed@example.com");

        assertEquals("EMPLOYEE", login.get("role").asText());
        assertEquals("Ahmed Benali", login.get("fullName").asText());
        assertFalse(login.get("refreshToken").asText().isBlank());

        Claims claims = jwtService.parse(login.get("token").asText());
        assertEquals("ahmed@example.com", claims.getSubject());
        assertEquals("EMPLOYEE", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void login_isCaseInsensitiveOnEmail() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "AHMED@Example.com", "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void login_withWrongPasswordOrUnknownEmail_returns401WithSameMessage() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ahmed@example.com", "password", "faux-mot-de-passe"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "inconnu@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    void refresh_rotatesTokens_andDetectsReuse() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        String refresh1 = loginAs("ahmed@example.com").get("refreshToken").asText();

        JsonNode renewed = read(mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh1))))
                .andExpect(status().isOk()));
        String refresh2 = renewed.get("refreshToken").asText();
        assertNotEquals(refresh1, refresh2);
        getAs(renewed.get("token").asText(), "/api/profile").andExpect(status().isOk());

        // Réutilisation de l'ancien token : refusée, et toute la famille de tokens est révoquée
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh1))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh2))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withUnknownToken_returns401() throws Exception {
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "inconnu"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void logout_revokesTheRefreshToken() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        String refresh = loginAs("ahmed@example.com").get("refreshToken").asText();

        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_forADisabledUser_returns401() throws Exception {
        User user = createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        String refresh = loginAs("ahmed@example.com").get("refreshToken").asText();
        user.setEnabled(false);
        userRepository.save(user);

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }
}
