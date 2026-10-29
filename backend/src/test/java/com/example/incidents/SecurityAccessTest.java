package com.example.incidents;

import com.example.incidents.entity.User;
import com.example.incidents.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Les 9 tests de sécurité du cahier des charges (section 25) : cas 1 à 7 ici, 8 dans AuthIntegrationTest, 9 dans IncidentRulesTest. */
class SecurityAccessTest extends AbstractIntegrationTest {

    @Test
    void case1_protectedApiWithoutJwt_returns401() throws Exception {
        mvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentification requise"));
    }

    @Test
    void case2_invalidJwt_returns401() throws Exception {
        mvc.perform(get("/api/profile").header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token invalide"));

        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        String forged = buildToken("ahmed@example.com", "EMPLOYEE", Instant.now(),
                Instant.now().plusSeconds(300), "another-secret-another-secret-0123456789");
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token invalide"));
    }

    @Test
    void case3_expiredJwt_returns401() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        String expired = buildToken("ahmed@example.com", "EMPLOYEE", Instant.now().minusSeconds(600),
                Instant.now().minusSeconds(300), jwtProperties.getSecret());
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Token expiré"));
    }

    @Test
    void case4_employeeOnAdminRoute_returns403() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        String token = tokenFor("ahmed@example.com");

        getAs(token, "/api/admin/users").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        getAs(token, "/api/admin/statistics").andExpect(status().isForbidden());
        putAs(token, "/api/admin/users/{id}/disable", 1L).andExpect(status().isForbidden());
    }

    @Test
    void case4b_technicianOnAdminRoute_returns403() throws Exception {
        createUser("Karim Alaoui", "tech@example.com", Role.TECHNICIAN);
        getAs(tokenFor("tech@example.com"), "/api/admin/users").andExpect(status().isForbidden());
    }

    @Test
    void case5_employeeReadingAnotherEmployeesIncident_returns403() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        createUser("Salma Zahidi", "salma@example.com", Role.EMPLOYEE);
        String ahmed = tokenFor("ahmed@example.com");
        String salma = tokenFor("salma@example.com");

        long incidentId = createIncident(salma, "Phishing reçu");

        getAs(ahmed, "/api/incidents/{id}", incidentId).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        getAs(salma, "/api/incidents/{id}", incidentId).andExpect(status().isOk());
        getAs(ahmed, "/api/incidents/my").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void case6_technicianModifyingUnassignedIncident_returns403() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        createUser("Admin", "admin@example.com", Role.ADMIN);
        User tech1 = createUser("Karim Alaoui", "tech1@example.com", Role.TECHNICIAN);
        createUser("Sara Idrissi", "tech2@example.com", Role.TECHNICIAN);

        long incidentId = createIncident(tokenFor("ahmed@example.com"), "Malware");
        String tech2 = tokenFor("tech2@example.com");

        // Incident non affecté du tout
        setStatus(tech2, incidentId, "IN_PROGRESS").andExpect(status().isForbidden());

        // Incident affecté à un autre technicien
        assign(tokenFor("admin@example.com"), incidentId, tech1.getId()).andExpect(status().isOk());
        setStatus(tech2, incidentId, "IN_PROGRESS").andExpect(status().isForbidden());
        postAs(tech2, "/api/incidents/{id}/comments", Map.of("content", "Note"), incidentId)
                .andExpect(status().isForbidden());
        getAs(tech2, "/api/incidents/{id}", incidentId).andExpect(status().isForbidden());

        // Le technicien affecté, lui, peut agir
        setStatus(tokenFor("tech1@example.com"), incidentId, "IN_PROGRESS").andExpect(status().isOk());
    }

    @Test
    void case7_disabledUserCannotLogin_returns403() throws Exception {
        User user = createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        user.setEnabled(false);
        userRepository.save(user);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ahmed@example.com", "password", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Compte désactivé. Contactez un administrateur."));

        // Avec un mauvais mot de passe on ne révèle pas que le compte existe ni qu'il est désactivé
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ahmed@example.com", "password", "mauvais-mot-de-passe"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disablingAnAccountInvalidatesItsExistingToken() throws Exception {
        User employee = createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        createUser("Admin", "admin@example.com", Role.ADMIN);
        String employeeToken = tokenFor("ahmed@example.com");
        getAs(employeeToken, "/api/profile").andExpect(status().isOk());

        putAs(tokenFor("admin@example.com"), "/api/admin/users/{id}/disable", employee.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        getAs(employeeToken, "/api/profile").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Compte désactivé"));
    }

    @Test
    void roleClaimInTheTokenIsNotTrusted_theDatabaseRoleApplies() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        String tokenClaimingAdmin = buildToken("ahmed@example.com", "ADMIN", Instant.now(),
                Instant.now().plusSeconds(300), jwtProperties.getSecret());

        getAs(tokenClaimingAdmin, "/api/admin/users").andExpect(status().isForbidden());
    }

    @Test
    void unknownIncident_returns404() throws Exception {
        createUser("Admin", "admin@example.com", Role.ADMIN);
        getAs(tokenFor("admin@example.com"), "/api/incidents/{id}", 99999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Incident introuvable"));
    }

    @Test
    void publicAuthRoutesNeedNoToken() throws Exception {
        createUser("Ahmed Benali", "ahmed@example.com", Role.EMPLOYEE);
        JsonNode login = loginAs("ahmed@example.com");
        org.junit.jupiter.api.Assertions.assertFalse(login.get("token").asText().isBlank());
    }
}
