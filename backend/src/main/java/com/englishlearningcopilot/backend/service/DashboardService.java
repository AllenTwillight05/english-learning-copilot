package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.DashboardCommunityLearningTrendsResponse;
import com.englishlearningcopilot.backend.dto.DashboardStudyPlanResponse;
import com.englishlearningcopilot.backend.dto.DashboardWeeklyOverviewResponse;

public interface DashboardService {

    DashboardCommunityLearningTrendsResponse getCommunityLearningTrends();

    DashboardStudyPlanResponse getStudyPlan(String username);

    DashboardWeeklyOverviewResponse getWeeklyOverview(String username);
}
