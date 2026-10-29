package com.example.incidents.dto.incident;

import com.example.incidents.enums.Category;
import com.example.incidents.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentCreateRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères")
        String title,

        @NotBlank(message = "La description est obligatoire")
        @Size(max = 4000, message = "La description ne doit pas dépasser 4000 caractères")
        String description,

        @NotNull(message = "La catégorie est obligatoire")
        Category category,

        @NotNull(message = "La priorité est obligatoire")
        Priority priority) {
}
