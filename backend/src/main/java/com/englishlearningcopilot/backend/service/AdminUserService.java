package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.UpdateUserRoleRequest;
import com.englishlearningcopilot.backend.dto.UpdateUserStatusRequest;
import com.englishlearningcopilot.backend.dto.UserResponse;
import java.util.List;

public interface AdminUserService {

    List<UserResponse> listUsers();

    UserResponse updateRole(Long id, UpdateUserRoleRequest request);

    UserResponse updateStatus(Long id, UpdateUserStatusRequest request);
}
