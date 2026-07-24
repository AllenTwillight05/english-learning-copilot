package com.englishlearningcopilot.backend.service.agent;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "speaking.agent.provider", havingValue = "sjtu", matchIfMissing = true)
@EnableConfigurationProperties(SjtuSpeakingAgentProperties.class)
public class SjtuDeepSeekSpeakingAgentClient implements SpeakingAgentClient {

    private static final Logger log = LoggerFactory.getLogger(SjtuDeepSeekSpeakingAgentClient.class);
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{([A-Z_]+)}}");

    private final SjtuSpeakingAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final MockSpeakingAgentClient fallbackAgent;

    public SjtuDeepSeekSpeakingAgentClient(
            SjtuSpeakingAgentProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .build();
        this.fallbackAgent = new MockSpeakingAgentClient();
    }

    @Override
    public SpeakingAgentReply reply(
            SpeakingScenario scenario,
            String selectedTopic,
            List<SpeakingMessage> history,
            String userMessage,
            int turnIndex
    ) {
        if (!properties.hasApiKey()) {
            log.warn("SJTU_AI_API_KEY is not configured. Falling back to mock speaking agent.");
            return fallbackAgent.reply(scenario, selectedTopic, history, userMessage, turnIndex);
        }

        try {
            List<Map<String, String>> messages = buildMessages(scenario, selectedTopic, history, userMessage, turnIndex);
            String content = callChatCompletions(messages);
            return parseReply(content);
        } catch (RuntimeException | IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("SJTU DeepSeek speaking agent failed. Falling back to mock speaking agent.", e);
            return fallbackAgent.reply(scenario, selectedTopic, history, userMessage, turnIndex);
        }
    }

    private List<Map<String, String>> buildMessages(
            SpeakingScenario scenario,
            String selectedTopic,
            List<SpeakingMessage> history,
            String userMessage,
            int turnIndex
    ) throws IOException {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(scenario, selectedTopic, turnIndex)));

        for (SpeakingMessage message : history) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            messages.add(Map.of(
                    "role", message.getSender() == SpeakingMessageSender.USER ? "user" : "assistant",
                    "content", message.getContent()
            ));
        }

        if (turnIndex == 0) {
            messages.add(Map.of("role", "user", "content", "Start the speaking session now."));
        } else if (userMessage != null && !userMessage.isBlank()) {
            messages.add(Map.of("role", "user", "content", userMessage));
        }
        return messages;
    }

    private String buildSystemPrompt(SpeakingScenario scenario, String selectedTopic, int turnIndex) throws IOException {
        String labPrompt = loadLabSystemPrompt(scenario.getId());
        String topic = selectedTopic == null || selectedTopic.isBlank() ? "Not selected." : selectedTopic.trim();

        return """
                %s

                Runtime context from the production backend:
                - Selected topic or cue card: %s
                - Current turn index: %d

                Response contract for the production backend:
                Return only one JSON object. Do not wrap it in Markdown.
                JSON shape:
                {"content":"The next examiner or role-play line shown in the chat UI.","spokenText":"Natural English text for TTS.","instantTip":"A concise teaching note for the learner, or null."}

                Rules for the JSON fields:
                - content must be only the next examiner or role-play line. Do not put corrections, translations, or explanations in content.
                - spokenText must be only the natural English line that should be read aloud. It is usually the same as content.
                - instantTip is where brief teaching, correction, explanation, or "if you don't know how to answer" help goes.
                - Keep spokenText concise, conversational, and suitable for direct text-to-speech.
                - Use null for instantTip when no immediate teaching note is needed, especially during IELTS mock test flow.
                """.formatted(labPrompt, topic, turnIndex);
    }

    private String loadLabSystemPrompt(String scenarioId) throws IOException {
        Path root = resolvePromptLabRoot();
        JsonNode scenario = objectMapper.readTree(Files.readString(root.resolve("scenarios").resolve(scenarioId + ".json")));
        String commonFile = scenarioId.startsWith("IELTS") ? "ielts-core.md" : "roleplay-core.md";
        String promptFile = switch (scenarioId) {
            case "IELTS-P1-practice" -> "IELTS-P1-practice-system.md";
            case "IELTS-P2-practice" -> "IELTS-P2-practice-system.md";
            case "IELTS-P3-practice" -> "IELTS-P3-practice-system.md";
            case "IELTS-mock-test" -> "IELTS-mock-test-system.md";
            default -> scenarioId + "-system.md";
        };

        String template = Files.readString(root.resolve("common").resolve(commonFile))
                + "\n\n"
                + Files.readString(root.resolve("prompts").resolve(promptFile));
        return renderTemplate(template, scenario);
    }

    private Path resolvePromptLabRoot() {
        Path configured = Path.of(properties.promptLabDir());
        if (Files.isDirectory(configured)) {
            return configured;
        }

        Path parent = Path.of("..").resolve(properties.promptLabDir()).normalize();
        if (Files.isDirectory(parent)) {
            return parent;
        }

        throw new IllegalStateException("Prompt lab directory was not found: " + properties.promptLabDir());
    }

    private String renderTemplate(String template, JsonNode scenario) {
        Matcher matcher = TEMPLATE_TOKEN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(templateValue(matcher.group(1), scenario)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String templateValue(String key, JsonNode scenario) {
        return switch (key) {
            case "SCENARIO_ID" -> text(scenario, "id");
            case "TITLE" -> text(scenario, "title");
            case "LEVEL" -> text(scenario, "level");
            case "LEARNER_ROLE" -> text(scenario, "learnerRole");
            case "AGENT_ROLE" -> text(scenario, "agentRole");
            case "GOAL" -> text(scenario, "goal");
            case "TARGET_TURNS" -> text(scenario, "targetTurns");
            case "KEYWORDS" -> joinTextArray(scenario.path("keywords"), ", ");
            case "OPENING_MESSAGE" -> text(scenario, "openingMessage");
            case "SAMPLE_DIALOGUE" -> formatDialogue(scenario.path("sampleDialogue"));
            case "CONVERSATION_FLOW" -> formatStringList(scenario.path("conversationFlow"));
            case "STATE_RULES" -> formatStringList(scenario.path("stateRules"));
            case "LEVEL_ADAPTATION" -> formatStringList(scenario.path("levelAdaptation"));
            case "ERROR_HANDLING" -> formatStringList(scenario.path("errorHandling"));
            case "EXPRESSION_HELP" -> formatExpressionHelp(scenario.path("expressionHelp"));
            case "SOURCE_EXAMPLES" -> formatNamedDialogues(scenario.path("sourceExamples"));
            case "TEST_INPUTS" -> formatStringList(scenario.path("testInputs"));
            default -> "";
        };
    }

    private String callChatCompletions(List<Map<String, String>> messages) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
                "model", properties.model(),
                "messages", messages,
                "temperature", properties.temperature(),
                "max_tokens", properties.maxTokens()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.endpoint() + "/chat/completions"))
                .timeout(Duration.ofMillis(properties.timeoutMs()))
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode body = parseJsonOrNull(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = body != null
                    ? body.path("error").path("message").asText(body.path("message").asText(response.body()))
                    : response.body();
            throw new IllegalStateException("SJTU API request failed (" + response.statusCode() + "): " + message);
        }

        String content = body == null ? "" : body.path("choices").path(0).path("message").path("content").asText("").trim();
        if (content.isBlank()) {
            throw new IllegalStateException("SJTU API response did not include choices[0].message.content.");
        }
        return content;
    }

    private SpeakingAgentReply parseReply(String rawContent) {
        String normalized = stripCodeFence(rawContent);
        JsonNode root = parseJsonOrNull(normalized);
        if (root == null || !root.isObject()) {
            return SpeakingAgentReply.of(normalized, null);
        }

        String content = root.path("content").asText("").trim();
        String spokenText = root.path("spokenText").asText("").trim();
        String instantTip = root.hasNonNull("instantTip") ? root.path("instantTip").asText("").trim() : null;
        if (content.isBlank()) {
            content = spokenText;
        }
        if (spokenText.isBlank()) {
            spokenText = content;
        }
        if (instantTip != null && instantTip.isBlank()) {
            instantTip = null;
        }
        if (content.isBlank()) {
            return SpeakingAgentReply.of(normalized, null);
        }
        return new SpeakingAgentReply(content, spokenText, instantTip);
    }

    private JsonNode parseJsonOrNull(String text) {
        try {
            return text == null || text.isBlank() ? null : objectMapper.readTree(text);
        } catch (IOException e) {
            return null;
        }
    }

    private String stripCodeFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
            return trimmed.substring(firstLineEnd + 1, lastFence).trim();
        }
        return trimmed;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private String joinTextArray(JsonNode array, String delimiter) {
        if (!array.isArray() || array.isEmpty()) {
            return "Not specified.";
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : array) {
            items.add(item.asText());
        }
        return String.join(delimiter, items);
    }

    private String formatStringList(JsonNode array) {
        if (!array.isArray() || array.isEmpty()) {
            return "Not specified.";
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : array) {
            items.add("- " + item.asText());
        }
        return String.join("\n", items);
    }

    private String formatDialogue(JsonNode dialogue) {
        if (!dialogue.isArray() || dialogue.isEmpty()) {
            return "Not specified.";
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode turn : dialogue) {
            lines.add(text(turn, "speaker") + ": " + text(turn, "text"));
        }
        return String.join("\n", lines);
    }

    private String formatNamedDialogues(JsonNode dialogues) {
        if (!dialogues.isArray() || dialogues.isEmpty()) {
            return "Not specified.";
        }
        List<String> items = new ArrayList<>();
        for (JsonNode dialogue : dialogues) {
            String title = text(dialogue, "title");
            String body = formatDialogue(dialogue.path("turns"));
            items.add(title.isBlank() ? body : title + "\n" + body);
        }
        return String.join("\n\n", items);
    }

    private String formatExpressionHelp(JsonNode expressions) {
        if (!expressions.isArray() || expressions.isEmpty()) {
            return "Not specified.";
        }

        List<String> blocks = new ArrayList<>();
        for (JsonNode item : expressions) {
            List<String> lines = new ArrayList<>();
            lines.add("Intent: " + text(item, "intent"));
            JsonNode triggers = item.path("triggers");
            if (triggers.isArray() && !triggers.isEmpty()) {
                lines.add("Triggers: " + joinTextArray(triggers, ", "));
            }
            lines.add("Suggested phrase: " + text(item, "phrase"));
            String explanation = text(item, "explanation");
            if (!explanation.isBlank()) {
                lines.add("Explanation: " + explanation);
            }
            blocks.add(String.join("\n", lines));
        }
        return String.join("\n\n", blocks);
    }
}
