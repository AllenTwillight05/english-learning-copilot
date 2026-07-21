package com.englishlearningcopilot.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishlearningcopilot.backend.dto.GrammarFavoriteRequest;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteResponse;
import com.englishlearningcopilot.backend.dto.GrammarNotebookQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeResultRequest;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarRatingRequest;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.GrammarQuestion;
import com.englishlearningcopilot.backend.entity.UserGrammarbook;
import com.englishlearningcopilot.backend.entity.UserWordProgress;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.repository.GrammarQuestionRepository;
import com.englishlearningcopilot.backend.repository.UserGrammarbookRepository;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordProgressRepository;
import com.englishlearningcopilot.backend.service.GrammarService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "debug=false")
class GrammarNotebookIntegrationTest {

    @Autowired
    private GrammarService grammarService;

    @Autowired
    private GrammarQuestionRepository grammarQuestionRepository;

    @Autowired
    private UserGrammarbookRepository userGrammarbookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWordProgressRepository userWordProgressRepository;

    @BeforeEach
    void setUp() {
        userWordProgressRepository.deleteAll();
        userGrammarbookRepository.deleteAll();
        grammarQuestionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void notebookReturnsOnlyIncorrectOrFavoritedQuestionsWithBothFlags() {
        AppUser user = saveUser("learner");
        GrammarQuestion wrongQuestion = saveQuestion(1, "Wrong question");
        GrammarQuestion favoriteQuestion = saveQuestion(2, "Favorite question");
        GrammarQuestion hiddenQuestion = saveQuestion(3, "Hidden question");

        saveGrammarbook(user, wrongQuestion, true, false);
        saveGrammarbook(user, favoriteQuestion, false, true);
        saveGrammarbook(user, hiddenQuestion, false, false);

        List<GrammarNotebookQuestionResponse> questions = grammarService.getNotebookQuestions(user.getUsername());

        assertThat(questions).extracting(GrammarNotebookQuestionResponse::id)
                .containsExactly(favoriteQuestion.getId(), wrongQuestion.getId());
        assertThat(questions.get(0).wrong()).isFalse();
        assertThat(questions.get(0).favorited()).isTrue();
        assertThat(questions.get(1).wrong()).isTrue();
        assertThat(questions.get(1).favorited()).isFalse();
        assertThat(questions.get(0).options()).containsExactly("A", "B", "C", "D");
    }

    @Test
    void practiceResultAndFavoriteUpdateTheSameGrammarbookRow() {
        AppUser user = saveUser("learner");
        GrammarQuestion question = saveQuestion(10, "Shared state question");

        grammarService.submitPracticeResult(
                user.getUsername(),
                new GrammarPracticeResultRequest(question.getId(), true)
        );
        GrammarFavoriteResponse favoriteResponse = grammarService.toggleFavorite(
                user.getUsername(),
                new GrammarFavoriteRequest(question.getId())
        );

        assertThat(favoriteResponse.favorited()).isTrue();
        assertThat(userGrammarbookRepository.findAll()).hasSize(1);
        UserGrammarbook saved = userGrammarbookRepository.findAll().get(0);
        assertThat(saved.isIncorrect()).isTrue();
        assertThat(saved.isFavorited()).isTrue();

        grammarService.submitPracticeResult(
                user.getUsername(),
                new GrammarPracticeResultRequest(question.getId(), false)
        );
        List<GrammarNotebookQuestionResponse> questions = grammarService.getNotebookQuestions(user.getUsername());
        assertThat(questions).singleElement().satisfies(response -> {
            assertThat(response.wrong()).isFalse();
            assertThat(response.favorited()).isTrue();
        });

        grammarService.toggleFavorite(user.getUsername(), new GrammarFavoriteRequest(question.getId()));
        assertThat(grammarService.getNotebookQuestions(user.getUsername())).isEmpty();
    }

    @Test
    void practiceReturnsThreeRandomQuestionsFromCategoryThatUserHasNotAnswered() {
        AppUser user = saveUser("learner");
        GrammarQuestion answeredWrong = saveQuestion(20, "Answered wrong", "Clause");
        GrammarQuestion answeredCorrect = saveQuestion(21, "Answered correctly", "Clause");
        GrammarQuestion availableOne = saveQuestion(22, "Available one", "Clause");
        GrammarQuestion availableTwo = saveQuestion(23, "Available two", "Clause");
        GrammarQuestion availableThree = saveQuestion(24, "Available three", "Clause");
        saveQuestion(25, "Different category", "Tense");

        saveGrammarbook(user, answeredWrong, true, false);
        saveGrammarbook(user, answeredCorrect, false, false);

        List<GrammarPracticeQuestionResponse> questions =
                grammarService.getPracticeQuestions(user.getUsername(), "Clause");

        assertThat(questions).hasSize(3);
        assertThat(questions).extracting(GrammarPracticeQuestionResponse::id)
                .containsExactlyInAnyOrder(
                        availableOne.getId(),
                        availableTwo.getId(),
                        availableThree.getId()
                );
        assertThat(questions).allSatisfy(question -> {
            assertThat(question.grammarCategory()).isEqualTo("Clause");
            assertThat(question.options()).containsExactly("A", "B", "C", "D");
        });
    }

    @Test
    void selfRatingAddsQuestionToFsrsReviewProgressWithoutUpdatingGrammarbook() {
        AppUser user = saveUser("learner");
        GrammarQuestion question = saveQuestion(30, "Self-rated question");

        grammarService.submitRating(
                user.getUsername(),
                new GrammarRatingRequest(question.getId(), 4)
        );

        assertThat(userGrammarbookRepository.findAll()).isEmpty();
        assertThat(userWordProgressRepository.findByUserIdAndQuestionIdAndQuestionType(
                user.getId(),
                String.valueOf(question.getId()),
                "grammar"
        )).isPresent();
    }

    @Test
    void reviewQuestionsReturnDueGrammarProgressRows() {
        AppUser user = saveUser("learner");
        GrammarQuestion dueQuestion = saveQuestion(40, "Due question");
        GrammarQuestion futureQuestion = saveQuestion(41, "Future question");

        saveGrammarProgress(user, dueQuestion, Instant.now().minusSeconds(60));
        saveGrammarProgress(user, futureQuestion, Instant.now().plusSeconds(86400));

        List<GrammarPracticeQuestionResponse> questions = grammarService.getReviewQuestions(user.getUsername());

        assertThat(questions).extracting(GrammarPracticeQuestionResponse::id)
                .containsExactly(dueQuestion.getId());
    }

    private UserWordProgress saveGrammarProgress(AppUser user, GrammarQuestion question, Instant due) {
        UserWordProgress progress = new UserWordProgress();
        progress.setUserId(user.getId());
        progress.setQuestionId(String.valueOf(question.getId()));
        progress.setQuestionType("grammar");
        progress.setDue(due);
        return userWordProgressRepository.save(progress);
    }

    private UserGrammarbook saveGrammarbook(
            AppUser user,
            GrammarQuestion question,
            boolean incorrect,
            boolean favorited
    ) {
        UserGrammarbook grammarbook = new UserGrammarbook();
        grammarbook.setUserId(user.getId());
        grammarbook.setGrammarQuestionId(question.getId());
        grammarbook.setIncorrect(incorrect);
        grammarbook.setFavorited(favorited);
        return userGrammarbookRepository.save(grammarbook);
    }

    private GrammarQuestion saveQuestion(Integer id, String text) {
        return saveQuestion(id, text, "Test category");
    }

    private GrammarQuestion saveQuestion(Integer id, String text, String category) {
        GrammarQuestion question = new GrammarQuestion();
        question.setId(id);
        question.setQuestionText(text);
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setOptionD("D");
        question.setAnswer("A");
        question.setGrammarCategory(category);
        question.setExplanation("Test explanation");
        return grammarQuestionRepository.save(question);
    }

    private AppUser saveUser(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setDisplayName(username);
        user.setPasswordHash("test-password-hash");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
