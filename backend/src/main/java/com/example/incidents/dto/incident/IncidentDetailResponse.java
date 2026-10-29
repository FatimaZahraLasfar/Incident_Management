package com.example.incidents.dto.incident;

import com.example.incidents.dto.UserSummary;
import com.example.incidents.enums.Category;
import com.example.incidents.enums.Priority;
import com.example.incidents.enums.Status;

import java.time.Instant;
import java.util.List;

/** "comments" (notes de traitement) est toujours vide pour un EMPLOYEE : notes internes. */
public record IncidentDetailResponse(
        Long id,
        String title,
        String description,
        Category category,
        Priority priority,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        UserSummary createdBy,
        UserSummary assignedTo,
        List<HistoryEntryResponse> history,
        List<CommentResponse> comments) {
}
