package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.GrammarFavoriteRequest;
import com.englishlearningcopilot.backend.dto.DailyPracticeProgressResponse;
import com.englishlearningcopilot.backend.dto.GrammarFavoriteResponse;
import com.englishlearningcopilot.backend.dto.GrammarNotebookQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarOverviewResponse;
import com.englishlearningcopilot.backend.dto.GrammarPracticeResultRequest;
import com.englishlearningcopilot.backend.dto.GrammarPracticeQuestionResponse;
import com.englishlearningcopilot.backend.dto.GrammarRatingRequest;
import com.englishlearningcopilot.backend.dto.GrammarTopicResponse;
import com.englishlearningcopilot.backend.dto.MessageResponse;
import com.englishlearningcopilot.backend.service.GrammarService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grammar")
public class GrammarController {

    private final GrammarService grammarService;

    public GrammarController(GrammarService grammarService) {
        this.grammarService = grammarService;
    }

    /**
     * GET /api/grammar/overview
     * Get the current user's grammar mastery summary
     */
    @GetMapping("/overview")
    public GrammarOverviewResponse getOverview(Principal principal) {
        return grammarService.getOverview(getUsername(principal));
    }

    /**
     * GET /api/grammar/topics
     * Get available grammar topics with current user's progress
     */
    @GetMapping("/topics")
    public List<GrammarTopicResponse> getTopics(Principal principal) {
        return grammarService.getTopics(getUsername(principal));
    }

    /**
     * GET /api/grammar/practice-questions?category={category}
     * Get three random questions in the selected category that the current user has not practiced
     */
    @GetMapping("/practice-questions")
    public List<GrammarPracticeQuestionResponse> getPracticeQuestions(
            Principal principal,
            @RequestParam String category
    ) {
        return grammarService.getPracticeQuestions(principal.getName(), category);
    }

    @GetMapping("/progress")
    public DailyPracticeProgressResponse getProgress(Principal principal) {
        return grammarService.getProgress(principal.getName());
    }

    /**
     * GET /api/grammar/review-grammar
     * Get the current user's due grammar review questions
     */
    @GetMapping("/review-grammar")
    public List<GrammarPracticeQuestionResponse> getReviewQuestions(Principal principal) {
        return grammarService.getReviewQuestions(principal.getName());
    }

    /**
     * GET /api/grammar/notebook-questions
     * Get the current user's incorrect or favorited grammar questions
     */
    @GetMapping("/notebook-questions")
    public List<GrammarNotebookQuestionResponse> getNotebookQuestions(Principal principal) {
        String username = principal == null ? null : principal.getName();
        return grammarService.getNotebookQuestions(username);
    }

    /**
     * POST /api/grammar/practice-results
     * Record whether the current user answered a grammar question incorrectly
     */
    @PostMapping("/practice-results")
    public MessageResponse submitPracticeResult(
            Principal principal,
            @Valid @RequestBody GrammarPracticeResultRequest request
    ) {
        grammarService.submitPracticeResult(principal.getName(), request);
        return new MessageResponse("Grammar practice result received.");
    }

    /**
     * POST /api/grammar/practice-ratings
     * Submit grammar practice self rating and update FSRS review progress
     */
    @PostMapping("/practice-ratings")
    public MessageResponse submitRating(
            Principal principal,
            @Valid @RequestBody GrammarRatingRequest request
    ) {
        grammarService.submitRating(principal.getName(), request);
        return new MessageResponse("Grammar rating received.");
    }

    /**
     * POST /api/grammar/notebook-favorites
     * Toggle the current user's grammar question favorite status
     */
    @PostMapping("/notebook-favorites")
    public GrammarFavoriteResponse toggleFavorite(
            Principal principal,
            @Valid @RequestBody GrammarFavoriteRequest request
    ) {
        return grammarService.toggleFavorite(principal.getName(), request);
    }

    private String getUsername(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
