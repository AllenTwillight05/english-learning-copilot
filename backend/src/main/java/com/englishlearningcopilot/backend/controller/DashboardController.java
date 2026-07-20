package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.DashboardCommunityLearningTrendsResponse;
import com.englishlearningcopilot.backend.dto.DashboardStudyPlanResponse;
import com.englishlearningcopilot.backend.dto.DashboardWeeklyOverviewResponse;
import com.englishlearningcopilot.backend.service.DashboardService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/dashboard/community-learning-trends
     * Get community learning leaderboards
    */
    @GetMapping("/community-learning-trends")
    public DashboardCommunityLearningTrendsResponse getCommunityLearningTrends() {
        return dashboardService.getCommunityLearningTrends();
    }

    @GetMapping("/study-plan")
    public DashboardStudyPlanResponse getStudyPlan(Principal principal) {
        return dashboardService.getStudyPlan(principal.getName());
    }

    @GetMapping("/weekly-overview")
    public DashboardWeeklyOverviewResponse getWeeklyOverview(Principal principal) {
        return dashboardService.getWeeklyOverview(principal.getName());
    }
}
