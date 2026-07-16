package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.AuthResponse;
import com.englishlearningcopilot.backend.dto.LoginRequest;
import com.englishlearningcopilot.backend.dto.MessageResponse;
import com.englishlearningcopilot.backend.dto.RegisterRequest;
import com.englishlearningcopilot.backend.dto.UserResponse;
import com.englishlearningcopilot.backend.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Register a new user
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * POST /api/auth/login
     * Log in a user
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * GET /api/auth/me
     * Get the current user
     */
    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return authService.currentUser(principal.getName());
    }

    /**
     * POST /api/auth/logout
     * Log out the current user
     */
    @PostMapping("/logout")
    public MessageResponse logout() {
        return new MessageResponse("Logged out. Please remove the token on the client.");
    }
}
