package com.englishlearningcopilot.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishlearningcopilot.backend.dto.CreateSpeakingSessionRequest;
import com.englishlearningcopilot.backend.dto.SpeakingSessionResponse;
import com.englishlearningcopilot.backend.dto.SpeakingTurnResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import com.englishlearningcopilot.backend.entity.SpeakingSession;
import com.englishlearningcopilot.backend.entity.SpeakingSessionStatus;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.exception.BadRequestException;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.repository.SpeakingScenarioRepository;
import com.englishlearningcopilot.backend.repository.SpeakingSessionRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.service.agent.SpeakingAgentClient;
import com.englishlearningcopilot.backend.service.agent.SpeakingAgentReply;
import com.englishlearningcopilot.backend.service.impl.SpeakingServiceImpl;
import com.englishlearningcopilot.backend.service.speech.AsrService;
import com.englishlearningcopilot.backend.service.speech.IseService;
import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import com.englishlearningcopilot.backend.service.speech.TtsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpeakingServiceImplTest {

    @Mock
    private SpeakingScenarioRepository scenarioRepository;

    @Mock
    private SpeakingSessionRepository sessionRepository;

    @Mock
    private SpeakingMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpeakingAgentClient agentClient;

    @Mock
    private AsrService asrService;

    @Mock
    private TtsService ttsService;

    @Mock
    private IseService iseService;

    @Mock
    private SpeakingAudioStorageService audioStorageService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SpeakingServiceImpl speakingService;

    @Test
    void listScenariosReturnsActiveScenariosSortedByRepository() {
        when(scenarioRepository.findByActiveTrueOrderByTitleAsc()).thenReturn(List.of(scenario("business-opening")));

        var scenarios = speakingService.listScenarios();

        assertThat(scenarios).extracting("id").containsExactly("business-opening");
    }

    @Test
    void createSessionPersistsSessionAndOpeningMessage() {
        AppUser user = user(7L, "learner");
        SpeakingScenario scenario = scenario("business-opening");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(scenarioRepository.findById("business-opening")).thenReturn(Optional.of(scenario));
        when(sessionRepository.save(any(SpeakingSession.class))).thenAnswer(invocation -> {
            SpeakingSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 99L);
            return session;
        });
        when(messageRepository.save(any(SpeakingMessage.class))).thenAnswer(invocation -> {
            SpeakingMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 100L);
            return message;
        });
        when(messageRepository.findBySessionIdOrderByTurnIndexAscCreatedAtAsc(99L)).thenReturn(List.of());

        SpeakingSessionResponse response =
                speakingService.createSession("learner", new CreateSpeakingSessionRequest("business-opening"));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.scenario().id()).isEqualTo("business-opening");
        verify(messageRepository).save(any(SpeakingMessage.class));
    }

    @Test
    void createSessionRejectsInactiveScenario() {
        SpeakingScenario scenario = scenario("business-opening");
        scenario.setActive(false);
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user(7L, "learner")));
        when(scenarioRepository.findById("business-opening")).thenReturn(Optional.of(scenario));

        assertThatThrownBy(() -> speakingService.createSession(
                "learner",
                new CreateSpeakingSessionRequest("business-opening")
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Speaking scenario was not found.");
    }

    @Test
    void getSessionRejectsSessionOwnedByAnotherUser() {
        SpeakingSession session = session(99L, user(8L, "other"), scenario("business-opening"));
        when(sessionRepository.findById(99L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> speakingService.getSession("learner", 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("This speaking session belongs to another user.");
    }

    @Test
    void submitRecordingProcessesSpeechAndPersistsUserAndAgentMessages() throws Exception {
        AppUser user = user(7L, "learner");
        SpeakingScenario scenario = scenario("business-opening");
        SpeakingSession session = session(99L, user, scenario);
        AtomicLong ids = new AtomicLong(100);
        when(sessionRepository.findById(99L)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(SpeakingMessage.class))).thenAnswer(invocation -> {
            SpeakingMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                ReflectionTestUtils.setField(message, "id", ids.getAndIncrement());
            }
            return message;
        });
        when(audioStorageService.save(eq(99L), any(), any())).thenReturn("/audio/turn.wav");
        when(asrService.transcribe(any())).thenReturn("Hello, nice to meet you.");
        when(iseService.evaluate(any(), eq(null))).thenReturn(new PronunciationScore(88, 87, 86, 85, 120));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(messageRepository.findBySessionIdOrderByTurnIndexAscCreatedAtAsc(99L)).thenReturn(List.of());
        when(agentClient.reply(eq(scenario), any(), eq("Hello, nice to meet you."), eq(1)))
                .thenReturn(new SpeakingAgentReply("Welcome to the meeting.", "Use a fuller greeting."));
        when(ttsService.synthesize("Welcome to the meeting.")).thenReturn(new byte[] {9, 8});
        when(sessionRepository.save(session)).thenReturn(session);

        SpeakingTurnResponse response = speakingService.submitRecording(
                "learner",
                99L,
                new MockMultipartFile("audio", "turn.wav", "audio/wav", new byte[] {1, 2, 3})
        );

        assertThat(response.userMessage().content()).isEqualTo("Hello, nice to meet you.");
        assertThat(response.agentMessage().content()).isEqualTo("Welcome to the meeting.");
        assertThat(response.pronunciationScore().totalScore()).isEqualTo(88);
        assertThat(response.session().currentTurn()).isEqualTo(1);
        verify(agentClient).reply(eq(scenario), any(), eq("Hello, nice to meet you."), eq(1));
        verify(ttsService).synthesize("Welcome to the meeting.");
    }

    @Test
    void submitRecordingRejectsCompletedSession() {
        SpeakingSession session = session(99L, user(7L, "learner"), scenario("business-opening"));
        session.setStatus(SpeakingSessionStatus.COMPLETED);
        when(sessionRepository.findById(99L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> speakingService.submitRecording(
                "learner",
                99L,
                new MockMultipartFile("audio", "turn.wav", "audio/wav", new byte[] {1})
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Speaking session is not active.");
    }

    private static AppUser user(Long id, String username) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setDisplayName("Learner");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        return user;
    }

    private static SpeakingScenario scenario(String id) {
        SpeakingScenario scenario = new SpeakingScenario();
        scenario.setId(id);
        scenario.setTitle("Business Opening");
        scenario.setDescription("description");
        scenario.setDifficulty("starter");
        scenario.setAccent("US");
        scenario.setDuration("5 min");
        scenario.setSummary("summary");
        scenario.setTone("friendly");
        scenario.setGoal("goal");
        scenario.setKeywords("greeting,meeting");
        scenario.setRolePrompt("role prompt");
        scenario.setOpeningMessage("Hello");
        scenario.setTargetTurns(3);
        scenario.setScoringRubric("rubric");
        scenario.setActive(true);
        return scenario;
    }

    private static SpeakingSession session(Long id, AppUser user, SpeakingScenario scenario) {
        SpeakingSession session = new SpeakingSession();
        ReflectionTestUtils.setField(session, "id", id);
        session.setUser(user);
        session.setScenario(scenario);
        session.setStatus(SpeakingSessionStatus.ACTIVE);
        session.setStartedAt(Instant.parse("2026-07-21T00:00:00Z"));
        session.setTargetTurns(3);
        session.setCurrentTurn(0);
        return session;
    }
}
