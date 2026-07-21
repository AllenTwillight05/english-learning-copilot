package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.DailyLearningStatusResponse;
import com.englishlearningcopilot.backend.dto.LearningPlanRequest;
import com.englishlearningcopilot.backend.dto.LearningPlanResponse;
import com.englishlearningcopilot.backend.dto.ProfileSnapshotResponse;
import com.englishlearningcopilot.backend.service.LearningPlanService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final LearningPlanService learningPlanService;

    public ProfileController(LearningPlanService learningPlanService) {
        this.learningPlanService = learningPlanService;
    }

    @GetMapping("/snapshot")
    public ProfileSnapshotResponse getSnapshot(Principal principal) {
        return learningPlanService.getProfileSnapshot(principal.getName());
    }

    @GetMapping("/learning-plan")
    public LearningPlanResponse getLearningPlan(Principal principal) {
        return learningPlanService.getLearningPlan(principal.getName());
    }

    @PostMapping("/learning-plan")
    public LearningPlanResponse updateLearningPlan(
            Principal principal,
            @Valid @RequestBody LearningPlanRequest request
    ) {
        return learningPlanService.updateLearningPlan(principal.getName(), request);
    }

    @GetMapping("/daily-status")
    public DailyLearningStatusResponse getDailyStatus(Principal principal) {
        return learningPlanService.getDailyStatus(principal.getName());
    }
}
