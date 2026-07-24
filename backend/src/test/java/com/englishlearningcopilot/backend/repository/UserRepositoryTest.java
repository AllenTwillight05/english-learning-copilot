package com.englishlearningcopilot.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = "debug=false")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUserByUsernameAndEmail() {
        AppUser saved = userRepository.save(user("learner", "learner@example.com"));

        assertThat(userRepository.findByUsername("learner")).contains(saved);
        assertThat(userRepository.findByEmail("learner@example.com")).contains(saved);
    }

    @Test
    void checksUsernameAndEmailExistence() {
        userRepository.save(user("learner", "learner@example.com"));

        assertThat(userRepository.existsByUsername("learner")).isTrue();
        assertThat(userRepository.existsByEmail("learner@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("missing")).isFalse();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    private static AppUser user(String username, String email) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Learner");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        return user;
    }
}
