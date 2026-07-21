package com.englishlearningcopilot.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishlearningcopilot.backend.dto.AuthResponse;
import com.englishlearningcopilot.backend.dto.RegisterRequest;
import com.englishlearningcopilot.backend.dto.UserResponse;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.exception.ConflictException;
import com.englishlearningcopilot.backend.exception.GlobalExceptionHandler;
import com.englishlearningcopilot.backend.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerDelegatesValidRequestToService() throws Exception {
        UserResponse user = userResponse("learner");
        when(authService.register(any(RegisterRequest.class))).thenReturn(new AuthResponse("jwt-token", user));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "learner",
                                  "email": "learner@example.com",
                                  "password": "Password123",
                                  "displayName": "Learner"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.username").value("learner"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        RegisterRequest request = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.email()).isEqualTo("learner@example.com");
    }

    @Test
    void registerReturnsBadRequestWhenBodyFailsValidation() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "email": "not-an-email",
                                  "password": "short",
                                  "displayName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    void registerMapsServiceConflictToConflictResponse() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Username is already registered."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "learner",
                                  "email": "learner@example.com",
                                  "password": "Password123",
                                  "displayName": "Learner"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already registered."));
    }

    @Test
    void meUsesPrincipalName() throws Exception {
        when(authService.currentUser("learner")).thenReturn(userResponse("learner"));

        mockMvc.perform(get("/api/auth/me").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("learner"));

        verify(authService).currentUser("learner");
    }

    @Test
    void logoutReturnsClientTokenRemovalMessage() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("remove the token")));
    }

    private static UserResponse userResponse(String username) {
        Instant now = Instant.parse("2026-07-21T00:00:00Z");
        return new UserResponse(
                1L,
                username,
                username + "@example.com",
                "Learner",
                UserRole.USER,
                true,
                now,
                now,
                null
        );
    }
}
