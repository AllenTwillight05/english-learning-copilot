package com.englishlearningcopilot.backend.service.agent;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import java.util.List;

public interface SpeakingAgentClient {

    SpeakingAgentReply reply(SpeakingScenario scenario, List<SpeakingMessage> history, String userMessage, int turnIndex);
}
