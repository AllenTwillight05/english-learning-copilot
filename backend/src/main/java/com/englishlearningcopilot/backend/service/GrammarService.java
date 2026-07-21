package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.GrammarFavoriteRequest;
import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteResponse;
import com.englishlearningcopilot.backend.dto.GrammarNotebookQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarOverviewResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeResultRequest;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarRatingRequest;
import com.englishlearningcopilot.backend.dto.GrammarTopicResponse;
import java.util.List;

public interface GrammarService {

    GrammarOverviewResponse getOverview(String username);

    List<GrammarTopicResponse> getTopics(String username);

    List<GrammarPracticeQuestionResponse> getPracticeQuestions(String username, String category);

    List<GrammarPracticeQuestionResponse> getReviewQuestions(String username);

    DailyPracticeProgressResponse getProgress(String username);

    List<GrammarNotebookQuestionResponse> getNotebookQuestions(String username);

    void submitPracticeResult(String username, GrammarPracticeResultRequest request);

    void submitRating(String username, GrammarRatingRequest request);

    GrammarFavoriteResponse toggleFavorite(String username, GrammarFavoriteRequest request);
}
