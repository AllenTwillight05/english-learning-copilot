package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteRequest;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteResponse;
import com.englishlearningcopilot.backend.dto.GrammarNotebookQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarOverviewResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeResultRequest;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarRatingRequest;
import com.englishlearningcopilot.backend.dto.GrammarTopicResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.GrammarQuestion;
import com.englishlearningcopilot.backend.entity.UserGrammarbook;
import com.englishlearningcopilot.backend.entity.UserWordProgress;
import com.englishlearningcopilot.backend.fsrs.FSRS;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.GrammarQuestionRepository;
import com.englishlearningcopilot.backend.repository.UserGrammarbookRepository;
import com.englishlearningcopilot.backend.repository.UserWordProgressRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.service.GrammarService;
import com.englishlearningcopilot.backend.service.LearningPlanService;
import com.englishlearningcopilot.backend.service.ReviewService;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
public class GrammarServiceImpl implements GrammarService {

    private static final String QUESTION_TYPE_GRAMMAR = "grammar";
    private static final int REVIEW_STATE = 1;
    private static final double FSRS_DECAY = -FSRS.defaultParams()[20];
    private static final double FSRS_FACTOR = Math.pow(0.9, 1.0 / FSRS_DECAY) - 1;

    private final GrammarQuestionRepository grammarQuestionRepository;
    private final UserGrammarbookRepository userGrammarbookRepository;
    private final UserWordProgressRepository userWordProgressRepository;
    private final UserRepository userRepository;
    private final LearningPlanService learningPlanService;
    private final ReviewService reviewService;

    public GrammarServiceImpl(
            GrammarQuestionRepository grammarQuestionRepository,
            UserGrammarbookRepository userGrammarbookRepository,
            UserWordProgressRepository userWordProgressRepository,
            UserRepository userRepository,
            ReviewService reviewService,
            LearningPlanService learningPlanService
    ) {
        this.grammarQuestionRepository = grammarQuestionRepository;
        this.userGrammarbookRepository = userGrammarbookRepository;
        this.userWordProgressRepository = userWordProgressRepository;
        this.userRepository = userRepository;
        this.learningPlanService = learningPlanService;
        this.reviewService = reviewService;
    }

    @Override
    @Transactional(readOnly = true)
    public GrammarOverviewResponse getOverview(String username) {
        List<GrammarQuestion> questions = grammarQuestionRepository.findAll();
        AppUser user = getCurrentUserOrNull(username);
        List<UserWordProgress> progressRows = user == null
                ? List.of()
                : getGrammarProgressRows(user.getId());
        int total = questions.size();
        List<UserWordProgress> reviewCards = progressRows.stream()
                .filter(this::isReviewCard)
                .toList();
        int mastered = reviewCards.size();
        int due = countDue(reviewCards);
        int masteryRate = reviewCards.isEmpty()
                ? percent(countCompleted(progressRows), total)
                : averageRetentionRate(reviewCards, Instant.now());

        return new GrammarOverviewResponse(
                masteryRate,
                List.of(
                        new GrammarOverviewResponse.Stat(mastered + " 题", "已掌握"),
                        new GrammarOverviewResponse.Stat(due + " 题", "今日待复习"),
                        new GrammarOverviewResponse.Stat("0 题", "今日待练")
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrammarTopicResponse> getTopics(String username) {
        AppUser user = getCurrentUserOrNull(username);
        Set<Integer> completedQuestionIds = user == null
                ? Set.of()
                : getGrammarProgressRows(user.getId()).stream()
                        .filter(this::isCompleted)
                        .map(UserWordProgress::getQuestionId)
                        .map(this::parseQuestionIdOrNull)
                        .filter(questionId -> questionId != null)
                        .collect(Collectors.toSet());

        Map<String, List<GrammarQuestion>> questionsByCategory = grammarQuestionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        GrammarQuestion::getGrammarCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return questionsByCategory.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toTopicResponse(entry.getKey(), entry.getValue(), completedQuestionIds))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrammarPracticeQuestionResponse> getPracticeQuestions(String username, String category) {
        AppUser user = getCurrentUser(username);

        return grammarQuestionRepository.findRandomUnpracticedQuestionsByCategory(
                        user.getId(),
                        category,
                        PageRequest.of(0, 3)
                ).stream()
                .map(GrammarPracticeQuestionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public DailyPracticeProgressResponse getProgress(String username) {
        return learningPlanService.getGrammarProgress(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrammarPracticeQuestionResponse> getReviewQuestions(String username) {
        AppUser user = getCurrentUser(username);
        return reviewService.getDueGrammar(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrammarNotebookQuestionResponse> getNotebookQuestions(String username) {
        AppUser user = getCurrentUser(username);
        List<UserGrammarbook> grammarbookRows = userGrammarbookRepository.findNotebookRowsByUserId(user.getId());
        List<Integer> questionIds = grammarbookRows.stream()
                .map(UserGrammarbook::getGrammarQuestionId)
                .toList();
        Map<Integer, GrammarQuestion> questionsById = grammarQuestionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(GrammarQuestion::getId, Function.identity()));

        return grammarbookRows.stream()
                .map(grammarbook -> {
                    GrammarQuestion question = questionsById.get(grammarbook.getGrammarQuestionId());
                    return question == null
                            ? null
                            : GrammarNotebookQuestionResponse.from(
                                    question,
                                    grammarbook.isIncorrect(),
                                    grammarbook.isFavorited()
                            );
                })
                .filter(response -> response != null)
                .toList();
    }

    @Override
    @Transactional
    public void submitPracticeResult(String username, GrammarPracticeResultRequest request) {
        AppUser user = getCurrentUser(username);
        Integer questionId = request.grammarQuestionId();
        validateQuestionExists(questionId);

        UserGrammarbook grammarbook = getOrCreateGrammarbook(user.getId(), questionId);
        grammarbook.setIncorrect(request.incorrect());
        userGrammarbookRepository.save(grammarbook);
        learningPlanService.recordGrammarPractice(user.getId(), questionId);
    }

    @Override
    @Transactional
    public void submitRating(String username, GrammarRatingRequest request) {
        AppUser user = getCurrentUser(username);
        validateQuestionExists(request.grammarQuestionId());
        reviewService.submitGrammarRating(user.getId(), request.grammarQuestionId(), request.score());
    }

    @Override
    @Transactional
    public GrammarFavoriteResponse toggleFavorite(String username, GrammarFavoriteRequest request) {
        AppUser user = getCurrentUser(username);
        Integer questionId = request.grammarQuestionId();
        validateQuestionExists(questionId);

        UserGrammarbook grammarbook = getOrCreateGrammarbook(user.getId(), questionId);
        grammarbook.setFavorited(!grammarbook.isFavorited());
        UserGrammarbook savedGrammarbook = userGrammarbookRepository.save(grammarbook);

        return new GrammarFavoriteResponse(questionId, savedGrammarbook.isFavorited());
    }

    private List<UserWordProgress> getGrammarProgressRows(Long userId) {
        return userWordProgressRepository.findByUserIdAndQuestionType(userId, QUESTION_TYPE_GRAMMAR);
    }

    private int countCompleted(List<UserWordProgress> progressRows) {
        return (int) progressRows.stream()
                .filter(this::isCompleted)
                .count();
    }

    private int countDue(List<UserWordProgress> progressRows) {
        Instant now = Instant.now();
        return (int) progressRows.stream()
                .filter(this::isCompleted)
                .filter(progress -> progress.getDue() != null && !progress.getDue().isAfter(now))
                .count();
    }

    private boolean isCompleted(UserWordProgress progress) {
        return nullToZero(progress.getReps()) > 0 || progress.getLastReview() != null;
    }

    private boolean isReviewCard(UserWordProgress progress) {
        return progress.getState() != null && progress.getState() == REVIEW_STATE;
    }

    private GrammarTopicResponse toTopicResponse(
            String category,
            List<GrammarQuestion> questions,
            Set<Integer> completedQuestionIds
    ) {
        int completed = (int) questions.stream()
                .map(GrammarQuestion::getId)
                .filter(completedQuestionIds::contains)
                .count();

        List<String> examples = questions.stream()
                .sorted(Comparator.comparing(GrammarQuestion::getId))
                .map(GrammarQuestion::getQuestionText)
                .filter(text -> text != null && !text.isBlank())
                .limit(2)
                .toList();

        return new GrammarTopicResponse(
                category,
                category,
                topicSummary(category),
                examples,
                percent(completed, questions.size()),
                questions.size() + " 题"
        );
    }

    private String topicSummary(String category) {
        return switch (category) {
            case "从句" -> "覆盖定语从句、名词性从句和状语从句，训练句子结构判断与连接词选择。";
            case "时态与语态" -> "训练时态、被动语态和时间线判断，提升复杂句中的谓语选择准确率。";
            case "词汇与逻辑" -> "通过上下文选择准确词义、逻辑连接和表达关系，适合综合语法题。";
            case "非谓语动词" -> "聚焦不定式、动名词和分词结构，训练主被动关系和句法功能判断。";
            case "介词与固定搭配" -> "整理高频介词、动词短语和固定搭配，减少介词误用。";
            case "代词与限定词" -> "训练代词指代、限定词搭配和数量关系，提升句子内部一致性判断。";
            case "主谓一致" -> "识别真正主语与谓语形式，处理插入语、并列结构和数量短语。";
            case "情态动词与虚拟语气" -> "训练情态动词语义、虚拟条件和委婉表达中的谓语形式。";
            default -> "围绕该语法分类进行专项选择题训练，巩固规则识别和语境判断。";
        };
    }

    private Integer parseQuestionIdOrNull(String questionId) {
        try {
            return Integer.valueOf(questionId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int percent(int value, int total) {
        return total > 0 ? Math.round((value * 100.0f) / total) : 0;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int averageRetentionRate(List<UserWordProgress> reviewCards, Instant now) {
        if (reviewCards.isEmpty()) {
            return 0;
        }

        double average = reviewCards.stream()
                .mapToDouble(progress -> retention(progress, now))
                .average()
                .orElse(0);
        return (int) Math.round(average * 100);
    }

    private double retention(UserWordProgress progress, Instant now) {
        Instant lastReview = progress.getLastReview() == null ? progress.getUpdatedAt() : progress.getLastReview();
        double elapsedDays = lastReview == null
                ? 0
                : Math.max(0, Duration.between(lastReview, now).toDays());
        double stability = progress.getStability() == null || progress.getStability() <= 0
                ? 0.1
                : progress.getStability();
        return Math.pow(1 + FSRS_FACTOR * elapsedDays / stability, FSRS_DECAY);
    }

    private UserGrammarbook getOrCreateGrammarbook(Long userId, Integer questionId) {
        return userGrammarbookRepository.findByUserIdAndGrammarQuestionId(userId, questionId)
                .orElseGet(() -> {
                    UserGrammarbook grammarbook = new UserGrammarbook();
                    grammarbook.setUserId(userId);
                    grammarbook.setGrammarQuestionId(questionId);
                    return grammarbook;
                });
    }

    private void validateQuestionExists(Integer questionId) {
        if (!grammarQuestionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Grammar question was not found.");
        }
    }

    private AppUser getCurrentUser(String username) {
        if (username == null) {
            throw new BadCredentialsException("Authentication is required.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
    }

    private AppUser getCurrentUserOrNull(String username) {
        if (username == null) {
            return null;
        }

        return userRepository.findByUsername(username).orElse(null);
    }
}
