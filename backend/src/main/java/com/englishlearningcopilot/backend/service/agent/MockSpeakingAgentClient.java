package com.englishlearningcopilot.backend.service.agent;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockSpeakingAgentClient implements SpeakingAgentClient {

    @Override
    public SpeakingAgentReply reply(
            SpeakingScenario scenario,
            List<SpeakingMessage> history,
            String userMessage,
            int turnIndex
    ) {
        if (turnIndex == 0) {
            return new SpeakingAgentReply(scenario.getOpeningMessage(), null);
        }

        String content = switch (scenario.getId()) {
            case "airport-checkin" -> airportReply(turnIndex);
            case "dinner-smalltalk" -> dinnerReply(turnIndex);
            case "clinic-visit" -> clinicReply(turnIndex);
            default -> businessReply(turnIndex);
        };

        String tip = buildTip(userMessage, scenario);
        return new SpeakingAgentReply(content, tip);
    }

    private String businessReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Thanks for clarifying. Could you also explain the main risk we should discuss today?";
            case 2 -> "That sounds reasonable. How would you confirm the next milestone with the team?";
            default -> "Good. Please summarize the action items and who will follow up after the meeting.";
        };
    }

    private String airportReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Thank you. Do you have any bags to check today?";
            case 2 -> "I can help with that. Would you prefer a window seat or an aisle seat?";
            default -> "Your boarding pass is ready. Could you confirm whether you need to reschedule any flight?";
        };
    }

    private String dinnerReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Nice choice. What kind of food do you usually enjoy when you eat out?";
            case 2 -> "That sounds great. How would you recommend a dish to someone politely?";
            default -> "Good follow-up. Could you ask one more natural question to keep the conversation going?";
        };
    }

    private String clinicReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "I see. Could you describe when the symptoms started and how serious they feel?";
            case 2 -> "Thank you. Are you taking any medicine or do you have any allergies?";
            default -> "Please repeat the prescription instructions to make sure everything is clear.";
        };
    }

    private String buildTip(String userMessage, SpeakingScenario scenario) {
        if (userMessage.length() < 20) {
            return "Try expanding your answer with one reason or detail so the conversation feels more natural.";
        }
        if (!userMessage.endsWith(".") && !userMessage.endsWith("?") && !userMessage.endsWith("!")) {
            return "Your meaning is clear. Add sentence-ending punctuation when typing practice responses.";
        }
        return "Good response. Keep using scenario keywords such as " + scenario.getKeywords() + " where they fit naturally.";
    }
}
