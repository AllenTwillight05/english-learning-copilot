package com.englishlearningcopilot.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.repository.SpeakingSessionRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class AuthAndAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpeakingMessageRepository speakingMessageRepository;

    @Autowired
    private SpeakingSessionRepository speakingSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        speakingMessageRepository.deleteAll();
        speakingSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerCreatesUserWithHashedPassword() throws Exception {
        String body = """
                {
                  "username": "learner",
                  "email": "learner@example.com",
                  "password": "Password123",
                  "displayName": "Learner"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username").value("learner"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        AppUser saved = userRepository.findByUsername("learner").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Password123");
        assertThat(passwordEncoder.matches("Password123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsernameOrEmail() throws Exception {
        createUser("learner", "learner@example.com", UserRole.USER);

        String duplicateUsername = """
                {
                  "username": "learner",
                  "email": "other@example.com",
                  "password": "Password123",
                  "displayName": "Other"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateUsername))
                .andExpect(status().isConflict());

        String duplicateEmail = """
                {
                  "username": "other",
                  "email": "learner@example.com",
                  "password": "Password123",
                  "displayName": "Other"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateEmail))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsTokenAndRejectsWrongPassword() throws Exception {
        createUser("learner", "learner@example.com", UserRole.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "learner@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username").value("learner"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "learner",
                                  "password": "WrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresBearerTokenAndReturnsCurrentUser() throws Exception {
        createUser("learner", "learner@example.com", UserRole.USER);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        String token = loginAndExtractToken("learner", "Password123");
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("learner"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void userCannotAccessAdminApis() throws Exception {
        createUser("learner", "learner@example.com", UserRole.USER);
        String token = loginAndExtractToken("learner", "Password123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageUsersAndReachReservedApi() throws Exception {
        AppUser learner = createUser("learner", "learner@example.com", UserRole.USER);
        createUser("admin", "admin@example.com", UserRole.ADMIN);
        String token = loginAndExtractToken("admin", "Password123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());

        mockMvc.perform(patch("/api/admin/users/" + learner.getId() + "/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(patch("/api/admin/users/" + learner.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/admin/question-types")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.resource").value("question-types"))
                .andExpect(jsonPath("$.operation").value("GET"));
    }

    private AppUser createUser(String username, String email, UserRole role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(username);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private String loginAndExtractToken(String account, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "%s"
                                }
                                """.formatted(account, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
