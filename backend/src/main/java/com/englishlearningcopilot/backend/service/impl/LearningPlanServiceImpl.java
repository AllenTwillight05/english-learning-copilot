package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.DailyLearningStatusResponse;
import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.LearningPlanRequest;
import com.englishlearningcopilot.backend.dto.LearningPlanResponse;
import com.englishlearningcopilot.backend.dto.ProfileSnapshotResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserDailyLearningProgress;
import com.englishlearningcopilot.backend.entity.UserDailyPracticeLog;
import com.englishlearningcopilot.backend.entity.UserLearningPlan;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.UserDailyLearningProgressRepository;
import com.englishlearningcopilot.backend.repository.UserDailyPracticeLogRepository;
import com.englishlearningcopilot.backend.repository.UserLearningPlanRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.service.LearningPlanService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningPlanServiceImpl implements LearningPlanService {

    private static final String PRACTICE_TYPE_VOCABULARY = "VOCABULARY";
    private static final String PRACTICE_TYPE_GRAMMAR = "GRAMMAR";

    private final UserRepository userRepository;
    private final UserLearningPlanRepository userLearningPlanRepository;
    private final UserDailyLearningProgressRepository userDailyLearningProgressRepository;
    private final UserDailyPracticeLogRepository userDailyPracticeLogRepository;

    public LearningPlanServiceImpl(
            UserRepository userRepository,
            UserLearningPlanRepository userLearningPlanRepository,
            UserDailyLearningProgressRepository userDailyLearningProgressRepository,
            UserDailyPracticeLogRepository userDailyPracticeLogRepository
    ) {
        this.userRepository = userRepository;
        this.userLearningPlanRepository = userLearningPlanRepository;
        this.userDailyLearningProgressRepository = userDailyLearningProgressRepository;
        this.userDailyPracticeLogRepository = userDailyPracticeLogRepository;
    }

    @Override
    @Transactional
    public LearningPlanResponse getLearningPlan(String username) {
        AppUser user = getCurrentUser(username);
        UserLearningPlan plan = getOrCreatePlan(user.getId());
        return toPlanResponse(plan);
    }

    @Override
    @Transactional
    public LearningPlanResponse updateLearningPlan(String username, LearningPlanRequest request) {
        AppUser user = getCurrentUser(username);
        UserLearningPlan plan = getOrCreatePlan(user.getId());
        plan.setDailyVocabularyGoal(request.dailyVocabularyGoal());
        plan.setDailyGrammarGoal(request.dailyGrammarGoal());
        plan.setEnabled(true);
        UserLearningPlan savedPlan = userLearningPlanRepository.save(plan);

        UserDailyLearningProgress todayProgress = getOrCreateTodayProgress(user.getId(), savedPlan);
        todayProgress.setVocabularyGoal(savedPlan.getDailyVocabularyGoal());
        todayProgress.setGrammarGoal(savedPlan.getDailyGrammarGoal());
        refreshCompletion(todayProgress);
        userDailyLearningProgressRepository.save(todayProgress);

        return toPlanResponse(savedPlan);
    }

    @Override
    @Transactional
    public DailyLearningStatusResponse getDailyStatus(String username) {
        AppUser user = getCurrentUser(username);
        return toDailyStatus(user.getId());
    }

    @Override
    @Transactional
    public DailyPracticeProgressResponse getVocabularyProgress(String username) {
        AppUser user = getCurrentUser(username);
        return toDailyStatus(user.getId()).vocabulary();
    }

    @Override
    @Transactional
    public DailyPracticeProgressResponse getGrammarProgress(String username) {
        AppUser user = getCurrentUser(username);
        return toDailyStatus(user.getId()).grammar();
    }

    @Override
    @Transactional
    public ProfileSnapshotResponse getProfileSnapshot(String username) {
        AppUser user = getCurrentUser(username);
        DailyLearningStatusResponse status = toDailyStatus(user.getId());

        List<ProfileSnapshotResponse.PlanItem> items = List.of(
                new ProfileSnapshotResponse.PlanItem(
                        "vocabulary",
                        "今日",
                        "词汇练习",
                        "已完成 " + status.vocabulary().completed() + " / " + status.vocabulary().total()
                                + "，待练 " + status.vocabulary().remaining(),
                        status.vocabulary().done()
                ),
                new ProfileSnapshotResponse.PlanItem(
                        "grammar",
                        "今日",
                        "语法练习",
                        "已完成 " + status.grammar().completed() + " / " + status.grammar().total()
                                + "，待练 " + status.grammar().remaining(),
                        status.grammar().done()
                )
        );

        List<ProfileSnapshotResponse.ProgressMetric> progress = List.of(
                new ProfileSnapshotResponse.ProgressMetric(
                        "vocabulary",
                        "Vocabulary",
                        toPercent(status.vocabulary()),
                        "teal"
                ),
                new ProfileSnapshotResponse.ProgressMetric(
                        "grammar",
                        "Grammar",
                        toPercent(status.grammar()),
                        "gold"
                )
        );

        ProfileSnapshotResponse.FeedbackSummary feedback = new ProfileSnapshotResponse.FeedbackSummary(
                "Ready",
                "Review",
                List.of(
                        new ProfileSnapshotResponse.FeedbackMetric(
                                "vocabulary",
                                "Vocabulary remaining",
                                String.valueOf(status.vocabulary().remaining())
                        ),
                        new ProfileSnapshotResponse.FeedbackMetric(
                                "grammar",
                                "Grammar remaining",
                                String.valueOf(status.grammar().remaining())
                        )
                ),
                List.of("Keep today's vocabulary and grammar practice moving from the plan.")
        );

        ProfileSnapshotResponse.ProfileDailyPlan dailyPlan = new ProfileSnapshotResponse.ProfileDailyPlan(
                true,
                status.allDone() ? "今日已完成" : "进行中",
                status.vocabulary().total(),
                status.grammar().total(),
                status.vocabulary(),
                status.grammar(),
                status.allDone(),
                items,
                progress
        );

        return new ProfileSnapshotResponse(
                displayName(user),
                "B1 -> B2",
                status.streakDays() + " 天",
                feedback,
                dailyPlan
        );
    }

    @Override
    @Transactional
    public void recordVocabularyPractice(Long userId, Long vocabularyId) {
        recordPractice(userId, PRACTICE_TYPE_VOCABULARY, String.valueOf(vocabularyId));
    }

    @Override
    @Transactional
    public void recordGrammarPractice(Long userId, Integer grammarQuestionId) {
        recordPractice(userId, PRACTICE_TYPE_GRAMMAR, String.valueOf(grammarQuestionId));
    }

    private void recordPractice(Long userId, String practiceType, String itemId) {
        LocalDate today = today();

        if (userDailyPracticeLogRepository.existsByUserIdAndPlanDateAndPracticeTypeAndItemId(
                userId,
                today,
                practiceType,
                itemId
        )) {
            return;
        }

        UserDailyPracticeLog log = new UserDailyPracticeLog();
        log.setUserId(userId);
        log.setPlanDate(today);
        log.setPracticeType(practiceType);
        log.setItemId(itemId);

        userDailyPracticeLogRepository.save(log);

        UserLearningPlan plan = getOrCreatePlan(userId);
        UserDailyLearningProgress progress = getOrCreateTodayProgress(userId, plan);

        if (PRACTICE_TYPE_VOCABULARY.equals(practiceType)) {
            progress.setVocabularyCompleted(progress.getVocabularyCompleted() + 1);
        } else if (PRACTICE_TYPE_GRAMMAR.equals(practiceType)) {
            progress.setGrammarCompleted(progress.getGrammarCompleted() + 1);
        }

        refreshCompletion(progress);
        userDailyLearningProgressRepository.save(progress);
    }

    private DailyLearningStatusResponse toDailyStatus(Long userId) {
        UserLearningPlan plan = getOrCreatePlan(userId);
        UserDailyLearningProgress progress = getOrCreateTodayProgress(userId, plan);
        refreshCompletion(progress);
        userDailyLearningProgressRepository.save(progress);

        DailyPracticeProgressResponse vocabulary = toVocabularyProgress(progress);
        DailyPracticeProgressResponse grammar = toGrammarProgress(progress);
        boolean allDone = vocabulary.done() && grammar.done();

        return new DailyLearningStatusResponse(today(), vocabulary, grammar, allDone, calculateStreakDays(userId));
    }

    private UserLearningPlan getOrCreatePlan(Long userId) {
        return userLearningPlanRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserLearningPlan plan = new UserLearningPlan();
                    plan.setUserId(userId);
                    return userLearningPlanRepository.save(plan);
                });
    }

    private UserDailyLearningProgress getOrCreateTodayProgress(Long userId, UserLearningPlan plan) {
        return userDailyLearningProgressRepository.findByUserIdAndPlanDate(userId, today())
                .orElseGet(() -> {
                    UserDailyLearningProgress progress = new UserDailyLearningProgress();
                    progress.setUserId(userId);
                    progress.setPlanDate(today());
                    progress.setVocabularyGoal(plan.getDailyVocabularyGoal());
                    progress.setGrammarGoal(plan.getDailyGrammarGoal());
                    return userDailyLearningProgressRepository.save(progress);
                });
    }

    private void refreshCompletion(UserDailyLearningProgress progress) {
        boolean wasCompleted = progress.isCompleted();
        boolean isCompleted = progress.getVocabularyCompleted() >= progress.getVocabularyGoal()
                && progress.getGrammarCompleted() >= progress.getGrammarGoal();
        progress.setCompleted(isCompleted);

        if (!wasCompleted && isCompleted) {
            progress.setCompletedAt(Instant.now());
        }

        if (!isCompleted) {
            progress.setCompletedAt(null);
        }
    }

    private DailyPracticeProgressResponse toVocabularyProgress(UserDailyLearningProgress progress) {
        return toPracticeProgress(progress.getVocabularyCompleted(), progress.getVocabularyGoal());
    }

    private DailyPracticeProgressResponse toGrammarProgress(UserDailyLearningProgress progress) {
        return toPracticeProgress(progress.getGrammarCompleted(), progress.getGrammarGoal());
    }

    private DailyPracticeProgressResponse toPracticeProgress(int completed, int total) {
        int remaining = Math.max(total - completed, 0);
        return new DailyPracticeProgressResponse(completed, total, remaining, completed >= total);
    }

    private LearningPlanResponse toPlanResponse(UserLearningPlan plan) {
        return new LearningPlanResponse(
                plan.getDailyVocabularyGoal(),
                plan.getDailyGrammarGoal(),
                plan.isEnabled()
        );
    }

    private int calculateStreakDays(Long userId) {
        List<UserDailyLearningProgress> completedDays =
                userDailyLearningProgressRepository.findByUserIdAndCompletedTrueOrderByPlanDateDesc(userId);
        LocalDate expectedDate = toDailyStatusStartDate(userId);
        int streak = 0;

        for (UserDailyLearningProgress progress : completedDays) {
            if (progress.getPlanDate().equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (progress.getPlanDate().isBefore(expectedDate)) {
                break;
            }
        }

        return streak;
    }

    private LocalDate toDailyStatusStartDate(Long userId) {
        return userDailyLearningProgressRepository.findByUserIdAndPlanDate(userId, today())
                .filter(UserDailyLearningProgress::isCompleted)
                .map(UserDailyLearningProgress::getPlanDate)
                .orElse(today().minusDays(1));
    }

    private int toPercent(DailyPracticeProgressResponse progress) {
        if (progress.total() <= 0) {
            return 100;
        }

        return Math.min(100, Math.round((progress.completed() * 100f) / progress.total()));
    }

    private AppUser getCurrentUser(String username) {
        if (username == null) {
            throw new BadCredentialsException("Authentication is required.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
    }

    private String displayName(AppUser user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername()
                : user.getDisplayName();
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.systemDefault());
    }
}
