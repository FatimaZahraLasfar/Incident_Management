package com.example.incidents.dto.admin;

import java.util.Map;

public record StatisticsResponse(
        long totalUsers,
        long employees,
        long technicians,
        long admins,
        long totalIncidents,
        long open,
        long assigned,
        long inProgress,
        long resolved,
        long closed,
        Map<String, Long> byCategory,
        Map<String, Long> byPriority) {
}
