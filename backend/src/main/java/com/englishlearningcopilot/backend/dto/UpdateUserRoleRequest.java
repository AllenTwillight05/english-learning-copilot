package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull UserRole role) {
}
