package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.dto.CreateSpeakingMessageRequest;
import com.englishlearningcopilot.backend.dto.CreateSpeakingSessionRequest;
import com.englishlearningcopilot.backend.dto.SpeakingScenarioResponse;
import com.englishlearningcopilot.backend.dto.SpeakingSessionResponse;
import com.englishlearningcopilot.backend.dto.SpeakingTurnResponse;
import java.util.List;

public interface SpeakingService {

    List<SpeakingScenarioResponse> listScenarios();

    SpeakingScenarioResponse getScenario(String scenarioId);

    SpeakingSessionResponse createSession(String username, CreateSpeakingSessionRequest request);

    SpeakingSessionResponse getSession(String username, Long sessionId);

    List<SpeakingSessionResponse> listHistory(String username);

    SpeakingTurnResponse addMessage(String username, Long sessionId, CreateSpeakingMessageRequest request);
}
