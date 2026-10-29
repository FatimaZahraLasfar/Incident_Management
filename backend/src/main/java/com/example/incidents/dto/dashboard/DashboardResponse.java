package com.example.incidents.dto.dashboard;

import com.example.incidents.dto.incident.IncidentSummaryResponse;
import com.example.incidents.enums.Role;

import java.util.List;
import java.util.Map;

/**
 * Tableau de bord adapté au rôle. Clés de "stats" :
 * EMPLOYEE   : total, open, assigned, inProgress, resolvedOrClosed
 * TECHNICIAN : assigned, critical (CRITICAL non résolus), inProgress, resolved (RESOLVED + CLOSED)
 * ADMIN      : totalUsers, employees, technicians, totalIncidents, open, inProgress, closed
 */
public record DashboardResponse(Role role, Map<String, Long> stats, List<IncidentSummaryResponse> recentIncidents) {
}
