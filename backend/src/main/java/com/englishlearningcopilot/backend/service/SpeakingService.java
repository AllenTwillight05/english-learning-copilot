package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.CreateSpeakingSessionRequest;
import com.englishlearningcopilot.backend.dto.SpeakingFeedbackResponse;
import com.englishlearningcopilot.backend.dto.SpeakingScenarioResponse;
import com.englishlearningcopilot.backend.dto.SpeakingSessionResponse;
import com.englishlearningcopilot.backend.dto.SpeakingTurnResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface SpeakingService {

    List<SpeakingScenarioResponse> listScenarios();

    SpeakingScenarioResponse getScenario(String scenarioId);

    SpeakingSessionResponse createSession(String username, CreateSpeakingSessionRequest request);

    SpeakingSessionResponse getSession(String username, Long sessionId);

    List<SpeakingSessionResponse> listHistory(String username);

    SpeakingTurnResponse submitRecording(String username, Long sessionId, MultipartFile audio);

    SpeakingFeedbackResponse getFeedback(String username, Long sessionId);
}
