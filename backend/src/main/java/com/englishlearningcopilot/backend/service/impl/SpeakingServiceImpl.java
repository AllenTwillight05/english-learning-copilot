package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.CreateSpeakingSessionRequest;
import com.englishlearningcopilot.backend.dto.SpeakingFeedbackResponse;
import com.englishlearningcopilot.backend.dto.SpeakingMessageResponse;
import com.englishlearningcopilot.backend.dto.SpeakingScenarioResponse;
import com.englishlearningcopilot.backend.dto.SpeakingSessionResponse;
import com.englishlearningcopilot.backend.dto.SpeakingTurnResponse;
import com.englishlearningcopilot.backend.dto.TurnFeedback;
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
import com.englishlearningcopilot.backend.service.SpeakingAudioStorageService;
import com.englishlearningcopilot.backend.service.SpeakingAgentAudioSynthesisService;
import com.englishlearningcopilot.backend.service.SpeakingPronunciationEvaluationService;
import com.englishlearningcopilot.backend.service.SpeakingService;
import com.englishlearningcopilot.backend.service.agent.SpeakingAgentClient;
import com.englishlearningcopilot.backend.service.agent.SpeakingAgentReply;
import com.englishlearningcopilot.backend.service.speech.AsrService;
import com.englishlearningcopilot.backend.service.speech.EnglishSpeechText;
import com.englishlearningcopilot.backend.service.speech.IseService;
import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import com.englishlearningcopilot.backend.service.speech.xfyun.XfyunAsrException;
import com.englishlearningcopilot.backend.service.speech.xfyun.XfyunIseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SpeakingServiceImpl implements SpeakingService {

    private static final Logger log = LoggerFactory.getLogger(SpeakingServiceImpl.class);

    private final SpeakingScenarioRepository scenarioRepository;
    private final SpeakingSessionRepository sessionRepository;
    private final SpeakingMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SpeakingAgentClient agentClient;
    private final AsrService asrService;
    private final IseService iseService;
    private final SpeakingAudioStorageService audioStorageService;
    private final SpeakingAgentAudioSynthesisService agentAudioSynthesisService;
    private final SpeakingPronunciationEvaluationService pronunciationEvaluationService;
    private final ObjectMapper objectMapper;
    private final boolean evaluatePronunciationOnTurn;
    private final boolean evaluatePronunciationAsync;

    public SpeakingServiceImpl(
            SpeakingScenarioRepository scenarioRepository,
            SpeakingSessionRepository sessionRepository,
            SpeakingMessageRepository messageRepository,
            UserRepository userRepository,
            SpeakingAgentClient agentClient,
            AsrService asrService,
            IseService iseService,
            SpeakingAudioStorageService audioStorageService,
            SpeakingAgentAudioSynthesisService agentAudioSynthesisService,
            SpeakingPronunciationEvaluationService pronunciationEvaluationService,
            ObjectMapper objectMapper,
            @Value("${speaking.pronunciation.evaluate-on-turn:false}") boolean evaluatePronunciationOnTurn,
            @Value("${speaking.pronunciation.evaluate-async:true}") boolean evaluatePronunciationAsync
    ) {
        this.scenarioRepository = scenarioRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.agentClient = agentClient;
        this.asrService = asrService;
        this.iseService = iseService;
        this.audioStorageService = audioStorageService;
        this.agentAudioSynthesisService = agentAudioSynthesisService;
        this.pronunciationEvaluationService = pronunciationEvaluationService;
        this.objectMapper = objectMapper;
        this.evaluatePronunciationOnTurn = evaluatePronunciationOnTurn;
        this.evaluatePronunciationAsync = evaluatePronunciationAsync;
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
        session.setSelectedTopic(normalizeSelectedTopic(request.selectedTopic()));
        SpeakingSession savedSession = sessionRepository.save(session);

        long modelStartedAt = System.nanoTime();
        SpeakingAgentReply openingReply = agentClient.reply(scenario, savedSession.getSelectedTopic(), List.of(), "", 0);
        saveAgentMessageWithAudio(savedSession, openingReply, 0);
        log.info("Speaking session opening generated: sessionId={}, llmMs={}",
                savedSession.getId(), (System.nanoTime() - modelStartedAt) / 1_000_000);

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
    public SpeakingTurnResponse submitRecording(String username, Long sessionId, MultipartFile audio, Long durationMs) {
        long requestStartedAt = System.nanoTime();
        SpeakingSession session = findOwnedSession(username, sessionId);
        if (session.getStatus() != SpeakingSessionStatus.ACTIVE) {
            throw new BadRequestException("Speaking session is not active.");
        }

        byte[] audioBytes;
        try {
            audioBytes = audio.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded audio file.");
        }

        int nextTurn = session.getCurrentTurn() + 1;

        // Save a placeholder USER message first to get an ID for file naming
        SpeakingMessage userMessage = new SpeakingMessage();
        userMessage.setSession(session);
        userMessage.setSender(SpeakingMessageSender.USER);
        userMessage.setContent(""); // will be filled after ASR
        userMessage.setTurnIndex(nextTurn);
        SpeakingMessage savedUserMessage = messageRepository.save(userMessage);

        // Save audio file
        String audioUrl = audioStorageService.save(sessionId, savedUserMessage.getId(), audioBytes);

        String transcribedText;
        PronunciationScore pronunciationScore = null;
        long asrStartedAt = System.nanoTime();
        long iseMs = 0;
        try {
            transcribedText = asrService.transcribe(audioBytes, audio.getOriginalFilename());
            if (evaluatePronunciationOnTurn
                    && EnglishSpeechText.isEligibleForPronunciationEvaluation(transcribedText)) {
                long iseStartedAt = System.nanoTime();
                pronunciationScore = iseService.evaluate(audioBytes, transcribedText);
                iseMs = (System.nanoTime() - iseStartedAt) / 1_000_000;
            }
        } catch (XfyunAsrException | XfyunIseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Speech processing failed: " + e.getMessage(), e);
        }
        long asrMs = (System.nanoTime() - asrStartedAt) / 1_000_000 - iseMs;

        // Serialize ISE detail to JSON
        String pronunciationDetail = null;
        try {
            if (pronunciationScore != null) {
                pronunciationDetail = objectMapper.writeValueAsString(pronunciationScore);
            }
        } catch (JsonProcessingException e) {
            pronunciationDetail = null;
        }

        // Update USER message with ASR + ISE results
        savedUserMessage.setContent(transcribedText);
        savedUserMessage.setAudioUrl(audioUrl);
        savedUserMessage.setTranscribedText(transcribedText);
        savedUserMessage.setPronunciationScore(pronunciationScore != null ? pronunciationScore.totalScore() : null);
        savedUserMessage.setPronunciationDetail(pronunciationDetail);
        savedUserMessage.setDurationMs(normalizeDurationMs(durationMs));
        savedUserMessage = messageRepository.save(savedUserMessage);
        if (pronunciationScore == null
                && evaluatePronunciationAsync
                && EnglishSpeechText.isEligibleForPronunciationEvaluation(transcribedText)) {
            scheduleAsyncPronunciationEvaluation(savedUserMessage.getId(), audioBytes, transcribedText);
        }

        // Agent reply
        List<SpeakingMessage> history = messageRepository.findBySessionIdOrderByTurnIndexAscCreatedAtAsc(sessionId);
        long modelStartedAt = System.nanoTime();
        SpeakingAgentReply reply = agentClient.reply(
                session.getScenario(),
                session.getSelectedTopic(),
                history,
                transcribedText,
                nextTurn
        );
        long modelMs = (System.nanoTime() - modelStartedAt) / 1_000_000;

        SpeakingMessage savedAgentMessage = saveAgentMessageWithAudio(session, reply, nextTurn);

        session.setCurrentTurn(nextTurn);
        SpeakingSession savedSession = sessionRepository.save(session);

        log.info("Speaking turn processed: sessionId={}, turn={}, asrMs={}, iseMs={}, llmMs={}, totalMs={}",
                sessionId,
                nextTurn,
                Math.max(0, asrMs),
                iseMs,
                modelMs,
                (System.nanoTime() - requestStartedAt) / 1_000_000);

        return new SpeakingTurnResponse(
                SpeakingMessageResponse.from(savedUserMessage),
                SpeakingMessageResponse.from(savedAgentMessage),
                pronunciationScore,
                toSessionResponse(savedSession)
        );
    }

    private Long normalizeDurationMs(Long durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return null;
        }
        return durationMs;
    }

    private void scheduleAsyncPronunciationEvaluation(Long messageId, byte[] audioBytes, String referenceText) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pronunciationEvaluationService.evaluateUserMessageAsync(messageId, audioBytes, referenceText);
                }
            });
            return;
        }
        pronunciationEvaluationService.evaluateUserMessageAsync(messageId, audioBytes, referenceText);
    }

    private void scheduleAgentAudioSynthesis(Long messageId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueueAgentAudioSynthesis(messageId);
                }
            });
            return;
        }
        enqueueAgentAudioSynthesis(messageId);
    }

    private void enqueueAgentAudioSynthesis(Long messageId) {
        try {
            agentAudioSynthesisService.synthesizeAgentMessageAsync(messageId);
        } catch (RuntimeException e) {
            log.warn("Could not queue Super Smart TTS for speaking message {}.", messageId, e);
        }
    }

    private SpeakingMessage saveAgentMessageWithAudio(
            SpeakingSession session,
            SpeakingAgentReply reply,
            int turnIndex
    ) {
        SpeakingMessage agentMessage = new SpeakingMessage();
        agentMessage.setSession(session);
        agentMessage.setSender(SpeakingMessageSender.AGENT);
        agentMessage.setContent(reply.content());
        agentMessage.setSpokenText(resolveSpokenText(reply));
        agentMessage.setInstantTip(reply.instantTip());
        agentMessage.setTurnIndex(turnIndex);
        agentMessage.setAudioPending(true);
        SpeakingMessage savedAgentMessage = messageRepository.save(agentMessage);
        scheduleAgentAudioSynthesis(savedAgentMessage.getId());
        return savedAgentMessage;
    }

    private String resolveSpokenText(SpeakingAgentReply reply) {
        if (reply.spokenText() != null && !reply.spokenText().isBlank()) {
            return reply.spokenText();
        }
        return reply.content();
    }

    @Override
    @Transactional
    public SpeakingFeedbackResponse getFeedback(String username, Long sessionId) {
        SpeakingSession session = findOwnedSession(username, sessionId);
        List<SpeakingMessage> messages = messageRepository.findBySessionIdOrderByTurnIndexAscCreatedAtAsc(sessionId);

        // Summary scores (mock aggregation from per-message ISE scores or random)
        List<SpeakingMessage> userMessages = messages.stream()
                .filter(m -> m.getSender() == SpeakingMessageSender.USER)
                .toList();

        // Issue sentences from low-scoring user turns.
        List<String> issueSentences = new ArrayList<>();
        for (SpeakingMessage userMessage : userMessages) {
            if (userMessage.getPronunciationScore() == null || userMessage.getContent() == null
                    || userMessage.getContent().isBlank()) {
                continue;
            }
            PronunciationScore score = readPronunciationScore(userMessage);
            if (score.totalScore() < 60) {
                issueSentences.add(userMessage.getContent());
            }
        }

        List<String> suggestions = List.of(
                "Try adding more detail to make your responses fuller and more natural.",
                "Pay attention to sentence stress — emphasize key words for clearer communication.",
                "Practice linking words together to improve your overall fluency."
        );

        // Per-turn feedback
        List<TurnFeedback> turns = new ArrayList<>();
        List<PronunciationScore> turnScores = new ArrayList<>();

        for (int turnIdx = 1; turnIdx <= session.getCurrentTurn(); turnIdx++) {
            final int t = turnIdx;
            List<SpeakingMessage> turnMsgs = messages.stream()
                    .filter(m -> m.getTurnIndex() == t)
                    .toList();

            SpeakingMessage userMsg = turnMsgs.stream()
                    .filter(m -> m.getSender() == SpeakingMessageSender.USER)
                    .findFirst().orElse(null);
            SpeakingMessage agentMsg = turnMsgs.stream()
                    .filter(m -> m.getSender() == SpeakingMessageSender.AGENT)
                    .findFirst().orElse(null);

            PronunciationScore ps;
            if (userMsg != null && userMsg.getPronunciationScore() != null) {
                ps = readPronunciationScore(userMsg);
                turnScores.add(ps);
            } else if (userMsg != null) {
                ps = evaluateMissingPronunciation(userMsg);
                if (ps != null) {
                    turnScores.add(ps);
                }
            } else {
                ps = null;
            }

            turns.add(new TurnFeedback(
                    turnIdx,
                    userMsg != null ? userMsg.getContent() : "",
                    agentMsg != null ? agentMsg.getContent() : "",
                    ps
            ));
        }

        double averagePronunciationScore = !turnScores.isEmpty()
                ? round1(turnScores.stream().mapToDouble(PronunciationScore::totalScore).average().orElse(0))
                : 0;
        int totalScore = toDisplayScore(averagePronunciationScore);
        int pronunciation = toDisplayScore(turnScores.stream()
                .mapToDouble(PronunciationScore::accuracy)
                .average()
                .orElse(averagePronunciationScore));
        int fluency = toDisplayScore(turnScores.stream()
                .mapToDouble(PronunciationScore::fluency)
                .average()
                .orElse(averagePronunciationScore));
        int integrity = toDisplayScore(turnScores.stream()
                .mapToDouble(PronunciationScore::integrity)
                .average()
                .orElse(averagePronunciationScore));
        String speed = formatSpeed(turnScores.stream()
                .mapToDouble(PronunciationScore::speed)
                .average()
                .orElse(0));

        return new SpeakingFeedbackResponse(
                totalScore,
                pronunciation,
                fluency,
                integrity,
                speed,
                issueSentences,
                suggestions,
                session.getScenario().getTitle(),
                session.getCurrentTurn(),
                averagePronunciationScore,
                turns,
                "Keep practicing! Focus on clarity and natural pacing."
        );
    }

    private PronunciationScore readPronunciationScore(SpeakingMessage message) {
        if (message.getPronunciationDetail() != null && !message.getPronunciationDetail().isBlank()) {
            try {
                return objectMapper.readValue(message.getPronunciationDetail(), PronunciationScore.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse stored pronunciation detail for speaking message {}.", message.getId(), e);
            }
        }
        double total = message.getPronunciationScore() != null ? message.getPronunciationScore() : 0;
        return new PronunciationScore(total, total, total, total, 0);
    }

    private PronunciationScore evaluateMissingPronunciation(SpeakingMessage message) {
        if (message.getAudioUrl() == null || message.getAudioUrl().isBlank()
                || message.getTranscribedText() == null || message.getTranscribedText().isBlank()) {
            return null;
        }
        if (!EnglishSpeechText.isEligibleForPronunciationEvaluation(message.getTranscribedText())) {
            return null;
        }
        return pronunciationEvaluationService
                .evaluateUserMessage(
                        message.getId(),
                        audioStorageService.load(message.getAudioUrl()),
                        message.getTranscribedText()
                )
                .orElse(null);
    }

    private int toDisplayScore(double value) {
        return (int) Math.round(value);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatSpeed(double value) {
        if (value <= 0) {
            return "0 WPM";
        }
        return toDisplayScore(value) + " WPM";
    }

    private String normalizeSelectedTopic(String selectedTopic) {
        if (selectedTopic == null || selectedTopic.isBlank()) {
            return null;
        }
        return selectedTopic.trim();
    }

    private SpeakingSessionResponse toSessionResponse(SpeakingSession session) {
        List<SpeakingMessageResponse> messages = messageRepository
                .findBySessionIdOrderByTurnIndexAscCreatedAtAsc(session.getId())
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
            throw new org.springframework.security.access.AccessDeniedException(
                    "This speaking session belongs to another user.");
        }
        return session;
    }
}
