package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.DailyLearningStatusResponse;
import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.LearningPlanRequest;
import com.englishlearningcopilot.backend.dto.LearningPlanResponse;
import com.englishlearningcopilot.backend.dto.ProfileSnapshotResponse;

public interface LearningPlanService {

    LearningPlanResponse getLearningPlan(String username);

    LearningPlanResponse updateLearningPlan(String username, LearningPlanRequest request);

    DailyLearningStatusResponse getDailyStatus(String username);

    DailyPracticeProgressResponse getVocabularyProgress(String username);

    DailyPracticeProgressResponse getGrammarProgress(String username);

    ProfileSnapshotResponse getProfileSnapshot(String username);

    void recordVocabularyPractice(Long userId, Long vocabularyId);

    void recordGrammarPractice(Long userId, Integer grammarQuestionId);
}
