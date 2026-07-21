package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteRequest;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteResponse;
import com.englishlearningcopilot.backend.dto.VocabularyPracticeWordResponse;
import com.englishlearningcopilot.backend.dto.VocabularyRatingRequest;
import com.englishlearningcopilot.backend.dto.VocabularyWordbookWordResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserWordProgress;
import com.englishlearningcopilot.backend.entity.UserWordbook;
import com.englishlearningcopilot.backend.entity.Vocabulary;
import com.englishlearningcopilot.backend.fsrs.FSRS;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordProgressRepository;
import com.englishlearningcopilot.backend.repository.UserWordbookRepository;
import com.englishlearningcopilot.backend.repository.VocabularyRepository;
import com.englishlearningcopilot.backend.service.LearningPlanService;
import com.englishlearningcopilot.backend.service.ReviewService;
import com.englishlearningcopilot.backend.service.VocabularyService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyServiceImpl implements VocabularyService {

    private static final String UNUSED_TAG = "__unused_vocabulary_tag__";
    private static final String QUESTION_TYPE_VOCABULARY = "vocabulary";
    private static final int REVIEW_STATE = 1;
    private static final double FSRS_DECAY = -FSRS.defaultParams()[20];
    private static final double FSRS_FACTOR = Math.pow(0.9, 1.0 / FSRS_DECAY) - 1;

    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final UserWordProgressRepository userWordProgressRepository;
    private final UserWordbookRepository userWordbookRepository;
    private final ReviewService reviewService;
    private final LearningPlanService learningPlanService;

    public VocabularyServiceImpl(
            VocabularyRepository vocabularyRepository,
            UserRepository userRepository,
            UserWordProgressRepository userWordProgressRepository,
            UserWordbookRepository userWordbookRepository,
            ReviewService reviewService,
            LearningPlanService learningPlanService
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.userRepository = userRepository;
        this.userWordProgressRepository = userWordProgressRepository;
        this.userWordbookRepository = userWordbookRepository;
        this.reviewService = reviewService;
        this.learningPlanService = learningPlanService;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMemory(String username) {
        AppUser user = getCurrentUserOrNull(username);
        if (user == null) {
            return memoryResponse(0, 0, 0);
        }

        List<UserWordProgress> reviewCards = userWordProgressRepository
                .findByUserIdAndQuestionType(user.getId(), QUESTION_TYPE_VOCABULARY)
                .stream()
                .filter(this::isReviewCard)
                .toList();

        int mastered = reviewCards.size();
        Instant now = Instant.now();
        int dueCount = (int) reviewCards.stream()
                .filter(progress -> progress.getDue() != null && !progress.getDue().isAfter(now))
                .count();
        int retentionRate = averageRetentionRate(reviewCards, now);

        return memoryResponse(retentionRate, mastered, dueCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyPracticeWordResponse> getPracticeWords(String username, String level) {
        PageRequest limit = PageRequest.of(0, 10);
        String[] tags = getLevelTags(level);

        if (username == null) {
            return vocabularyRepository.findRandomPracticeWordsByTags(
                            tags[0], tags[1], tags[2], tags[3], limit
                    ).stream()
                    .map(VocabularyPracticeWordResponse::from)
                    .toList();
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));

        return vocabularyRepository.findRandomUnlearnedPracticeWordsByTags(
                        user.getId(), tags[0], tags[1], tags[2], tags[3], limit
                ).stream()
                .map(VocabularyPracticeWordResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public DailyPracticeProgressResponse getPracticeProgress(String username) {
        return learningPlanService.getVocabularyProgress(username);
    }

    private String[] getLevelTags(String level) {
        String normalizedLevel = level == null ? "starter" : level.toLowerCase(Locale.ROOT);
        return switch (normalizedLevel) {
            case "starter" -> paddedTags("zk", "gk");
            case "basic" -> paddedTags("cet4");
            case "intermediate" -> paddedTags("cet6");
            case "advanced" -> paddedTags("ky", "toefl", "gre", "ielts");
            default -> throw new IllegalArgumentException("Unsupported vocabulary practice level: " + level);
        };
    }

    private String[] paddedTags(String... tags) {
        String[] padded = {UNUSED_TAG, UNUSED_TAG, UNUSED_TAG, UNUSED_TAG};
        System.arraycopy(tags, 0, padded, 0, tags.length);
        return padded;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VocabularyWordbookWordResponse> getWordbookWords(String username) {
        AppUser user = getCurrentUser(username);
        List<UserWordbook> wordbookRows = userWordbookRepository.findByUserIdOrderByIdDesc(user.getId());
        List<Long> vocabularyIds = wordbookRows.stream()
                .map(UserWordbook::getVocabularyId)
                .toList();
        Map<Long, Vocabulary> vocabularyById = vocabularyRepository.findAllById(vocabularyIds).stream()
                .collect(Collectors.toMap(Vocabulary::getId, Function.identity()));

        return wordbookRows.stream()
                .map(wordbook -> {
                    Vocabulary vocabulary = vocabularyById.get(wordbook.getVocabularyId());
                    return vocabulary == null
                            ? null
                            : VocabularyWordbookWordResponse.from(vocabulary, wordbook.isFavorited());
                })
                .filter(response -> response != null)
                .toList();
    }

    @Override
    @Transactional
    public void submitRating(String username, VocabularyRatingRequest request) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
        Long vocabularyId = request.vocabularyId();

        if (!vocabularyRepository.existsById(vocabularyId)) {
            throw new ResourceNotFoundException("Vocabulary word was not found.");
        }

        if (userWordbookRepository.findByUserIdAndVocabularyId(user.getId(), vocabularyId).isEmpty()) {
            UserWordbook wordbook = new UserWordbook();
            wordbook.setUserId(user.getId());
            wordbook.setVocabularyId(vocabularyId);
            userWordbookRepository.save(wordbook);
        }

        reviewService.submitRating(user.getId(), String.valueOf(vocabularyId), request.score());
        learningPlanService.recordVocabularyPractice(user.getId(), vocabularyId);
    }

    @Override
    @Transactional
    public VocabularyFavoriteResponse toggleFavorite(String username, VocabularyFavoriteRequest request) {
        AppUser user = getCurrentUser(username);
        Long vocabularyId = request.vocabularyId();

        if (!vocabularyRepository.existsById(vocabularyId)) {
            throw new ResourceNotFoundException("Vocabulary word was not found.");
        }

        UserWordbook wordbook = userWordbookRepository.findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .orElseGet(() -> {
                    UserWordbook newWordbook = new UserWordbook();
                    newWordbook.setUserId(user.getId());
                    newWordbook.setVocabularyId(vocabularyId);
                    return newWordbook;
                });

        wordbook.setFavorited(!wordbook.isFavorited());
        UserWordbook savedWordbook = userWordbookRepository.save(wordbook);

        return new VocabularyFavoriteResponse(vocabularyId, savedWordbook.isFavorited());
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

    private Map<String, Object> memoryResponse(int retentionRate, int mastered, int dueCount) {
        return Map.of(
                "retentionRate", retentionRate,
                "stats", List.of(
                        Map.of("value", mastered + " 词", "label", "已掌握"),
                        Map.of("value", dueCount + " 词", "label", "今日待复习"),
                        Map.of("value", "0 词", "label", "今日待练")
                )
        );
    }

    private boolean isReviewCard(UserWordProgress progress) {
        return progress.getState() != null && progress.getState() == REVIEW_STATE;
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
}
