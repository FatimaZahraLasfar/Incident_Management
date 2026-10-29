package com.example.incidents.dto.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "La note est obligatoire")
        @Size(max = 2000, message = "La note ne doit pas dépasser 2000 caractères")
        String content) {
}
