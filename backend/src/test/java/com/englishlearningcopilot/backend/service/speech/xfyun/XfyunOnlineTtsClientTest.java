package com.englishlearningcopilot.backend.service.speech.xfyun;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class XfyunOnlineTtsClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XfyunOnlineTtsClient client = new XfyunOnlineTtsClient(objectMapper);

    @Test
    void buildsSingleShotTtsPayload() throws Exception {
        String payload = client.toRequestPayload("Welcome to the meeting.", properties());

        JsonNode root = objectMapper.readTree(payload);
        assertThat(root.path("common").path("app_id").asText()).isEqualTo("app-id");
        assertThat(root.path("business").path("aue").asText()).isEqualTo("lame");
        assertThat(root.path("business").path("sfl").asInt()).isEqualTo(1);
        assertThat(root.path("business").path("vcn").asText()).isEqualTo("xiaoyan");
        assertThat(root.path("data").path("status").asInt()).isEqualTo(2);

        String encodedText = root.path("data").path("text").asText();
        String decodedText = new String(Base64.getDecoder().decode(encodedText), StandardCharsets.UTF_8);
        assertThat(decodedText).isEqualTo("Welcome to the meeting.");
    }

    @Test
    void buildsAuthenticatedWebSocketUri() {
        URI uri = client.buildAuthenticatedUri(
                properties(),
                ZonedDateTime.parse("2026-07-21T06:00:00Z")
        );

        Map<String, String> query = parseQuery(uri);
        String authorization = new String(
                Base64.getDecoder().decode(query.get("authorization")),
                StandardCharsets.UTF_8
        );

        assertThat(uri.getScheme()).isEqualTo("wss");
        assertThat(uri.getHost()).isEqualTo("tts-api.xfyun.cn");
        assertThat(uri.getPath()).isEqualTo("/v2/tts");
        assertThat(query.get("host")).isEqualTo("tts-api.xfyun.cn");
        assertThat(query.get("date")).contains("Tue, 21 Jul 2026");
        assertThat(authorization).contains("api_key=\"api-key\"");
        assertThat(authorization).contains("algorithm=\"hmac-sha256\"");
        assertThat(authorization).contains("headers=\"host date request-line\"");
        assertThat(authorization).contains("signature=\"");
    }

    private Map<String, String> parseQuery(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                ));
    }

    private XfyunOnlineTtsProperties properties() {
        return new XfyunOnlineTtsProperties(
                true,
                "wss://tts-api.xfyun.cn/v2/tts",
                "app-id",
                "api-key",
                "api-secret",
                "xiaoyan",
                "lame",
                "utf8",
                50,
                50,
                50,
                30000
        );
    }
}
