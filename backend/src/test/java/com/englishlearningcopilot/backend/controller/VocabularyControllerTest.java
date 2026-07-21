package com.englishlearningcopilot.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteRequest;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteResponse;
import com.englishlearningcopilot.backend.dto.VocabularyPracticeWordResponse;
import com.englishlearningcopilot.backend.dto.VocabularyRatingRequest;
import com.englishlearningcopilot.backend.dto.VocabularyWordbookWordResponse;
import com.englishlearningcopilot.backend.exception.GlobalExceptionHandler;
import com.englishlearningcopilot.backend.service.VocabularyService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class VocabularyControllerTest {

    @Mock
    private VocabularyService vocabularyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new VocabularyController(vocabularyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void getMemoryPassesPrincipalNameToService() throws Exception {
        when(vocabularyService.getMemory("learner"))
                .thenReturn(Map.of("retentionRate", 92, "stats", List.of()));

        mockMvc.perform(get("/api/vocabulary/memory").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionRate").value(92));

        verify(vocabularyService).getMemory("learner");
    }

    @Test
    void getPracticeWordsUsesDefaultStarterLevel() throws Exception {
        when(vocabularyService.getPracticeWords("learner", "starter"))
                .thenReturn(List.of(practiceWord(10L, "accept")));

        mockMvc.perform(get("/api/vocabulary/practice-words").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].word").value("accept"));

        verify(vocabularyService).getPracticeWords("learner", "starter");
    }

    @Test
    void getWordbookWordsReturnsUserWordbook() throws Exception {
        when(vocabularyService.getWordbookWords("learner"))
                .thenReturn(List.of(new VocabularyWordbookWordResponse(
                        10L,
                        "accept",
                        "/ak-sept/",
                        "receive willingly",
                        "接受",
                        "cet4",
                        "accept.mp3",
                        true
                )));

        mockMvc.perform(get("/api/vocabulary/wordbook-words").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].word").value("accept"))
                .andExpect(jsonPath("$[0].favorited").value(true));
    }

    @Test
    void submitRatingValidatesBodyAndDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/vocabulary/practice-ratings")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vocabularyId": 10,
                                  "score": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vocabulary rating received."));

        ArgumentCaptor<VocabularyRatingRequest> captor = ArgumentCaptor.forClass(VocabularyRatingRequest.class);
        verify(vocabularyService).submitRating(org.mockito.Mockito.eq("learner"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().score()).isEqualTo(3);
    }

    @Test
    void submitRatingRejectsOutOfRangeScore() throws Exception {
        mockMvc.perform(post("/api/vocabulary/practice-ratings")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vocabularyId": 10,
                                  "score": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.score").exists());
    }

    @Test
    void toggleFavoriteReturnsFavoriteState() throws Exception {
        when(vocabularyService.toggleFavorite(any(String.class), any(VocabularyFavoriteRequest.class)))
                .thenReturn(new VocabularyFavoriteResponse(10L, true));

        mockMvc.perform(post("/api/vocabulary/wordbook-favorites")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vocabularyId": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vocabularyId").value(10))
                .andExpect(jsonPath("$.favorited").value(true));

        verify(vocabularyService).toggleFavorite(any(String.class), any(VocabularyFavoriteRequest.class));
    }

    @Test
    void getPracticeProgressDelegatesToLearningProgressServicePath() throws Exception {
        when(vocabularyService.getPracticeProgress("learner"))
                .thenReturn(new DailyPracticeProgressResponse(2, 5, 3, false));

        mockMvc.perform(get("/api/vocabulary/practice-progress").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(2))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.remaining").value(3));

        verify(vocabularyService).getPracticeProgress("learner");
    }

    private static VocabularyPracticeWordResponse practiceWord(Long id, String word) {
        return new VocabularyPracticeWordResponse(
                id,
                word,
                "/ak-sept/",
                "receive willingly",
                "接受",
                "translation",
                "3",
                "1",
                "cet4",
                "1000",
                "2000",
                "",
                "uk.mp3",
                "us.mp3",
                false,
                List.of("接受", "拒绝"),
                List.of("accept", "reject")
        );
    }
}
