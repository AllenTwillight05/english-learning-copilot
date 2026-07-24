package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.CreateSpeakingSessionRequest;
import com.englishlearningcopilot.backend.dto.SpeakingFeedbackResponse;
import com.englishlearningcopilot.backend.dto.SpeakingScenarioResponse;
import com.englishlearningcopilot.backend.dto.SpeakingSessionResponse;
import com.englishlearningcopilot.backend.dto.SpeakingTurnResponse;
import com.englishlearningcopilot.backend.service.SpeakingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/speaking")
public class SpeakingController {

    private final SpeakingService speakingService;

    public SpeakingController(SpeakingService speakingService) {
        this.speakingService = speakingService;
    }

    /**
     * GET /api/speaking/scenarios
     * Get all speaking scenarios
     */
    @GetMapping("/scenarios")
    public List<SpeakingScenarioResponse> listScenarios() {
        return speakingService.listScenarios();
    }

    /**
     * GET /api/speaking/scenarios/{scenarioId}
     * Get a speaking scenario
     */
    @GetMapping("/scenarios/{scenarioId}")
    public SpeakingScenarioResponse getScenario(@PathVariable String scenarioId) {
        return speakingService.getScenario(scenarioId);
    }

    /**
     * POST /api/speaking/sessions
     * Create a speaking session
     */
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SpeakingSessionResponse createSession(
            Principal principal,
            @Valid @RequestBody CreateSpeakingSessionRequest request
    ) {
        return speakingService.createSession(principal.getName(), request);
    }

    /**
     * GET /api/speaking/sessions/{sessionId}
     * Get a speaking session
     */
    @GetMapping("/sessions/{sessionId}")
    public SpeakingSessionResponse getSession(Principal principal, @PathVariable Long sessionId) {
        return speakingService.getSession(principal.getName(), sessionId);
    }

    /**
     * GET /api/speaking/history
     * Get speaking session history
     */
    @GetMapping("/history")
    public List<SpeakingSessionResponse> listHistory(Principal principal) {
        return speakingService.listHistory(principal.getName());
    }

    /**
     * POST /api/speaking/sessions/{sessionId}/messages (multipart)
     * Submit a voice recording. Backend runs ASR, then calls the reply agent
     * and Super Smart TTS. Pronunciation scoring is deferred to feedback.
     */
    @PostMapping(value = "/sessions/{sessionId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpeakingTurnResponse submitRecording(
            Principal principal,
            @PathVariable Long sessionId,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "durationMs", required = false) Long durationMs
    ) {
        return speakingService.submitRecording(principal.getName(), sessionId, audio, durationMs);
    }

    /**
     * GET /api/speaking/sessions/{sessionId}/feedback
     * Get feedback for a speaking session
     */
    @GetMapping("/sessions/{sessionId}/feedback")
    public SpeakingFeedbackResponse getFeedback(Principal principal, @PathVariable Long sessionId) {
        return speakingService.getFeedback(principal.getName(), sessionId);
    }
}
