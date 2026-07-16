package com.englishlearningcopilot.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.repository.SpeakingSessionRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
class SpeakingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpeakingSessionRepository speakingSessionRepository;

    @Autowired
    private SpeakingMessageRepository speakingMessageRepository;

    @BeforeEach
    void setUp() {
        clearUserPracticeData();
    }

    @AfterEach
    void tearDown() {
        clearUserPracticeData();
    }

    @Test
    void speakingTextConversationPersistsSessionMessagesAndHistory() throws Exception {
        mockMvc.perform(get("/api/speaking/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString());

        String token = registerAndExtractToken();

        MvcResult createdSessionResult = mockMvc.perform(post("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioId": "business-opening"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scenario.id").value("business-opening"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.messages[0].sender").value("AGENT"))
                .andReturn();

        Long sessionId = objectMapper.readTree(createdSessionResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(post("/api/speaking/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Today I would like to discuss the delivery timeline."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessage.sender").value("USER"))
                .andExpect(jsonPath("$.agentMessage.sender").value("AGENT"))
                .andExpect(jsonPath("$.agentMessage.content").isString())
                .andExpect(jsonPath("$.agentMessage.instantTip").isString())
                .andExpect(jsonPath("$.session.currentTurn").value(1))
                .andExpect(jsonPath("$.session.messages.length()").value(3));

        mockMvc.perform(get("/api/speaking/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId))
                .andExpect(jsonPath("$[0].messages.length()").value(3));
    }

    @Test
    void speakingSessionApisRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/speaking/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioId": "business-opening"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/speaking/history"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndExtractToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "speaker",
                                  "email": "speaker@example.com",
                                  "password": "Password123",
                                  "displayName": "Speaker"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private void clearUserPracticeData() {
        speakingMessageRepository.deleteAll();
        speakingSessionRepository.deleteAll();
        userRepository.deleteAll();
    }
}
