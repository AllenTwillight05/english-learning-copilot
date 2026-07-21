package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.DueVocabularyCard;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserWordProgress;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.fsrs.FSRS;
import com.englishlearningcopilot.backend.fsrs.FSRS.CardState;
import com.englishlearningcopilot.backend.fsrs.FSRS.Rating;
import com.englishlearningcopilot.backend.repository.GrammarQuestionRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordProgressRepository;
import com.englishlearningcopilot.backend.repository.VocabularyRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final String QUESTION_TYPE_VOCABULARY = "vocabulary";
    private static final String QUESTION_TYPE_GRAMMAR = "grammar";

    private final FSRS fsrs = new FSRS(0.9, FSRS.defaultParams());
    private final UserWordProgressRepository userWordProgressRepository;
    private final VocabularyRepository vocabularyRepository;
    private final GrammarQuestionRepository grammarQuestionRepository;
    private final UserRepository userRepository;

    public ReviewService(
            UserWordProgressRepository userWordProgressRepository,
            VocabularyRepository vocabularyRepository,
            GrammarQuestionRepository grammarQuestionRepository,
            UserRepository userRepository
    ) {
        this.userWordProgressRepository = userWordProgressRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.grammarQuestionRepository = grammarQuestionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void submitRating(Long userId, String questionId, int rating) {
        if (!vocabularyRepository.existsById(parseVocabularyId(questionId))) {
            throw new ResourceNotFoundException("Vocabulary word was not found.");
        }

        UserWordProgress progress = userWordProgressRepository
                .findByUserIdAndQuestionIdAndQuestionType(userId, questionId, QUESTION_TYPE_VOCABULARY)
                .orElseGet(() -> newProgress(userId, questionId));

        CardState card = toCardState(progress);
        CardState reviewed = fsrs.review(card, Rating.fromInt(rating));
        applyCardState(progress, reviewed);
        userWordProgressRepository.save(progress);
    }

    @Transactional
    public void submitGrammarRating(Long userId, Integer grammarQuestionId, int rating) {
        if (!grammarQuestionRepository.existsById(grammarQuestionId)) {
            throw new ResourceNotFoundException("Grammar question was not found.");
        }

        String questionId = String.valueOf(grammarQuestionId);
        UserWordProgress progress = userWordProgressRepository
                .findByUserIdAndQuestionIdAndQuestionType(userId, questionId, QUESTION_TYPE_GRAMMAR)
                .orElseGet(() -> newProgress(userId, questionId, QUESTION_TYPE_GRAMMAR));

        CardState card = toCardState(progress);
        CardState reviewed = fsrs.review(card, Rating.fromInt(rating));
        applyCardState(progress, reviewed);
        userWordProgressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public List<DueVocabularyCard> getDueVocabulary(Long userId) {
        return userWordProgressRepository
                .findByUserIdAndQuestionTypeAndDueBeforeOrderByDueAsc(
                        userId,
                        QUESTION_TYPE_VOCABULARY,
                        Instant.now()
                )
                .stream()
                .map(UserWordProgress::getQuestionId)
                .map(this::parseVocabularyId)
                .map(vocabularyRepository::findById)
                .flatMap(optionalVocabulary -> optionalVocabulary.stream())
                .map(DueVocabularyCard::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrammarPracticeQuestionResponse> getDueGrammar(Long userId) {
        return userWordProgressRepository
                .findByUserIdAndQuestionTypeAndDueBeforeOrderByDueAsc(
                        userId,
                        QUESTION_TYPE_GRAMMAR,
                        Instant.now()
                )
                .stream()
                .map(UserWordProgress::getQuestionId)
                .map(this::parseGrammarQuestionId)
                .map(grammarQuestionRepository::findById)
                .flatMap(optionalQuestion -> optionalQuestion.stream())
                .map(GrammarPracticeQuestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Long getUserIdByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user was not found."));
        return user.getId();
    }

    private UserWordProgress newProgress(Long userId, String questionId) {
        return newProgress(userId, questionId, QUESTION_TYPE_VOCABULARY);
    }

    private UserWordProgress newProgress(Long userId, String questionId, String questionType) {
        UserWordProgress progress = new UserWordProgress();
        progress.setUserId(userId);
        progress.setQuestionId(questionId);
        progress.setQuestionType(questionType);
        progress.setDifficulty(2.5);
        progress.setStability(2.5);
        progress.setInterval(0);
        progress.setReps(0);
        progress.setLapses(0);
        progress.setState(0);
        progress.setDue(Instant.now());
        return progress;
    }

    private CardState toCardState(UserWordProgress progress) {
        CardState card = new CardState();
        card.difficulty = nullToDefault(progress.getDifficulty(), 2.5);
        card.stability = nullToDefault(progress.getStability(), 2.5);
        card.interval = nullToDefault(progress.getInterval(), 0);
        card.reps = nullToDefault(progress.getReps(), 0);
        card.lapses = nullToDefault(progress.getLapses(), 0);
        card.state = nullToDefault(progress.getState(), 0) == 0
                ? CardState.State.New
                : CardState.State.Review;
        card.due = progress.getDue() == null ? Instant.now() : progress.getDue();
        card.lastReview = progress.getLastReview() == null ? Instant.now() : progress.getLastReview();
        return card;
    }

    private void applyCardState(UserWordProgress progress, CardState card) {
        progress.setDifficulty(card.difficulty);
        progress.setStability(card.stability);
        progress.setInterval(card.interval);
        progress.setReps(card.reps);
        progress.setLapses(card.lapses);
        progress.setState(card.state == CardState.State.New ? 0 : 1);
        progress.setDue(card.due);
        progress.setLastReview(card.lastReview);
    }

    private Long parseVocabularyId(String questionId) {
        try {
            return Long.parseLong(questionId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid vocabulary questionId: " + questionId);
        }
    }

    private Integer parseGrammarQuestionId(String questionId) {
        try {
            return Integer.parseInt(questionId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid grammar questionId: " + questionId);
        }
    }

    private double nullToDefault(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private int nullToDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
