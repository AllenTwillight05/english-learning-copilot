package com.englishlearningcopilot.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.GrammarQuestion;
import com.englishlearningcopilot.backend.entity.UserGrammarbook;
import com.englishlearningcopilot.backend.entity.UserRole;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = "debug=false")
class GrammarRepositoryTest {

    @Autowired
    private GrammarQuestionRepository grammarQuestionRepository;

    @Autowired
    private UserGrammarbookRepository userGrammarbookRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUnpracticedQuestionsByCategory() {
        AppUser user = userRepository.save(user("learner"));
        GrammarQuestion practiced = grammarQuestionRepository.save(question(1, "Tense"));
        GrammarQuestion unpracticed = grammarQuestionRepository.save(question(2, "Tense"));
        grammarQuestionRepository.save(question(3, "Clause"));
        userGrammarbookRepository.save(grammarbook(user.getId(), practiced.getId(), true, false));

        List<GrammarQuestion> questions = grammarQuestionRepository.findRandomUnpracticedQuestionsByCategory(
                user.getId(),
                "Tense",
                PageRequest.of(0, 3)
        );

        assertThat(questions).extracting(GrammarQuestion::getId).containsExactly(unpracticed.getId());
    }

    @Test
    void findsNotebookRowsForIncorrectOrFavoritedQuestionsNewestFirst() {
        AppUser user = userRepository.save(user("learner"));
        UserGrammarbook normal = userGrammarbookRepository.save(grammarbook(user.getId(), 1, false, false));
        UserGrammarbook incorrect = userGrammarbookRepository.save(grammarbook(user.getId(), 2, true, false));
        UserGrammarbook favorited = userGrammarbookRepository.save(grammarbook(user.getId(), 3, false, true));

        List<UserGrammarbook> rows = userGrammarbookRepository.findNotebookRowsByUserId(user.getId());

        assertThat(rows).extracting(UserGrammarbook::getId)
                .containsExactly(favorited.getId(), incorrect.getId());
        assertThat(rows).doesNotContain(normal);
    }

    private static AppUser user(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setDisplayName("Learner");
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
