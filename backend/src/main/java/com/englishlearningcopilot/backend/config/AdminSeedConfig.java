package com.englishlearningcopilot.backend.config;

import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedConfig {

    @Bean
    CommandLineRunner seedAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.seed.enabled}") boolean enabled,
            @Value("${app.admin.seed.username}") String username,
            @Value("${app.admin.seed.email}") String email,
            @Value("${app.admin.seed.password}") String password,
            @Value("${app.admin.seed.display-name}") String displayName
    ) {
        return args -> {
            if (!enabled || userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
                return;
            }

            AppUser admin = new AppUser();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setDisplayName(displayName);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        };
    }
}
