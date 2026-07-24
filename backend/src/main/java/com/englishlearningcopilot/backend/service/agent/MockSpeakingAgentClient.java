package com.englishlearningcopilot.backend.service.agent;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "speaking.agent.provider", havingValue = "mock")
public class MockSpeakingAgentClient implements SpeakingAgentClient {

    @Override
    public SpeakingAgentReply reply(
            SpeakingScenario scenario,
            String selectedTopic,
            List<SpeakingMessage> history,
            String userMessage,
            int turnIndex
    ) {
        String topic = normalizeTopic(selectedTopic);
        if (turnIndex == 0) {
            return SpeakingAgentReply.of(openingMessage(scenario, topic), null);
        }

        String content = switch (scenario.getId()) {
            case "IELTS-P1-practice" -> ieltsPart1Reply(topic, turnIndex);
            case "IELTS-P2-practice" -> ieltsPart2Reply(topic, turnIndex);
            case "IELTS-P3-practice" -> ieltsPart3Reply(topic, turnIndex);
            case "IELTS-mock-test" -> ieltsMockReply(turnIndex);
            case "G-01-airport", "airport-checkin" -> airportReply(turnIndex);
            case "G-02-restaurant" -> restaurantReply(turnIndex);
            case "G-03-hotel" -> hotelReply(turnIndex);
            case "G-04-shopping", "store-return" -> shoppingReply(turnIndex);
            case "G-05-clinic", "clinic-visit" -> clinicReply(turnIndex);
            case "G-06-job-interview" -> jobInterviewReply(turnIndex);
            case "G-07-business-meeting", "business-opening" -> businessReply(turnIndex);
            case "G-08-small-talk", "dinner-smalltalk" -> smallTalkReply(turnIndex);
            case "G-09-presentation" -> presentationReply(turnIndex);
            case "G-10-phone-call" -> phoneCallReply(turnIndex);
            case "G-11-directions" -> directionsReply(turnIndex);
            case "G-12-renting-apartment" -> rentingReply(turnIndex);
            default -> genericReply(scenario, topic, turnIndex);
        };

        return SpeakingAgentReply.of(content, buildTip(userMessage, scenario, topic));
    }

    private String openingMessage(SpeakingScenario scenario, String topic) {
        if ("IELTS-P1-practice".equals(scenario.getId())) {
            return "Let's practise IELTS Speaking Part 1. The topic is " + topic
                    + ". First question: do you like this topic, and why?";
        }
        if ("IELTS-P2-practice".equals(scenario.getId())) {
            return "Let's practise IELTS Speaking Part 2. Your cue card is: " + topic
                    + ". You have one minute to prepare. When you are ready, describe it in detail.";
        }
        if ("IELTS-P3-practice".equals(scenario.getId())) {
            return "Let's practise IELTS Speaking Part 3. The discussion topic is " + topic
                    + ". First question: why is this topic important in modern society?";
        }
        if ("IELTS-mock-test".equals(scenario.getId())) {
            return "This is your IELTS Speaking mock test. Let's begin with Part 1. Do you work or are you a student?";
        }
        return scenario.getOpeningMessage();
    }

    private String ieltsPart1Reply(String topic, int turnIndex) {
        return switch (turnIndex % 4) {
            case 1 -> "How often do you talk about " + topic + " in your daily life?";
            case 2 -> "Has your opinion about " + topic + " changed since you were younger?";
            case 3 -> "Do people in your country usually care about " + topic + "?";
            default -> "Would you like to learn more about " + topic + " in the future?";
        };
    }

    private String ieltsPart2Reply(String topic, int turnIndex) {
        return switch (turnIndex % 4) {
            case 1 -> "Please add one specific detail about " + topic + ": when did it happen, or when did you first notice it?";
            case 2 -> "What made " + topic + " memorable or important to you?";
            case 3 -> "How did you feel at that time, and why?";
            default -> "Please finish with a short conclusion about what you learned from " + topic + ".";
        };
    }

    private String ieltsPart3Reply(String topic, int turnIndex) {
        return switch (turnIndex % 4) {
            case 1 -> "What are the main advantages and disadvantages of " + topic + "?";
            case 2 -> "How might " + topic + " change in the next ten years?";
            case 3 -> "Do you think governments should do more about " + topic + "? Why or why not?";
            default -> "Can you compare young people's and older people's views on " + topic + "?";
        };
    }

    private String ieltsMockReply(int turnIndex) {
        return switch (turnIndex) {
            case 1 -> "Let's stay in Part 1. What do you usually do in your free time?";
            case 2 -> "Do you prefer spending time indoors or outdoors?";
            case 3 -> "Now let's move to Part 2. Describe a place you visited that you would recommend to others.";
            case 4 -> "Please continue your Part 2 answer with more details about who you went with and what you did there.";
            case 5 -> "Thank you. Now Part 3. Why do people like visiting new places?";
            case 6 -> "How has tourism changed compared with the past?";
            default -> "Do you think travel will become more or less important in the future? Give reasons.";
        };
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

    private String restaurantReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Do you have a reservation, or would you like a table for tonight?";
            case 2 -> "Are there any foods you cannot eat, such as nuts, seafood, or dairy?";
            default -> "Would you like to ask for the bill, or order dessert first?";
        };
    }

    private String hotelReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "May I confirm your booking name and the number of nights you will stay?";
            case 2 -> "Would you like to ask about breakfast, Wi-Fi, or the fitness center?";
            default -> "If there is a problem with the room, how would you politely explain it to reception?";
        };
    }

    private String shoppingReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "What size or color are you looking for?";
            case 2 -> "Would you like to ask whether there is a discount today?";
            default -> "Please explain why you want to return or exchange this item.";
        };
    }

    private String clinicReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "I see. Could you describe when the symptoms started and how serious they feel?";
            case 2 -> "Thank you. Are you taking any medicine or do you have any allergies?";
            default -> "Please repeat the prescription instructions to make sure everything is clear.";
        };
    }

    private String jobInterviewReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Could you tell me about a project or experience that shows your strengths?";
            case 2 -> "What challenge did you face, and how did you handle it?";
            default -> "Why are you interested in this role and this company?";
        };
    }

    private String smallTalkReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Nice. What kind of activities do you usually enjoy on weekends?";
            case 2 -> "That sounds interesting. Could you ask me one follow-up question?";
            default -> "Good follow-up. How would you politely end this conversation?";
        };
    }

    private String presentationReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Please introduce the main point of your presentation in one sentence.";
            case 2 -> "Could you explain one piece of data or evidence that supports your point?";
            default -> "Now answer a question from the audience: why should we care about this topic?";
        };
    }

    private String phoneCallReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Thanks for calling. Could you explain the reason for your call?";
            case 2 -> "Could you repeat your phone number and email address slowly?";
            default -> "The person is not available now. Would you like to leave a message?";
        };
    }

    private String directionsReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "Where would you like to go, and are you walking or taking public transport?";
            case 2 -> "Please confirm the directions: turn left, cross the street, then go straight.";
            default -> "If you get lost, how would you ask someone to explain the route again?";
        };
    }

    private String rentingReply(int turnIndex) {
        return switch (turnIndex % 3) {
            case 1 -> "What kind of apartment are you looking for, and what is your budget?";
            case 2 -> "Would you like to ask about the deposit, lease length, or utilities?";
            default -> "There is a maintenance problem. How would you report it to the landlord?";
        };
    }

    private String genericReply(SpeakingScenario scenario, String topic, int turnIndex) {
        if (!topic.isBlank()) {
            return "Let's continue with " + topic + ". Could you add one clear example?";
        }
        return "Let's continue the " + scenario.getTitle() + " practice. Could you answer with one specific detail?";
    }

    private String buildTip(String userMessage, SpeakingScenario scenario, String topic) {
        if (userMessage == null || userMessage.length() < 20) {
            return "Try expanding your answer with one reason or example so it sounds more complete.";
        }
        if (scenario.getId().startsWith("IELTS")) {
            return "IELTS answers are stronger when you answer directly, then add a reason and a short example.";
        }
        if (!topic.isBlank()) {
            return "Keep your answer connected to the selected topic: " + topic + ".";
        }
        return "Good response. Keep using scenario keywords such as " + scenario.getKeywords() + " where they fit naturally.";
    }

    private String normalizeTopic(String selectedTopic) {
        return selectedTopic == null || selectedTopic.isBlank() ? "General English" : selectedTopic.trim();
    }
}
