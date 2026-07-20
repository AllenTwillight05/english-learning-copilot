package com.englishlearningcopilot.backend.service.impl;

import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteRequest;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteResponse;
import com.englishlearningcopilot.backend.dto.GrammarNotebookQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeResultRequest;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarRatingRequest;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.GrammarQuestion;
import com.englishlearningcopilot.backend.entity.UserGrammarbook;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.GrammarQuestionRepository;
import com.englishlearningcopilot.backend.repository.UserGrammarbookRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.service.GrammarService;
import com.englishlearningcopilot.backend.service.LearningPlanService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrammarServiceImpl implements GrammarService {

    private final GrammarQuestionRepository grammarQuestionRepository;
    private final UserGrammarbookRepository userGrammarbookRepository;
    private final UserRepository userRepository;
    private final LearningPlanService learningPlanService;

    public GrammarServiceImpl(
            GrammarQuestionRepository grammarQuestionRepository,
            UserGrammarbookRepository userGrammarbookRepository,
            UserRepository userRepository,
            LearningPlanService learningPlanService
    ) {
        this.grammarQuestionRepository = grammarQuestionRepository;
        this.userGrammarbookRepository = userGrammarbookRepository;
        this.userRepository = userRepository;
        this.learningPlanService = learningPlanService;
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
    @Transactional(readOnly = true)
    public void submitRating(String username, GrammarRatingRequest request) {
        getCurrentUser(username);
        validateQuestionExists(request.grammarQuestionId());
        // Grammar self-ratings are accepted by the service but are not persisted yet.
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
}
