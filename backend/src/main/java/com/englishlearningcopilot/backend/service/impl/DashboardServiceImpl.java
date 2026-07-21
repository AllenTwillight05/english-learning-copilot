package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.DashboardCommunityLearningTrendsResponse;
import com.englishlearningcopilot.backend.dto.DashboardGrammarTrendResponse;
import com.englishlearningcopilot.backend.dto.DashboardSpeakingTrendResponse;
import com.englishlearningcopilot.backend.dto.DashboardStudyPlanResponse;
import com.englishlearningcopilot.backend.dto.DashboardWeeklyOverviewResponse;
import com.englishlearningcopilot.backend.dto.DailyLearningStatusResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.dto.VocabularyLeaderboardItemResponse;
import com.englishlearningcopilot.backend.entity.Vocabulary;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.GrammarLeaderboardProjection;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final UserGrammarbookRepository userGrammarbookRepository;
    private final UserWordbookRepository userWordbookRepository;
    private final VocabularyRepository vocabularyRepository;
    private final LearningPlanService learningPlanService;
    private final UserRepository userRepository;
    private final UserDailyLearningProgressRepository userDailyLearningProgressRepository;
    private final UserDailyPracticeLogRepository userDailyPracticeLogRepository;

    public DashboardServiceImpl(
            SpeakingSessionRepository speakingSessionRepository,
            UserGrammarbookRepository userGrammarbookRepository,
            UserWordbookRepository userWordbookRepository,
            VocabularyRepository vocabularyRepository,
            LearningPlanService learningPlanService,
            UserRepository userRepository,
            UserDailyLearningProgressRepository userDailyLearningProgressRepository,
            UserDailyPracticeLogRepository userDailyPracticeLogRepository
    ) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.userGrammarbookRepository = userGrammarbookRepository;
        this.userWordbookRepository = userWordbookRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.learningPlanService = learningPlanService;
        this.userRepository = userRepository;
        this.userDailyLearningProgressRepository = userDailyLearningProgressRepository;
        this.userDailyPracticeLogRepository = userDailyPracticeLogRepository;
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

        long learningDays = userDailyLearningProgressRepository.countLearningDaysInRange(
                user.getId(),
                weekStart,
                weekEnd
        );
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

        return new DashboardWeeklyOverviewResponse(
                "148 min",
                "92%",
                learningDays + " 天",
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

    private AppUser getCurrentUser(String username) {
        if (username == null) {
            throw new BadCredentialsException("Authentication is required.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
    }
}
