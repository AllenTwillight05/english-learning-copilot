package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.UpdateUserRoleRequest;
import com.englishlearningcopilot.backend.dto.UpdateUserStatusRequest;
import com.englishlearningcopilot.backend.dto.UserResponse;
import com.englishlearningcopilot.backend.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * GET /api/admin/users
     * Get all users
     */
    @GetMapping
    public List<UserResponse> listUsers() {
        return adminUserService.listUsers();
    }

    /**
     * PATCH /api/admin/users/{id}/role
     * Update a user's role（USER or ADMIN)
     */
    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return adminUserService.updateRole(id, request);
    }

    /**
     * PATCH /api/admin/users/{id}/status
     * Update a user's status(enable or not)
     */
    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return adminUserService.updateStatus(id, request);
    }
}
