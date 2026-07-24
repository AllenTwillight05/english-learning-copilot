package com.englishlearningcopilot.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishlearningcopilot.backend.dto.AuthResponse;
import com.englishlearningcopilot.backend.dto.LoginRequest;
import com.englishlearningcopilot.backend.dto.RegisterRequest;
import com.englishlearningcopilot.backend.dto.UserResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.exception.ConflictException;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.security.JwtService;
import com.englishlearningcopilot.backend.service.impl.AuthServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerNormalizesInputHashesPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest(
                " learner ",
                " LEARNER@EXAMPLE.COM ",
                "Password123",
                " Learner "
        );
        when(userRepository.existsByUsername("learner")).thenReturn(false);
        when(userRepository.existsByEmail("learner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(AppUser.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("learner");
        assertThat(response.user().email()).isEqualTo("learner@example.com");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getDisplayName()).isEqualTo("Learner");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsernameBeforeEncodingPassword() {
        RegisterRequest request = new RegisterRequest(
                "learner",
                "learner@example.com",
                "Password123",
                "Learner"
        );
        when(userRepository.existsByUsername("learner")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username is already registered.");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginAuthenticatesByUsernameUpdatesLastLoginAndReturnsToken() {
        AppUser user = user("learner", "learner@example.com");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("learner", "Password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("learner");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("learner", "Password123")
        );
        verify(userRepository).save(user);
    }

    @Test
    void loginFallsBackToEmailLookupUsingLowercaseEmail() {
        AppUser user = user("learner", "learner@example.com");
        when(userRepository.findByUsername("LEARNER@EXAMPLE.COM")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("LEARNER@EXAMPLE.COM", "Password123"));

        assertThat(response.user().email()).isEqualTo("learner@example.com");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("learner", "Password123")
        );
    }

    @Test
    void currentUserReturnsUserResponseForExistingUser() {
        AppUser user = user("learner", "learner@example.com");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));

        UserResponse response = authService.currentUser("learner");

        assertThat(response.username()).isEqualTo("learner");
        assertThat(response.email()).isEqualTo("learner@example.com");
    }

    @Test
    void currentUserThrowsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.currentUser("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Current user was not found.");
    }

    private static AppUser user(String username, String email) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName("Learner");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        return user;
    }
}
