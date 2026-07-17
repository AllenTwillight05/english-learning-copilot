package com.englishlearningcopilot.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishlearningcopilot.backend.dto.VocabularyRatingRequest;
import com.englishlearningcopilot.backend.dto.VocabularyPracticeWordResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.entity.UserWordbook;
import com.englishlearningcopilot.backend.entity.Vocabulary;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordProgressRepository;
import com.englishlearningcopilot.backend.repository.UserWordbookRepository;
import com.englishlearningcopilot.backend.repository.VocabularyRepository;
import com.englishlearningcopilot.backend.service.VocabularyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "debug=false")
class VocabularyPracticeDifficultyIntegrationTest {

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWordbookRepository userWordbookRepository;

    @Autowired
    private UserWordProgressRepository userWordProgressRepository;

    @BeforeEach
    void setUp() {
        userWordProgressRepository.deleteAll();
        userWordbookRepository.deleteAll();
        vocabularyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void filtersPracticeWordsByWholeTagForEveryDifficulty() {
        saveVocabulary("starter-zk", "zk");
        saveVocabulary("starter-gk", "gk extra");
        saveVocabulary("basic", "cet4");
        saveVocabulary("intermediate", "cet6");
        saveVocabulary("advanced-ky", "ky");
        saveVocabulary("advanced-toefl", "toefl");
        saveVocabulary("advanced-gre", "gre");
        saveVocabulary("advanced-ielts", "ielts");
        saveVocabulary("empty", "");
        saveVocabulary("null-tag", null);
        saveVocabulary("not-cet4-substring", "cet40");

        assertWords("starter", "starter-gk", "starter-zk");
        assertWords("basic", "basic");
        assertWords("intermediate", "intermediate");
        assertWords("advanced", "advanced-gre", "advanced-ielts", "advanced-ky", "advanced-toefl");
    }

    @Test
    void keepsExcludingWordsAlreadyInTheUsersWordbook() {
        Vocabulary learned = saveVocabulary("learned", "zk");
        saveVocabulary("unlearned", "gk");
        AppUser user = saveUser("learner");

        UserWordbook wordbook = new UserWordbook();
        wordbook.setUserId(user.getId());
        wordbook.setVocabularyId(learned.getId());
        userWordbookRepository.save(wordbook);

        List<String> words = vocabularyService.getPracticeWords(user.getUsername(), "starter").stream()
                .map(VocabularyPracticeWordResponse::word)
                .toList();

        assertThat(words).containsExactly("unlearned");
    }

    @Test
    void practiceRatingAddsWordToFsrsReviewProgress() {
        Vocabulary vocabulary = saveVocabulary("reviewable", "zk");
        AppUser user = saveUser("learner");

        vocabularyService.submitRating(user.getUsername(), new VocabularyRatingRequest(vocabulary.getId(), 3));

        assertThat(userWordbookRepository.findByUserIdAndVocabularyId(user.getId(), vocabulary.getId())).isPresent();
        assertThat(userWordProgressRepository.findByUserIdAndQuestionIdAndQuestionType(
                user.getId(),
                String.valueOf(vocabulary.getId()),
                "vocabulary"
        )).isPresent();
    }

    private void assertWords(String level, String... expectedWords) {
        List<String> words = vocabularyService.getPracticeWords(null, level).stream()
                .map(VocabularyPracticeWordResponse::word)
                .sorted()
                .toList();
        assertThat(words).containsExactly(expectedWords);
    }

    private Vocabulary saveVocabulary(String word, String tag) {
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWord(word);
        vocabulary.setTag(tag);
        return vocabularyRepository.save(vocabulary);
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
