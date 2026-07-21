package com.englishlearningcopilot.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteRequest;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeResultRequest;
import com.englishlearningcopilot.backend.dto.GrammarRatingRequest;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.GrammarQuestion;
import com.englishlearningcopilot.backend.entity.UserGrammarbook;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.GrammarQuestionRepository;
import com.englishlearningcopilot.backend.repository.UserGrammarbookRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordProgressRepository;
import com.englishlearningcopilot.backend.service.impl.GrammarServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GrammarServiceImplTest {

    @Mock
    private GrammarQuestionRepository grammarQuestionRepository;

    @Mock
    private UserGrammarbookRepository userGrammarbookRepository;

    @Mock
    private UserWordProgressRepository userWordProgressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LearningPlanService learningPlanService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private GrammarServiceImpl grammarService;

    @Test
    void getPracticeQuestionsReturnsUnpracticedQuestionsForUserAndCategory() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(grammarQuestionRepository.findRandomUnpracticedQuestionsByCategory(eq(7L), eq("Tense"), any(Pageable.class)))
                .thenReturn(List.of(question(1, "Tense")));

        List<GrammarPracticeQuestionResponse> questions = grammarService.getPracticeQuestions("learner", "Tense");

        assertThat(questions).extracting(GrammarPracticeQuestionResponse::id).containsExactly(1);
        verify(grammarQuestionRepository)
                .findRandomUnpracticedQuestionsByCategory(eq(7L), eq("Tense"), any(Pageable.class));
    }

    @Test
    void submitPracticeResultCreatesGrammarbookRowAndRecordsProgress() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(grammarQuestionRepository.existsById(1)).thenReturn(true);
        when(userGrammarbookRepository.findByUserIdAndGrammarQuestionId(7L, 1)).thenReturn(Optional.empty());

        grammarService.submitPracticeResult("learner", new GrammarPracticeResultRequest(1, true));

        ArgumentCaptor<UserGrammarbook> captor = ArgumentCaptor.forClass(UserGrammarbook.class);
        verify(userGrammarbookRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getGrammarQuestionId()).isEqualTo(1);
        assertThat(captor.getValue().isIncorrect()).isTrue();
        verify(learningPlanService).recordGrammarPractice(7L, 1);
    }

    @Test
    void submitPracticeResultRejectsMissingQuestion() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(grammarQuestionRepository.existsById(1)).thenReturn(false);

        assertThatThrownBy(() -> grammarService.submitPracticeResult(
                "learner",
                new GrammarPracticeResultRequest(1, true)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Grammar question was not found.");

        verify(userGrammarbookRepository, never()).save(any());
        verify(learningPlanService, never()).recordGrammarPractice(any(), anyInt());
    }

    @Test
    void submitRatingDelegatesToReviewService() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(grammarQuestionRepository.existsById(1)).thenReturn(true);

        grammarService.submitRating("learner", new GrammarRatingRequest(1, 4));

        verify(reviewService).submitGrammarRating(7L, 1, 4);
    }

    @Test
    void toggleFavoriteFlipsExistingGrammarbookRow() {
        AppUser user = user(7L, "learner");
        UserGrammarbook row = grammarbook(7L, 1, false, false);
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(grammarQuestionRepository.existsById(1)).thenReturn(true);
        when(userGrammarbookRepository.findByUserIdAndGrammarQuestionId(7L, 1)).thenReturn(Optional.of(row));
        when(userGrammarbookRepository.save(row)).thenReturn(row);

        GrammarFavoriteResponse response = grammarService.toggleFavorite("learner", new GrammarFavoriteRequest(1));

        assertThat(response.favorited()).isTrue();
        verify(userGrammarbookRepository).save(row);
    }

    @Test
    void getNotebookQuestionsCombinesNotebookRowsWithQuestions() {
        AppUser user = user(7L, "learner");
        UserGrammarbook row = grammarbook(7L, 1, true, true);
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(userGrammarbookRepository.findNotebookRowsByUserId(7L)).thenReturn(List.of(row));
        when(grammarQuestionRepository.findAllById(List.of(1))).thenReturn(List.of(question(1, "Tense")));

        var questions = grammarService.getNotebookQuestions("learner");

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).wrong()).isTrue();
        assertThat(questions.get(0).favorited()).isTrue();
    }

    @Test
    void getProgressDelegatesToLearningPlanService() {
        when(learningPlanService.getGrammarProgress("learner"))
                .thenReturn(new DailyPracticeProgressResponse(2, 5, 3, false));

        DailyPracticeProgressResponse response = grammarService.getProgress("learner");

        assertThat(response.completed()).isEqualTo(2);
        assertThat(response.total()).isEqualTo(5);
    }

    @Test
    void toggleFavoriteRequiresAuthentication() {
        assertThatThrownBy(() -> grammarService.toggleFavorite(null, new GrammarFavoriteRequest(1)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Authentication is required.");
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

    private static GrammarQuestion question(Integer id, String category) {
        GrammarQuestion question = new GrammarQuestion();
        question.setId(id);
        question.setGrammarCategory(category);
        question.setQuestionText("Choose the answer.");
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setOptionD("D");
        question.setAnswer("A");
        question.setExplanation("Because.");
        return question;
    }

    private static UserGrammarbook grammarbook(Long userId, Integer questionId, boolean incorrect, boolean favorited) {
        UserGrammarbook grammarbook = new UserGrammarbook();
        grammarbook.setUserId(userId);
        grammarbook.setGrammarQuestionId(questionId);
        grammarbook.setIncorrect(incorrect);
        grammarbook.setFavorited(favorited);
        return grammarbook;
    }
}
