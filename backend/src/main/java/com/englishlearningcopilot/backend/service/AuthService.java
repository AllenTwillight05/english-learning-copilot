package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.AuthResponse;
import com.englishlearningcopilot.backend.dto.LoginRequest;
import com.englishlearningcopilot.backend.dto.RegisterRequest;
import com.englishlearningcopilot.backend.dto.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse currentUser(String username);
}
