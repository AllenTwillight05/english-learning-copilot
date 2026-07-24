package com.englishlearningcopilot.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.mock.web.MockMultipartFile;
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
    void speakingRecordingConversationPersistsSessionMessagesAndHistory() throws Exception {
        mockMvc.perform(get("/api/speaking/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString());

        String token = registerAndExtractToken();

        MvcResult createdSessionResult = mockMvc.perform(post("/api/speaking/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioId": "IELTS-P1-practice",
                                  "selectedTopic": "Hometown"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scenario.id").value("IELTS-P1-practice"))
                .andExpect(jsonPath("$.selectedTopic").value("Hometown"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.messages[0].sender").value("AGENT"))
                .andExpect(jsonPath("$.messages[0].content").isString())
                .andExpect(jsonPath("$.messages[0].spokenText").isString())
                .andExpect(jsonPath("$.messages[0].autoPlay").value(true))
                .andReturn();

        Long sessionId = objectMapper.readTree(createdSessionResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", new byte[]{0x1a, 0x45, 0x1f});

        mockMvc.perform(multipart("/api/speaking/sessions/" + sessionId + "/messages")
                        .file(audioFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessage.sender").value("USER"))
                .andExpect(jsonPath("$.userMessage.transcribedText").isString())
                .andExpect(jsonPath("$.userMessage.pronunciationScore").value(nullValue()))
                .andExpect(jsonPath("$.agentMessage.sender").value("AGENT"))
                .andExpect(jsonPath("$.agentMessage.content").isString())
                .andExpect(jsonPath("$.agentMessage.spokenText").isString())
                .andExpect(jsonPath("$.agentMessage.autoPlay").value(true))
                .andExpect(jsonPath("$.agentMessage.instantTip").isString())
                .andExpect(jsonPath("$.pronunciationScore").value(nullValue()))
                .andExpect(jsonPath("$.session.currentTurn").value(1))
                .andExpect(jsonPath("$.session.selectedTopic").value("Hometown"))
                .andExpect(jsonPath("$.session.messages.length()").value(3));

        mockMvc.perform(get("/api/speaking/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId))
                .andExpect(jsonPath("$[0].selectedTopic").value("Hometown"))
                .andExpect(jsonPath("$[0].messages.length()").value(3));

        // Verify feedback endpoint returns expected structure including new fields
        mockMvc.perform(get("/api/speaking/sessions/" + sessionId + "/feedback")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").isNumber())
                .andExpect(jsonPath("$.pronunciation").isNumber())
                .andExpect(jsonPath("$.fluency").isNumber())
                .andExpect(jsonPath("$.speed").isString())
                .andExpect(jsonPath("$.issueSentences").isArray())
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.suggestions.length()").value(3))
                .andExpect(jsonPath("$.scenarioTitle").isString())
                .andExpect(jsonPath("$.totalTurns").isNumber())
                .andExpect(jsonPath("$.averagePronunciationScore").isNumber())
                .andExpect(jsonPath("$.turns").isArray())
                .andExpect(jsonPath("$.agentOverallComment").isString());
    }

    @Test
    void submitRecordingReturnsTranscribedTextAndAgentReplyWithoutBlockingOnPronunciationScores() throws Exception {
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
                .andReturn();

        Long sessionId = objectMapper.readTree(createdSessionResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", new byte[]{0x1a, 0x45, 0x1f});

        mockMvc.perform(multipart("/api/speaking/sessions/" + sessionId + "/messages")
                        .file(audioFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessage.sender").value("USER"))
                .andExpect(jsonPath("$.userMessage.content").isString())
                .andExpect(jsonPath("$.userMessage.audioUrl").isString())
                .andExpect(jsonPath("$.userMessage.spokenText").value(nullValue()))
                .andExpect(jsonPath("$.userMessage.autoPlay").value(false))
                .andExpect(jsonPath("$.userMessage.transcribedText").isString())
                .andExpect(jsonPath("$.userMessage.pronunciationScore").value(nullValue()))
                .andExpect(jsonPath("$.userMessage.pronunciationDetail").value(nullValue()))
                .andExpect(jsonPath("$.agentMessage.sender").value("AGENT"))
                .andExpect(jsonPath("$.agentMessage.content").isString())
                .andExpect(jsonPath("$.agentMessage.spokenText").isString())
                .andExpect(jsonPath("$.agentMessage.autoPlay").value(true))
                .andExpect(jsonPath("$.pronunciationScore").value(nullValue()))
                .andExpect(jsonPath("$.session.currentTurn").value(1));
    }

    @Test
    void speakingTextMessageEndpointIsNotSupported() throws Exception {
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
                .andReturn();

        Long sessionId = objectMapper.readTree(createdSessionResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(post("/api/speaking/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "This text endpoint has been removed."
                                }
                                """))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported request content type."));
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

        mockMvc.perform(get("/api/speaking/sessions/1/feedback"))
                .andExpect(status().isUnauthorized());

        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "recording.webm", "audio/webm", new byte[]{0x1a, 0x45});

        mockMvc.perform(multipart("/api/speaking/sessions/1/messages")
                        .file(audioFile))
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
