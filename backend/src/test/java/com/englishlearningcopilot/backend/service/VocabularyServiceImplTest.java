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
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteRequest;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteResponse;
import com.englishlearningcopilot.backend.dto.VocabularyPracticeWordResponse;
import com.englishlearningcopilot.backend.dto.VocabularyRatingRequest;
import com.englishlearningcopilot.backend.dto.VocabularyWordbookWordResponse;
import com.englishlearningcopilot.backend.entity.AppUser;
import com.englishlearningcopilot.backend.entity.UserRole;
import com.englishlearningcopilot.backend.entity.UserWordbook;
import com.englishlearningcopilot.backend.entity.Vocabulary;
import com.englishlearningcopilot.backend.exception.ResourceNotFoundException;
import com.englishlearningcopilot.backend.repository.UserRepository;
import com.englishlearningcopilot.backend.repository.UserWordProgressRepository;
import com.englishlearningcopilot.backend.repository.UserWordbookRepository;
import com.englishlearningcopilot.backend.repository.VocabularyRepository;
import com.englishlearningcopilot.backend.service.impl.VocabularyServiceImpl;
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
class VocabularyServiceImplTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWordProgressRepository userWordProgressRepository;

    @Mock
    private UserWordbookRepository userWordbookRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private LearningPlanService learningPlanService;

    @InjectMocks
    private VocabularyServiceImpl vocabularyService;

    @Test
    void getPracticeWordsForAnonymousUserUsesLevelTagsWithoutUserLookup() {
        when(vocabularyRepository.findRandomPracticeWordsByTags(
                eq("zk"), eq("gk"), any(String.class), any(String.class), any(Pageable.class)
        )).thenReturn(List.of(vocabulary(10L, "accept", "zk gk")));

        List<VocabularyPracticeWordResponse> words = vocabularyService.getPracticeWords(null, "starter");

        assertThat(words).extracting(VocabularyPracticeWordResponse::word).containsExactly("accept");
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void getPracticeWordsForLoggedInUserExcludesLearnedWords() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(vocabularyRepository.findRandomUnlearnedPracticeWordsByTags(
                eq(7L), eq("cet4"), any(String.class), any(String.class), any(String.class), any(Pageable.class)
        )).thenReturn(List.of(vocabulary(10L, "accept", "cet4")));

        List<VocabularyPracticeWordResponse> words = vocabularyService.getPracticeWords("learner", "basic");

        assertThat(words).extracting(VocabularyPracticeWordResponse::id).containsExactly(10L);
    }

    @Test
    void getPracticeWordsRejectsUnsupportedLevel() {
        assertThatThrownBy(() -> vocabularyService.getPracticeWords(null, "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported vocabulary practice level: unknown");
    }

    @Test
    void getWordbookWordsCombinesWordbookRowsWithVocabularyDetails() {
        AppUser user = user(7L, "learner");
        UserWordbook wordbook = wordbook(7L, 10L, true);
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(userWordbookRepository.findByUserIdOrderByIdDesc(7L)).thenReturn(List.of(wordbook));
        when(vocabularyRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(vocabulary(10L, "accept", "cet4")));

        List<VocabularyWordbookWordResponse> words = vocabularyService.getWordbookWords("learner");

        assertThat(words).hasSize(1);
        assertThat(words.get(0).word()).isEqualTo("accept");
        assertThat(words.get(0).favorited()).isTrue();
    }

    @Test
    void submitRatingCreatesWordbookRowWhenWordIsNew() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(vocabularyRepository.existsById(10L)).thenReturn(true);
        when(userWordbookRepository.findByUserIdAndVocabularyId(7L, 10L)).thenReturn(Optional.empty());

        vocabularyService.submitRating("learner", new VocabularyRatingRequest(10L, 3));

        ArgumentCaptor<UserWordbook> captor = ArgumentCaptor.forClass(UserWordbook.class);
        verify(userWordbookRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getVocabularyId()).isEqualTo(10L);
        verify(reviewService).submitRating(7L, "10", 3);
        verify(learningPlanService).recordVocabularyPractice(7L, 10L);
    }

    @Test
    void submitRatingDoesNotCreateDuplicateWordbookRow() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(vocabularyRepository.existsById(10L)).thenReturn(true);
        when(userWordbookRepository.findByUserIdAndVocabularyId(7L, 10L))
                .thenReturn(Optional.of(wordbook(7L, 10L, false)));

        vocabularyService.submitRating("learner", new VocabularyRatingRequest(10L, 4));

        verify(userWordbookRepository, never()).save(any(UserWordbook.class));
        verify(reviewService).submitRating(7L, "10", 4);
    }

    @Test
    void submitRatingRejectsMissingVocabulary() {
        AppUser user = user(7L, "learner");
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(vocabularyRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> vocabularyService.submitRating("learner", new VocabularyRatingRequest(10L, 3)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vocabulary word was not found.");

        verify(reviewService, never()).submitRating(any(), any(), anyInt());
    }

    @Test
    void toggleFavoriteFlipsExistingWordbookFavoriteState() {
        AppUser user = user(7L, "learner");
        UserWordbook wordbook = wordbook(7L, 10L, false);
        when(userRepository.findByUsername("learner")).thenReturn(Optional.of(user));
        when(vocabularyRepository.existsById(10L)).thenReturn(true);
        when(userWordbookRepository.findByUserIdAndVocabularyId(7L, 10L)).thenReturn(Optional.of(wordbook));
        when(userWordbookRepository.save(wordbook)).thenReturn(wordbook);

        VocabularyFavoriteResponse response = vocabularyService.toggleFavorite(
                "learner",
                new VocabularyFavoriteRequest(10L)
        );

        assertThat(response.favorited()).isTrue();
        verify(userWordbookRepository).save(wordbook);
    }

    @Test
    void toggleFavoriteRequiresAuthentication() {
        assertThatThrownBy(() -> vocabularyService.toggleFavorite(null, new VocabularyFavoriteRequest(10L)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Authentication is required.");
    }

    @Test
    void getPracticeProgressDelegatesToLearningPlanService() {
        when(learningPlanService.getVocabularyProgress("learner"))
                .thenReturn(new DailyPracticeProgressResponse(3, 8, 5, false));

        DailyPracticeProgressResponse response = vocabularyService.getPracticeProgress("learner");

        assertThat(response.completed()).isEqualTo(3);
        assertThat(response.total()).isEqualTo(8);
        assertThat(response.remaining()).isEqualTo(5);
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

    private static Vocabulary vocabulary(Long id, String word, String tag) {
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setId(id);
        vocabulary.setWord(word);
        vocabulary.setPhonetic("/" + word + "/");
        vocabulary.setDefinition("definition");
        vocabulary.setBriefTranslation("释义");
        vocabulary.setTranslation("translation");
        vocabulary.setTag(tag);
        vocabulary.setUsAudio(word + ".mp3");
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
