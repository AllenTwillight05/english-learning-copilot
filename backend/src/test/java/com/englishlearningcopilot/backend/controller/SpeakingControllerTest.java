package com.englishlearningcopilot.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishlearningcopilot.backend.dto.CreateSpeakingSessionRequest;
import com.englishlearningcopilot.backend.dto.SpeakingFeedbackResponse;
import com.englishlearningcopilot.backend.dto.SpeakingMessageResponse;
import com.englishlearningcopilot.backend.dto.SpeakingScenarioResponse;
import com.englishlearningcopilot.backend.dto.SpeakingSessionResponse;
import com.englishlearningcopilot.backend.dto.SpeakingTurnResponse;
import com.englishlearningcopilot.backend.exception.GlobalExceptionHandler;
import com.englishlearningcopilot.backend.service.SpeakingService;
import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class SpeakingControllerTest {

    @Mock
    private SpeakingService speakingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SpeakingController(speakingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listScenariosReturnsActiveScenarios() throws Exception {
        when(speakingService.listScenarios()).thenReturn(List.of(scenario()));

        mockMvc.perform(get("/api/speaking/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("business-opening"));
    }

    @Test
    void createSessionValidatesBodyAndDelegates() throws Exception {
        when(speakingService.createSession(eq("learner"), any(CreateSpeakingSessionRequest.class)))
                .thenReturn(sessionResponse(List.of()));

        mockMvc.perform(post("/api/speaking/sessions")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioId": "business-opening",
                                  "selectedTopic": "Hometown"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.scenario.id").value("business-opening"));

        ArgumentCaptor<CreateSpeakingSessionRequest> captor =
                ArgumentCaptor.forClass(CreateSpeakingSessionRequest.class);
        verify(speakingService).createSession(eq("learner"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().scenarioId()).isEqualTo("business-opening");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().selectedTopic()).isEqualTo("Hometown");
    }

    @Test
    void createSessionRejectsBlankScenarioId() throws Exception {
        mockMvc.perform(post("/api/speaking/sessions")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioId": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.scenarioId").exists());
    }

    @Test
    void submitRecordingPassesMultipartAudio() throws Exception {
        SpeakingMessageResponse userMessage = message(1L, "USER", "hello");
        SpeakingMessageResponse agentMessage = message(2L, "AGENT", "welcome");
        when(speakingService.submitRecording(eq("learner"), eq(99L), any(), eq(1234L)))
                .thenReturn(new SpeakingTurnResponse(
                        userMessage,
                        agentMessage,
                        new PronunciationScore(88, 87, 86, 85, 120),
                        sessionResponse(List.of(userMessage, agentMessage))
                ));

        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "turn.wav",
                "audio/wav",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/api/speaking/sessions/99/messages")
                        .file(audio)
                        .param("durationMs", "1234")
                        .principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessage.content").value("hello"))
                .andExpect(jsonPath("$.agentMessage.content").value("welcome"))
                .andExpect(jsonPath("$.pronunciationScore.totalScore").value(88.0));

        verify(speakingService).submitRecording(eq("learner"), eq(99L), any(), eq(1234L));
    }

    @Test
    void getFeedbackReturnsSessionFeedback() throws Exception {
        when(speakingService.getFeedback("learner", 99L))
                .thenReturn(new SpeakingFeedbackResponse(
                        90,
                        88,
                        86,
                        84,
                        "120 WPM",
                        List.of("hello"),
                        List.of("add detail"),
                        "Business Opening",
                        1,
                        88.0,
                        List.of(),
                        "Keep practicing."
                ));

        mockMvc.perform(get("/api/speaking/sessions/99/feedback").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioTitle").value("Business Opening"))
                .andExpect(jsonPath("$.totalTurns").value(1));
    }

    private static SpeakingScenarioResponse scenario() {
        return new SpeakingScenarioResponse(
                "business-opening",
                "Business Opening",
                "description",
                "starter",
                "starter",
                "US",
                "5 min",
                "summary",
                "friendly",
                "goal",
                List.of("greeting"),
                "Hello",
                "A: Hello",
                3,
                "rubric"
        );
    }

    private static SpeakingSessionResponse sessionResponse(List<SpeakingMessageResponse> messages) {
        return new SpeakingSessionResponse(
                99L,
                7L,
                scenario(),
                "ACTIVE",
                Instant.parse("2026-07-21T00:00:00Z"),
                null,
                0,
                3,
                "Hometown",
                messages
        );
    }

    private static SpeakingMessageResponse message(Long id, String sender, String content) {
        return new SpeakingMessageResponse(
                id,
                sender,
                content,
                "AGENT".equals(sender) ? content : null,
                null,
                "AGENT".equals(sender),
                content,
                null,
                null,
                null,
                1,
                null
        );
    }
}
