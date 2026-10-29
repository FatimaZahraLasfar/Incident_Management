package com.example.incidents;

import com.example.incidents.entity.User;
import com.example.incidents.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Règles métier, parcours complet du cahier des charges (section 24), profil, administration, tableaux de bord. */
class IncidentRulesTest extends AbstractIntegrationTest {

    private User employee1;
    private User employee2;
    private User tech1;
    private User tech2;
    private User admin;
    private String e1;
    private String e2;
    private String t1;
    private String t2;
    private String adm;

    @BeforeEach
    void setUpUsers() throws Exception {
        employee1 = createUser("Ahmed Benali", "employee1@example.com", Role.EMPLOYEE);
        employee2 = createUser("Salma Zahidi", "employee2@example.com", Role.EMPLOYEE);
        tech1 = createUser("Karim Alaoui", "tech1@example.com", Role.TECHNICIAN);
        tech2 = createUser("Sara Idrissi", "tech2@example.com", Role.TECHNICIAN);
        admin = createUser("Admin Système", "admin@example.com", Role.ADMIN);
        e1 = tokenFor("employee1@example.com");
        e2 = tokenFor("employee2@example.com");
        t1 = tokenFor("tech1@example.com");
        t2 = tokenFor("tech2@example.com");
        adm = tokenFor("admin@example.com");
    }

    // ------------------------------------------------------------------ scénario du cahier des charges

    @Test
    void fullScenario() throws Exception {
        long i1 = createIncident(e1, "Ordinateur infecté par un malware");
        long i2 = createIncident(e1, "Tentative de phishing");
        long i3 = createIncident(e2, "Panne réseau");

        // 8. le premier employé ne voit pas l'incident du second
        getAs(e1, "/api/incidents/{id}", i3).andExpect(status().isForbidden());
        getAs(e1, "/api/incidents/my").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        getAs(e2, "/api/incidents/my").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        // 10-11. l'admin voit tout et affecte
        getAs(adm, "/api/incidents").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        assign(adm, i1, tech1.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo.fullName").value("Karim Alaoui"));

        // 13. le technicien voit uniquement ses incidents
        getAs(t1, "/api/incidents/assigned").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) i1));
        getAs(t2, "/api/incidents/assigned").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        // 14. changements de statut puis 15. note de traitement
        setStatus(t1, i1, "IN_PROGRESS").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        setStatus(t1, i1, "RESOLVED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        postAs(t1, "/api/incidents/{id}/comments", Map.of("content", "Le malware a été supprimé."), i1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Le malware a été supprimé."))
                .andExpect(jsonPath("$.author.fullName").value("Karim Alaoui"));

        // 16-17. l'employé retrouve l'état et l'historique (mais pas les notes internes)
        JsonNode asEmployee = read(getAs(e1, "/api/incidents/{id}", i1).andExpect(status().isOk()));
        assertEquals("RESOLVED", asEmployee.get("status").asText());
        assertEquals(0, asEmployee.get("comments").size());
        JsonNode history = asEmployee.get("history");
        assertEquals(5, history.size());
        assertEquals("CREATED", history.get(0).get("action").asText());
        assertEquals("Incident créé", history.get(0).get("message").asText());
        assertEquals("ASSIGNED", history.get(1).get("action").asText());
        assertEquals("Affecté à Karim Alaoui", history.get(1).get("message").asText());
        assertEquals("OPEN -> ASSIGNED", history.get(2).get("message").asText());
        assertEquals("ASSIGNED -> IN_PROGRESS", history.get(3).get("message").asText());
        assertEquals("IN_PROGRESS -> RESOLVED", history.get(4).get("message").asText());

        // l'admin et le technicien voient la note
        JsonNode asAdmin = read(getAs(adm, "/api/incidents/{id}", i1).andExpect(status().isOk()));
        assertEquals(1, asAdmin.get("comments").size());
        JsonNode asTech = read(getAs(t1, "/api/incidents/{id}", i1).andExpect(status().isOk()));
        assertEquals(1, asTech.get("comments").size());

        // l'incident non traité reste ouvert
        getAs(e1, "/api/incidents/{id}", i2).andExpect(jsonPath("$.status").value("OPEN"));
    }

    // ------------------------------------------------------------------ statuts et affectation

    @Test
    void onlyAdminCanAssign_andOnlyTechniciansOrAdminChangeStatus() throws Exception {
        long id = createIncident(e1, "Incident");

        assign(e1, id, tech1.getId()).andExpect(status().isForbidden());
        assign(t1, id, tech1.getId()).andExpect(status().isForbidden());
        setStatus(e1, id, "IN_PROGRESS").andExpect(status().isForbidden());
        postAs(e1, "/api/incidents/{id}/comments", Map.of("content", "Note"), id).andExpect(status().isForbidden());
    }

    @Test
    void assignmentValidation() throws Exception {
        long id = createIncident(e1, "Incident");

        assign(adm, id, 99999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable"));
        assign(adm, id, employee2.getId()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("L'utilisateur sélectionné n'est pas un technicien"));
        assign(adm, 99999L, tech1.getId()).andExpect(status().isNotFound());

        tech2.setEnabled(false);
        userRepository.save(tech2);
        assign(adm, id, tech2.getId()).andExpect(status().isBadRequest());

        assign(adm, id, tech1.getId()).andExpect(status().isOk());
        assign(adm, id, tech1.getId()).andExpect(status().isBadRequest());
    }

    @Test
    void reassignment_keepsStatusAndRevokesTheFirstTechniciansAccess() throws Exception {
        long id = createIncident(e1, "Incident");
        assign(adm, id, tech1.getId()).andExpect(status().isOk());
        assign(adm, id, tech2.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo.fullName").value("Sara Idrissi"));

        JsonNode history = read(getAs(adm, "/api/incidents/{id}", id).andExpect(status().isOk())).get("history");
        assertEquals("Réaffecté de Karim Alaoui à Sara Idrissi", history.get(history.size() - 1).get("message").asText());

        setStatus(t1, id, "IN_PROGRESS").andExpect(status().isForbidden());
        setStatus(t2, id, "IN_PROGRESS").andExpect(status().isOk());
    }

    @Test
    void invalidTransitions_areRejected() throws Exception {
        long id = createIncident(e1, "Incident");

        // pas de technicien : on ne peut pas démarrer le traitement
        setStatus(adm, id, "IN_PROGRESS").andExpect(status().isBadRequest());

        assign(adm, id, tech1.getId()).andExpect(status().isOk());
        setStatus(t1, id, "RESOLVED").andExpect(status().isBadRequest());   // ASSIGNED -> RESOLVED
        setStatus(t1, id, "OPEN").andExpect(status().isBadRequest());       // retour en arrière
        setStatus(t1, id, "ASSIGNED").andExpect(status().isBadRequest());   // il faut passer par l'affectation
        setStatus(t1, id, "IN_PROGRESS").andExpect(status().isOk());
        setStatus(t1, id, "IN_PROGRESS").andExpect(status().isBadRequest()); // même statut

        mvcPutRawStatus(t1, id, "N_IMPORTE_QUOI").andExpect(status().isBadRequest());
        mvcPutRawStatus(t1, id, null).andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions mvcPutRawStatus(String token, long id, String status)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        return putJson(token, "/api/incidents/{id}/status", body, id);
    }

    @Test
    void closedIncident_isNotModifiableNormally() throws Exception {
        long id = createIncident(e1, "Incident");
        assign(adm, id, tech1.getId()).andExpect(status().isOk());
        setStatus(t1, id, "IN_PROGRESS").andExpect(status().isOk());
        setStatus(t1, id, "RESOLVED").andExpect(status().isOk());

        // le technicien ne peut pas clôturer
        setStatus(t1, id, "CLOSED").andExpect(status().isForbidden());
        setStatus(adm, id, "CLOSED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));

        // R8 : plus de modification normale
        setStatus(t1, id, "IN_PROGRESS").andExpect(status().isForbidden());
        postAs(t1, "/api/incidents/{id}/comments", Map.of("content", "Trop tard"), id)
                .andExpect(status().isForbidden());
        assign(adm, id, tech2.getId()).andExpect(status().isBadRequest());

        // l'employé peut toujours consulter son incident clôturé
        getAs(e1, "/api/incidents/{id}", id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));

        // seul l'admin peut rouvrir, et la réouverture est historisée
        setStatus(adm, id, "IN_PROGRESS").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        JsonNode history = read(getAs(adm, "/api/incidents/{id}", id).andExpect(status().isOk())).get("history");
        String last = history.get(history.size() - 1).get("message").asText();
        assertTrue(last.startsWith("Incident rouvert"), last);
    }

    // ------------------------------------------------------------------ création et validation (cas 9)

    @Test
    void case9_createIncidentWithInvalidData_returns400() throws Exception {
        Map<String, Object> noTitle = validIncident("x");
        noTitle.put("title", "  ");
        postAs(e1, "/api/incidents", noTitle).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.title").exists());

        Map<String, Object> noDescription = validIncident("x");
        noDescription.remove("description");
        postAs(e1, "/api/incidents", noDescription).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists());

        Map<String, Object> noPriority = validIncident("x");
        noPriority.remove("priority");
        postAs(e1, "/api/incidents", noPriority).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priority").exists());

        Map<String, Object> badCategory = validIncident("x");
        badCategory.put("category", "N_IMPORTE_QUOI");
        postAs(e1, "/api/incidents", badCategory).andExpect(status().isBadRequest());

        Map<String, Object> tooLong = validIncident("x");
        tooLong.put("title", "a".repeat(151));
        postAs(e1, "/api/incidents", tooLong).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        mvc.perform(post("/api/incidents").header("Authorization", "Bearer " + e1)
                        .contentType(MediaType.APPLICATION_JSON).content("{ pas du json"))
                .andExpect(status().isBadRequest());

        assertEquals(0, incidentRepository.count());
    }

    @Test
    void onlyEmployeesCanCreateIncidents() throws Exception {
        postAs(adm, "/api/incidents", validIncident("x")).andExpect(status().isForbidden());
        postAs(t1, "/api/incidents", validIncident("x")).andExpect(status().isForbidden());
        assertEquals(0, incidentRepository.count());
    }

    @Test
    void createdIncident_startsOpenAndBelongsToTheCaller() throws Exception {
        postAs(e1, "/api/incidents", validIncident("Poste suspect")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdBy.fullName").value("Ahmed Benali"))
                .andExpect(jsonPath("$.assignedTo").doesNotExist())
                .andExpect(jsonPath("$.createdBy.email").doesNotExist());
    }

    @Test
    void listEndpointsAreRestrictedByRole() throws Exception {
        getAs(e1, "/api/incidents").andExpect(status().isForbidden());
        getAs(e1, "/api/incidents/assigned").andExpect(status().isForbidden());
        getAs(t1, "/api/incidents/my").andExpect(status().isForbidden());
        getAs(t1, "/api/incidents").andExpect(status().isForbidden());
        getAs(adm, "/api/incidents/my").andExpect(status().isForbidden());
    }

    @Test
    void commentValidation() throws Exception {
        long id = createIncident(e1, "Incident");
        assign(adm, id, tech1.getId()).andExpect(status().isOk());
        postAs(t1, "/api/incidents/{id}/comments", Map.of("content", "  "), id).andExpect(status().isBadRequest());
        postAs(t1, "/api/incidents/{id}/comments", Map.of("content", "a".repeat(2001)), id)
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericIncidentId_returns400() throws Exception {
        getAs(adm, "/api/incidents/{id}", "abc").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ profil

    @Test
    void profile_canBeReadAndUpdated_andANewTokenFollowsAnEmailChange() throws Exception {
        getAs(e1, "/api/profile").andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("employee1@example.com"))
                .andExpect(jsonPath("$.fullName").value("Ahmed Benali"))
                .andExpect(jsonPath("$.password").doesNotExist());

        JsonNode updated = read(putJson(e1, "/api/profile",
                Map.of("fullName", "Ahmed B.", "email", "ahmed.new@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("ahmed.new@example.com")));
        String newToken = updated.get("token").asText();

        getAs(newToken, "/api/profile").andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Ahmed B."));
        // l'ancien token désigne l'ancien email, qui n'existe plus
        getAs(e1, "/api/profile").andExpect(status().isUnauthorized());
    }

    @Test
    void profileUpdate_rejectsAnEmailAlreadyUsedAndInvalidData() throws Exception {
        putJson(e1, "/api/profile", Map.of("fullName", "Ahmed", "email", "employee2@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email déjà utilisé"));
        putJson(e1, "/api/profile", Map.of("fullName", "Ahmed", "email", "pas-un-email"))
                .andExpect(status().isBadRequest());
        // garder son propre email est autorisé
        putJson(e1, "/api/profile", Map.of("fullName", "Ahmed Nouveau", "email", "employee1@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void passwordChange_verifiesTheOldPassword_andRevokesOldSessions() throws Exception {
        String oldRefresh = loginAs("employee1@example.com").get("refreshToken").asText();

        // un mauvais ancien mot de passe donne 400 (et non 401, qui déconnecterait l'utilisateur côté React)
        putJson(e1, "/api/profile/password",
                Map.of("oldPassword", "faux", "newPassword", "NouveauPass@1", "confirmPassword", "NouveauPass@1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ancien mot de passe incorrect"));
        putJson(e1, "/api/profile/password",
                Map.of("oldPassword", PASSWORD, "newPassword", "NouveauPass@1", "confirmPassword", "Autre@12345"))
                .andExpect(status().isBadRequest());
        putJson(e1, "/api/profile/password",
                Map.of("oldPassword", PASSWORD, "newPassword", "court", "confirmPassword", "court"))
                .andExpect(status().isBadRequest());
        putJson(e1, "/api/profile/password",
                Map.of("oldPassword", PASSWORD, "newPassword", PASSWORD, "confirmPassword", PASSWORD))
                .andExpect(status().isBadRequest());

        JsonNode renewed = read(putJson(e1, "/api/profile/password",
                Map.of("oldPassword", PASSWORD, "newPassword", "NouveauPass@1", "confirmPassword", "NouveauPass@1"))
                .andExpect(status().isOk()));
        assertFalse(renewed.get("token").asText().isBlank());
        assertNotEquals(oldRefresh, renewed.get("refreshToken").asText());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "employee1@example.com", "password", "NouveauPass@1"))))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "employee1@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ administration

    @Test
    void admin_listsUsersWithoutPasswords_andManagesAccounts() throws Exception {
        getAs(adm, "/api/admin/users").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        putAs(adm, "/api/admin/users/{id}/disable", employee2.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        putAs(adm, "/api/admin/users/{id}/enable", employee2.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
        putAs(adm, "/api/admin/users/{id}/disable", 99999L).andExpect(status().isNotFound());
        putAs(adm, "/api/admin/users/{id}/disable", admin.getId()).andExpect(status().isBadRequest());
    }

    @Test
    void admin_canPromoteAnEmployeeToTechnician() throws Exception {
        putJson(adm, "/api/admin/users/{id}/role", Map.of("role", "TECHNICIAN"), employee2.getId())
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("TECHNICIAN"));

        String promoted = tokenFor("employee2@example.com");
        getAs(promoted, "/api/incidents/assigned").andExpect(status().isOk());
        getAs(promoted, "/api/incidents/my").andExpect(status().isForbidden());

        putJson(adm, "/api/admin/users/{id}/role", Map.of("role", "EMPLOYEE"), admin.getId())
                .andExpect(status().isBadRequest());
        putJson(adm, "/api/admin/users/{id}/role", Map.of("role", "SUPERUSER"), employee1.getId())
                .andExpect(status().isBadRequest());
    }

    @Test
    void statistics_reflectTheData() throws Exception {
        long i1 = createIncident(e1, "A");
        createIncident(e1, "B");
        createIncident(e2, "C");
        assign(adm, i1, tech1.getId()).andExpect(status().isOk());

        getAs(adm, "/api/admin/statistics").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.employees").value(2))
                .andExpect(jsonPath("$.technicians").value(2))
                .andExpect(jsonPath("$.admins").value(1))
                .andExpect(jsonPath("$.totalIncidents").value(3))
                .andExpect(jsonPath("$.open").value(2))
                .andExpect(jsonPath("$.assigned").value(1))
                .andExpect(jsonPath("$.closed").value(0))
                .andExpect(jsonPath("$.byCategory.MALWARE").value(3))
                .andExpect(jsonPath("$.byCategory.PHISHING").value(0))
                .andExpect(jsonPath("$.byPriority.HIGH").value(3));
    }

    // ------------------------------------------------------------------ tableaux de bord

    @Test
    void employeeDashboard() throws Exception {
        long i1 = createIncident(e1, "A");
        createIncident(e1, "B");
        assign(adm, i1, tech1.getId()).andExpect(status().isOk());
        setStatus(t1, i1, "IN_PROGRESS").andExpect(status().isOk());
        setStatus(t1, i1, "RESOLVED").andExpect(status().isOk());

        getAs(e1, "/api/dashboard").andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.stats.total").value(2))
                .andExpect(jsonPath("$.stats.open").value(1))
                .andExpect(jsonPath("$.stats.inProgress").value(0))
                .andExpect(jsonPath("$.stats.resolvedOrClosed").value(1))
                .andExpect(jsonPath("$.recentIncidents.length()").value(2));
        getAs(e2, "/api/dashboard").andExpect(jsonPath("$.stats.total").value(0))
                .andExpect(jsonPath("$.recentIncidents.length()").value(0));
    }

    @Test
    void technicianDashboard() throws Exception {
        long normal = createIncident(e1, "Normal");
        Map<String, Object> critical = validIncident("Fuite de données");
        critical.put("priority", "CRITICAL");
        critical.put("category", "DATA_LEAK");
        long criticalId = read(postAs(e1, "/api/incidents", critical).andExpect(status().isCreated())).get("id").asLong();
        assign(adm, normal, tech1.getId()).andExpect(status().isOk());
        assign(adm, criticalId, tech1.getId()).andExpect(status().isOk());
        setStatus(t1, normal, "IN_PROGRESS").andExpect(status().isOk());

        getAs(t1, "/api/dashboard").andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.stats.assigned").value(2))
                .andExpect(jsonPath("$.stats.critical").value(1))
                .andExpect(jsonPath("$.stats.inProgress").value(1))
                .andExpect(jsonPath("$.stats.resolved").value(0))
                .andExpect(jsonPath("$.recentIncidents.length()").value(2));

        setStatus(t1, criticalId, "IN_PROGRESS").andExpect(status().isOk());
        setStatus(t1, criticalId, "RESOLVED").andExpect(status().isOk());
        getAs(t1, "/api/dashboard").andExpect(jsonPath("$.stats.critical").value(0))
                .andExpect(jsonPath("$.stats.resolved").value(1));
    }

    @Test
    void adminDashboard() throws Exception {
        createIncident(e1, "A");
        long b = createIncident(e2, "B");
        assign(adm, b, tech1.getId()).andExpect(status().isOk());
        setStatus(t1, b, "IN_PROGRESS").andExpect(status().isOk());

        getAs(adm, "/api/dashboard").andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.stats.totalUsers").value(5))
                .andExpect(jsonPath("$.stats.employees").value(2))
                .andExpect(jsonPath("$.stats.technicians").value(2))
                .andExpect(jsonPath("$.stats.totalIncidents").value(2))
                .andExpect(jsonPath("$.stats.open").value(1))
                .andExpect(jsonPath("$.stats.inProgress").value(1))
                .andExpect(jsonPath("$.stats.closed").value(0))
                .andExpect(jsonPath("$.recentIncidents.length()").value(2));
    }
}
