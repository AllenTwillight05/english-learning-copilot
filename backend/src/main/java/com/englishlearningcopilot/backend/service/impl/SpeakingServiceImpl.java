package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.CreateSpeakingMessageRequest;
import com.englishlearningcopilot.backend.dto.CreateSpeakingSessionRequest;
import com.englishlearningcopilot.backend.dto.SpeakingMessageResponse;
import com.englishlearningcopilot.backend.dto.SpeakingScenarioResponse;
import com.englishlearningcopilot.backend.dto.SpeakingSessionResponse;
import com.englishlearningcopilot.backend.dto.SpeakingTurnResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import com.englishlearningcopilot.backend.entity.SpeakingSession;
import com.englishlearningcopilot.backend.entity.SpeakingSessionStatus;
import com.englishlearningcopilot.backend.exception.BadRequestException;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.repository.SpeakingScenarioRepository;
import com.englishlearningcopilot.backend.repository.SpeakingSessionRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.service.SpeakingService;
import com.englishlearningcopilot.backend.service.agent.SpeakingAgentClient;
import com.englishlearningcopilot.backend.service.agent.SpeakingAgentReply;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpeakingServiceImpl implements SpeakingService {

    private final SpeakingScenarioRepository scenarioRepository;
    private final SpeakingSessionRepository sessionRepository;
    private final SpeakingMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SpeakingAgentClient agentClient;

    public SpeakingServiceImpl(
            SpeakingScenarioRepository scenarioRepository,
            SpeakingSessionRepository sessionRepository,
            SpeakingMessageRepository messageRepository,
            UserRepository userRepository,
            SpeakingAgentClient agentClient
    ) {
        this.scenarioRepository = scenarioRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.agentClient = agentClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpeakingScenarioResponse> listScenarios() {
        return scenarioRepository.findByActiveTrueOrderByTitleAsc().stream()
                .map(SpeakingScenarioResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingScenarioResponse getScenario(String scenarioId) {
        return SpeakingScenarioResponse.from(findActiveScenario(scenarioId));
    }

    @Override
    @Transactional
    public SpeakingSessionResponse createSession(String username, CreateSpeakingSessionRequest request) {
        AppUser user = findUser(username);
        SpeakingScenario scenario = findActiveScenario(request.scenarioId());

        SpeakingSession session = new SpeakingSession();
        session.setUser(user);
        session.setScenario(scenario);
        session.setStatus(SpeakingSessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        session.setTargetTurns(scenario.getTargetTurns());
        session.setCurrentTurn(0);
        SpeakingSession savedSession = sessionRepository.save(session);

        SpeakingMessage openingMessage = new SpeakingMessage();
        openingMessage.setSession(savedSession);
        openingMessage.setSender(SpeakingMessageSender.AGENT);
        openingMessage.setContent(scenario.getOpeningMessage());
        openingMessage.setTurnIndex(0);
        messageRepository.save(openingMessage);

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingSessionResponse getSession(String username, Long sessionId) {
        SpeakingSession session = findOwnedSession(username, sessionId);
        return toSessionResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpeakingSessionResponse> listHistory(String username) {
        findUser(username);
        return sessionRepository.findByUserUsernameOrderByStartedAtDesc(username).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Override
    @Transactional
    public SpeakingTurnResponse addMessage(String username, Long sessionId, CreateSpeakingMessageRequest request) {
        SpeakingSession session = findOwnedSession(username, sessionId);
        if (session.getStatus() != SpeakingSessionStatus.ACTIVE) {
            throw new BadRequestException("Speaking session is not active.");
        }

        String userContent = request.content().trim();
        int nextTurn = session.getCurrentTurn() + 1;

        SpeakingMessage userMessage = new SpeakingMessage();
        userMessage.setSession(session);
        userMessage.setSender(SpeakingMessageSender.USER);
        userMessage.setContent(userContent);
        userMessage.setTurnIndex(nextTurn);
        SpeakingMessage savedUserMessage = messageRepository.save(userMessage);

        List<SpeakingMessage> history = messageRepository.findBySessionIdOrderByTurnIndexAscCreatedAtAsc(sessionId);
        SpeakingAgentReply reply = agentClient.reply(session.getScenario(), history, userContent, nextTurn);

        SpeakingMessage agentMessage = new SpeakingMessage();
        agentMessage.setSession(session);
        agentMessage.setSender(SpeakingMessageSender.AGENT);
        agentMessage.setContent(reply.content());
        agentMessage.setInstantTip(reply.instantTip());
        agentMessage.setTurnIndex(nextTurn);
        SpeakingMessage savedAgentMessage = messageRepository.save(agentMessage);

        session.setCurrentTurn(nextTurn);
        SpeakingSession savedSession = sessionRepository.save(session);

        return new SpeakingTurnResponse(
                SpeakingMessageResponse.from(savedUserMessage),
                SpeakingMessageResponse.from(savedAgentMessage),
                toSessionResponse(savedSession)
        );
    }

    private SpeakingSessionResponse toSessionResponse(SpeakingSession session) {
        List<SpeakingMessageResponse> messages = messageRepository.findBySessionIdOrderByTurnIndexAscCreatedAtAsc(session.getId())
                .stream()
                .map(SpeakingMessageResponse::from)
                .toList();
        return SpeakingSessionResponse.from(session, messages);
    }

    private AppUser findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
    }

    private SpeakingScenario findActiveScenario(String scenarioId) {
        SpeakingScenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaking scenario was not found."));
        if (!scenario.isActive()) {
            throw new ResourceNotFoundException("Speaking scenario was not found.");
        }
        return scenario;
    }

    private SpeakingSession findOwnedSession(String username, Long sessionId) {
        SpeakingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaking session was not found."));
        if (!session.getUser().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException("This speaking session belongs to another user.");
        }
        return session;
    }
}
