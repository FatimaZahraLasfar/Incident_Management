package com.example.incidents.dto.admin;

import com.example.incidents.enums.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull(message = "Le rôle est obligatoire") Role role) {
}
