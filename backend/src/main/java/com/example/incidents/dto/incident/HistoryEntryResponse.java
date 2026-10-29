package com.example.incidents.dto.incident;

import com.example.incidents.dto.UserSummary;
import com.example.incidents.enums.HistoryAction;

import java.time.Instant;

public record HistoryEntryResponse(
        Long id,
        HistoryAction action,
        String oldValue,
        String newValue,
        String message,
        UserSummary performedBy,
        Instant createdAt) {
}
