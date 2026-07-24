package com.englishlearningcopilot.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteRequest;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteResponse;
import com.englishlearningcopilot.backend.dto.GrammarOverviewResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeResultRequest;
import com.englishlearningcopilot.backend.dto.GrammarRatingRequest;
import com.englishlearningcopilot.backend.dto.GrammarTopicResponse;
import com.englishlearningcopilot.backend.exception.GlobalExceptionHandler;
import com.englishlearningcopilot.backend.service.GrammarService;
import java.util.List;
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
class GrammarControllerTest {

    @Mock
    private GrammarService grammarService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new GrammarController(grammarService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void getOverviewPassesPrincipalNameToService() throws Exception {
        when(grammarService.getOverview("learner"))
                .thenReturn(new GrammarOverviewResponse(80, List.of()));

        mockMvc.perform(get("/api/grammar/overview").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masteryRate").value(80));

        verify(grammarService).getOverview("learner");
    }

    @Test
    void getTopicsReturnsTopicProgress() throws Exception {
        when(grammarService.getTopics("learner"))
                .thenReturn(List.of(new GrammarTopicResponse("Tense", "Tense", "summary", List.of("example"), 50, "2 questions")));

        mockMvc.perform(get("/api/grammar/topics").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("Tense"))
                .andExpect(jsonPath("$[0].progress").value(50));
    }

    @Test
    void getPracticeQuestionsDelegatesCategoryAndPrincipal() throws Exception {
        when(grammarService.getPracticeQuestions("learner", "Tense"))
                .thenReturn(List.of(question(1, "Tense")));

        mockMvc.perform(get("/api/grammar/practice-questions")
                        .principal(() -> "learner")
                        .param("category", "Tense"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].grammar_category").value("Tense"));

        verify(grammarService).getPracticeQuestions("learner", "Tense");
    }

    @Test
    void submitPracticeResultValidatesBodyAndDelegates() throws Exception {
        mockMvc.perform(post("/api/grammar/practice-results")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grammarQuestionId": 1,
                                  "incorrect": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Grammar practice result received."));

        ArgumentCaptor<GrammarPracticeResultRequest> captor =
                ArgumentCaptor.forClass(GrammarPracticeResultRequest.class);
        verify(grammarService).submitPracticeResult(org.mockito.Mockito.eq("learner"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().incorrect()).isTrue();
    }

    @Test
    void submitRatingRejectsOutOfRangeScore() throws Exception {
        mockMvc.perform(post("/api/grammar/practice-ratings")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grammarQuestionId": 1,
                                  "score": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.score").exists());
    }

    @Test
    void toggleFavoriteReturnsFavoriteState() throws Exception {
        when(grammarService.toggleFavorite(any(String.class), any(GrammarFavoriteRequest.class)))
                .thenReturn(new GrammarFavoriteResponse(1, true));

        mockMvc.perform(post("/api/grammar/notebook-favorites")
                        .principal(() -> "learner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grammarQuestionId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grammarQuestionId").value(1))
                .andExpect(jsonPath("$.favorited").value(true));
    }

    @Test
    void getProgressReturnsDailyProgress() throws Exception {
        when(grammarService.getProgress("learner"))
                .thenReturn(new DailyPracticeProgressResponse(1, 5, 4, false));

        mockMvc.perform(get("/api/grammar/progress").principal(() -> "learner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.total").value(5));
    }

    private static GrammarPracticeQuestionResponse question(Integer id, String category) {
        return new GrammarPracticeQuestionResponse(
                id,
                "Choose the answer.",
                List.of("A", "B", "C", "D"),
                "A",
                category,
                "Because."
        );
    }
}
