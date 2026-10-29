package com.example.incidents.controller;

import com.example.incidents.dto.UserResponse;
import com.example.incidents.dto.admin.RoleUpdateRequest;
import com.example.incidents.dto.admin.StatisticsResponse;
import com.example.incidents.service.StatisticsService;
import com.example.incidents.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Réservé aux ADMIN : règle d'URL dans SecurityConfig + @PreAuthorize (défense en profondeur). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserAdminService userAdminService;
    private final StatisticsService statisticsService;

    public AdminController(UserAdminService userAdminService, StatisticsService statisticsService) {
        this.userAdminService = userAdminService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userAdminService.listUsers();
    }

    @PutMapping("/users/{id}/enable")
    public UserResponse enable(@PathVariable Long id) {
        return userAdminService.setEnabled(id, true);
    }

    @PutMapping("/users/{id}/disable")
    public UserResponse disable(@PathVariable Long id) {
        return userAdminService.setEnabled(id, false);
    }

    @PutMapping("/users/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return userAdminService.changeRole(id, request.role());
    }

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }
}
