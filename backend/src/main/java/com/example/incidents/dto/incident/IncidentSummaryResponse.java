package com.example.incidents.dto.incident;

import com.example.incidents.dto.UserSummary;
import com.example.incidents.enums.Category;
import com.example.incidents.enums.Priority;
import com.example.incidents.enums.Status;

import java.time.Instant;

public record IncidentSummaryResponse(
        Long id,
        String title,
        Category category,
        Priority priority,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        UserSummary createdBy,
        UserSummary assignedTo) {
}
