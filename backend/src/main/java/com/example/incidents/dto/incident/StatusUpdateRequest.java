package com.example.incidents.dto.incident;

import com.example.incidents.enums.Status;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull(message = "Le statut est obligatoire") Status status) {
}
