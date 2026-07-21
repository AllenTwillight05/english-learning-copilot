package com.englishlearningcopilot.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.entity.UserWordbook;
import com.englishlearningcopilot.backend.entity.Vocabulary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = "debug=false")
class VocabularyRepositoryTest {

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWordbookRepository userWordbookRepository;

    @Test
    void findsPracticeWordsByWholeTag() {
        Vocabulary starter = vocabulary("accept", "zk cet4");
        Vocabulary advanced = vocabulary("abstract", "gre");
        vocabularyRepository.saveAll(List.of(starter, advanced));

        List<Vocabulary> words = vocabularyRepository.findRandomPracticeWordsByTags(
                "zk",
                "__unused_vocabulary_tag__",
                "__unused_vocabulary_tag__",
                "__unused_vocabulary_tag__",
                PageRequest.of(0, 10)
        );

        assertThat(words).extracting(Vocabulary::getWord).containsExactly("accept");
    }

    @Test
    void findsUnlearnedPracticeWordsByTagsForUser() {
        AppUser user = userRepository.save(user("learner"));
        Vocabulary learned = vocabularyRepository.save(vocabulary("accept", "cet4"));
        Vocabulary unlearned = vocabularyRepository.save(vocabulary("ability", "cet4"));
        userWordbookRepository.save(wordbook(user.getId(), learned.getId(), false));

        List<Vocabulary> words = vocabularyRepository.findRandomUnlearnedPracticeWordsByTags(
                user.getId(),
                "cet4",
                "__unused_vocabulary_tag__",
                "__unused_vocabulary_tag__",
                "__unused_vocabulary_tag__",
                PageRequest.of(0, 10)
        );

        assertThat(words).extracting(Vocabulary::getId).containsExactly(unlearned.getId());
    }

    @Test
    void findsUserWordbookRowsInNewestFirstOrder() {
        AppUser user = userRepository.save(user("learner"));
        Vocabulary first = vocabularyRepository.save(vocabulary("accept", "cet4"));
        Vocabulary second = vocabularyRepository.save(vocabulary("ability", "cet4"));
        UserWordbook firstRow = userWordbookRepository.save(wordbook(user.getId(), first.getId(), false));
        UserWordbook secondRow = userWordbookRepository.save(wordbook(user.getId(), second.getId(), true));

        List<UserWordbook> rows = userWordbookRepository.findByUserIdOrderByIdDesc(user.getId());

        assertThat(rows).extracting(UserWordbook::getId)
                .containsExactly(secondRow.getId(), firstRow.getId());
        assertThat(userWordbookRepository.findByUserIdAndVocabularyId(user.getId(), second.getId()))
                .contains(secondRow);
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

    private static Vocabulary vocabulary(String word, String tag) {
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWord(word);
        vocabulary.setDefinition("definition");
        vocabulary.setTranslation("translation");
        vocabulary.setBriefTranslation("释义");
        vocabulary.setTag(tag);
        vocabulary.setChineseOptions(List.of("释义", "干扰项"));
        vocabulary.setEnglishOptions(List.of(word, "other"));
        return vocabulary;
    }

    private static UserWordbook wordbook(Long userId, Long vocabularyId, boolean favorited) {
        UserWordbook wordbook = new UserWordbook();
        wordbook.setUserId(userId);
        wordbook.setVocabularyId(vocabularyId);
        wordbook.setFavorited(favorited);
        return wordbook;
    }
}
