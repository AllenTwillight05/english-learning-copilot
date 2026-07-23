package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.DashboardCommunityLearningTrendsResponse;
import com.englishlearningcopilot.backend.dto.DashboardGrammarTrendResponse;
import com.englishlearningcopilot.backend.dto.DashboardSpeakingTrendResponse;
import com.englishlearningcopilot.backend.dto.DashboardStudyPlanResponse;
import com.englishlearningcopilot.backend.dto.DashboardWeeklyOverviewResponse;
import com.englishlearningcopilot.backend.dto.DailyLearningStatusResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.dto.VocabularyLeaderboardItemResponse;
import com.englishlearningcopilot.backend.entity.Vocabulary;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.GrammarLeaderboardProjection;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.repository.SpeakingLeaderboardProjection;
import com.englishlearningcopilot.backend.repository.SpeakingSessionRepository;
import com.englishlearningcopilot.backend.repository.UserDailyLearningProgressRepository;
import com.englishlearningcopilot.backend.repository.UserDailyPracticeLogRepository;
import com.englishlearningcopilot.backend.repository.UserGrammarbookRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordbookRepository;
import com.englishlearningcopilot.backend.repository.VocabularyLeaderboardProjection;
import com.englishlearningcopilot.backend.repository.VocabularyRepository;
import com.englishlearningcopilot.backend.service.DashboardService;
import com.englishlearningcopilot.backend.service.LearningPlanService;
import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int VOCABULARY_LEADERBOARD_LIMIT = 10;
    private static final int GRAMMAR_LEADERBOARD_LIMIT = 10;
    private static final int SPEAKING_LEADERBOARD_LIMIT = 10;
    private static final String PRACTICE_TYPE_VOCABULARY = "VOCABULARY";
    private static final String PRACTICE_TYPE_GRAMMAR = "GRAMMAR";

    private final SpeakingSessionRepository speakingSessionRepository;
    private final SpeakingMessageRepository speakingMessageRepository;
    private final UserGrammarbookRepository userGrammarbookRepository;
    private final UserWordbookRepository userWordbookRepository;
    private final VocabularyRepository vocabularyRepository;
    private final LearningPlanService learningPlanService;
    private final UserRepository userRepository;
    private final UserDailyLearningProgressRepository userDailyLearningProgressRepository;
    private final UserDailyPracticeLogRepository userDailyPracticeLogRepository;
    private final ObjectMapper objectMapper;

    public DashboardServiceImpl(
            SpeakingSessionRepository speakingSessionRepository,
            SpeakingMessageRepository speakingMessageRepository,
            UserGrammarbookRepository userGrammarbookRepository,
            UserWordbookRepository userWordbookRepository,
            VocabularyRepository vocabularyRepository,
            LearningPlanService learningPlanService,
            UserRepository userRepository,
            UserDailyLearningProgressRepository userDailyLearningProgressRepository,
            UserDailyPracticeLogRepository userDailyPracticeLogRepository,
            ObjectMapper objectMapper
    ) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.speakingMessageRepository = speakingMessageRepository;
        this.userGrammarbookRepository = userGrammarbookRepository;
        this.userWordbookRepository = userWordbookRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.learningPlanService = learningPlanService;
        this.userRepository = userRepository;
        this.userDailyLearningProgressRepository = userDailyLearningProgressRepository;
        this.userDailyPracticeLogRepository = userDailyPracticeLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardCommunityLearningTrendsResponse getCommunityLearningTrends() {
        return new DashboardCommunityLearningTrendsResponse(
                getSpeakingLeaderboard(),
                getVocabularyLeaderboard(),
                getGrammarLeaderboard()
        );
    }

    @Override
    @Transactional
    public DashboardStudyPlanResponse getStudyPlan(String username) {
        DailyLearningStatusResponse status = learningPlanService.getDailyStatus(username);
        return new DashboardStudyPlanResponse(
                status.vocabulary(),
                status.grammar(),
                status.streakDays(),
                status.allDone()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardWeeklyOverviewResponse getWeeklyOverview(String username) {
        AppUser user = getCurrentUser(username);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        ZoneId zone = ZoneId.systemDefault();
        Instant weekStartInstant = weekStart.atStartOfDay(zone).toInstant();
        Instant weekEndExclusiveInstant = weekEnd.plusDays(1).atStartOfDay(zone).toInstant();

        Set<LocalDate> learningDates = new HashSet<>(userDailyLearningProgressRepository.findLearningDatesInRange(
                user.getId(),
                weekStart,
                weekEnd
        ));
        long vocabularyLearned = userDailyPracticeLogRepository.countByUserIdAndPlanDateBetweenAndPracticeType(
                user.getId(),
                weekStart,
                weekEnd,
                PRACTICE_TYPE_VOCABULARY
        );
        long grammarPracticed = userDailyPracticeLogRepository.countByUserIdAndPlanDateBetweenAndPracticeType(
                user.getId(),
                weekStart,
                weekEnd,
                PRACTICE_TYPE_GRAMMAR
        );
        List<SpeakingMessage> weeklyUserMessages = speakingMessageRepository
                .findBySessionUserIdAndSenderAndCreatedAtBetween(
                        user.getId(),
                        SpeakingMessageSender.USER,
                        weekStartInstant,
                        weekEndExclusiveInstant
                );
        weeklyUserMessages.stream()
                .map(SpeakingMessage::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .map(createdAt -> LocalDate.ofInstant(createdAt, zone))
                .forEach(learningDates::add);
        long speakingDurationMs = weeklyUserMessages.stream()
                .map(SpeakingMessage::getDurationMs)
                .filter(durationMs -> durationMs != null && durationMs > 0)
                .mapToLong(Long::longValue)
                .sum();
        double averageAccuracy = weeklyUserMessages.stream()
                .filter(message -> message.getPronunciationScore() != null)
                .mapToDouble(this::readAccuracy)
                .average()
                .orElse(Double.NaN);

        return new DashboardWeeklyOverviewResponse(
                formatDurationMinutes(speakingDurationMs),
                formatAccuracy(averageAccuracy),
                learningDates.size() + " 天",
                vocabularyLearned + " 词",
                grammarPracticed + " 题"
        );
    }

    private List<DashboardSpeakingTrendResponse> getSpeakingLeaderboard() {
        List<SpeakingLeaderboardProjection> leaderboardRows = speakingSessionRepository.findSpeakingLeaderboard(
                PageRequest.of(0, SPEAKING_LEADERBOARD_LIMIT)
        );

        List<DashboardSpeakingTrendResponse> leaderboard = new ArrayList<>();
        for (SpeakingLeaderboardProjection row : leaderboardRows) {
            leaderboard.add(new DashboardSpeakingTrendResponse(
                    leaderboard.size() + 1,
                    row.getScenarioId(),
                    row.getTopic(),
                    nullToEmpty(row.getDescription()),
                    row.getLearnerCount()
            ));
        }

        return leaderboard;
    }

    private List<VocabularyLeaderboardItemResponse> getVocabularyLeaderboard() {
        List<VocabularyLeaderboardProjection> leaderboardRows = userWordbookRepository.findVocabularyLeaderboard(
                PageRequest.of(0, VOCABULARY_LEADERBOARD_LIMIT)
        );
        List<Long> vocabularyIds = leaderboardRows.stream()
                .map(VocabularyLeaderboardProjection::getVocabularyId)
                .toList();
        Map<Long, Vocabulary> vocabularyById = vocabularyRepository.findAllById(vocabularyIds).stream()
                .collect(Collectors.toMap(Vocabulary::getId, Function.identity()));

        List<VocabularyLeaderboardItemResponse> leaderboard = new ArrayList<>();
        for (VocabularyLeaderboardProjection row : leaderboardRows) {
            Vocabulary vocabulary = vocabularyById.get(row.getVocabularyId());

            if (vocabulary == null) {
                continue;
            }

            leaderboard.add(new VocabularyLeaderboardItemResponse(
                    leaderboard.size() + 1,
                    vocabulary.getId(),
                    vocabulary.getWord(),
                    nullToEmpty(vocabulary.getBriefTranslation()),
                    row.getLearnerCount()
            ));
        }

        return leaderboard;
    }

    private List<DashboardGrammarTrendResponse> getGrammarLeaderboard() {
        List<GrammarLeaderboardProjection> leaderboardRows = userGrammarbookRepository.findGrammarLeaderboard(
                PageRequest.of(0, GRAMMAR_LEADERBOARD_LIMIT)
        );

        List<DashboardGrammarTrendResponse> leaderboard = new ArrayList<>();
        for (GrammarLeaderboardProjection row : leaderboardRows) {
            leaderboard.add(new DashboardGrammarTrendResponse(
                    leaderboard.size() + 1,
                    row.getGrammarCategory(),
                    row.getLearnerCount()
            ));
        }

        return leaderboard;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double readAccuracy(SpeakingMessage message) {
        if (message.getPronunciationDetail() != null && !message.getPronunciationDetail().isBlank()) {
            try {
                return objectMapper.readValue(message.getPronunciationDetail(), PronunciationScore.class).accuracy();
            } catch (JsonProcessingException ignored) {
                // Fall back to stored total score below.
            }
        }
        return message.getPronunciationScore() != null ? message.getPronunciationScore() : Double.NaN;
    }

    private String formatDurationMinutes(long durationMs) {
        if (durationMs <= 0) {
            return "0 min";
        }
        long minutes = Math.round(durationMs / 60000.0);
        return Math.max(minutes, 1) + " min";
    }

    private String formatAccuracy(double accuracy) {
        if (Double.isNaN(accuracy)) {
            return "-";
        }
        return Math.round(accuracy) + "%";
    }

    private AppUser getCurrentUser(String username) {
        if (username == null) {
            throw new BadCredentialsException("Authentication is required.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
    }
}
