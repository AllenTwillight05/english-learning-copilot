package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.MessageResponse;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteRequest;
import com.englishlearningcopilot.backend.dto.VocabularyFavoriteResponse;
import com.englishlearningcopilot.backend.dto.VocabularyPracticeWordResponse;
import com.englishlearningcopilot.backend.dto.VocabularyRatingRequest;
import com.englishlearningcopilot.backend.dto.VocabularyWordbookWordResponse;
import com.englishlearningcopilot.backend.service.VocabularyService;
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
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * GET /api/vocabulary/practice-words
     * Get random vocabulary practice words excluding current user's wordbook
    */
    @GetMapping("/practice-words")
    public List<VocabularyPracticeWordResponse> getPracticeWords(
            Principal principal,
            @RequestParam(defaultValue = "starter") String level
    ) {
        String username = principal == null ? null : principal.getName();
        return vocabularyService.getPracticeWords(username, level);
    }

    /**
     * GET /api/vocabulary/wordbook-words
     * Get current user's vocabulary wordbook words
    */
    @GetMapping("/wordbook-words")
    public List<VocabularyWordbookWordResponse> getWordbookWords(Principal principal) {
        String username = principal == null ? null : principal.getName();
        return vocabularyService.getWordbookWords(username);
    }

    /**
     * POST /api/vocabulary/practice-ratings
     * Submit vocabulary practice self rating
    */
    @PostMapping("/practice-ratings")
    public MessageResponse submitRating(Principal principal, @Valid @RequestBody VocabularyRatingRequest request) {
        vocabularyService.submitRating(principal.getName(), request);
        return new MessageResponse("Vocabulary rating received.");
    }

    /**
     * POST /api/vocabulary/wordbook-favorites
     * Toggle current user's vocabulary favorite status
    */
    @PostMapping("/wordbook-favorites")
    public VocabularyFavoriteResponse toggleFavorite(
            Principal principal,
            @Valid @RequestBody VocabularyFavoriteRequest request
    ) {
        return vocabularyService.toggleFavorite(principal.getName(), request);
    }
}
