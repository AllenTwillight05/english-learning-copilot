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

class XfyunIseClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XfyunIseClient client = new XfyunIseClient(
            objectMapper,
            new XfyunIseResultParser(objectMapper)
    );

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
        assertThat(uri.getHost()).isEqualTo("ise-api.xfyun.cn");
        assertThat(uri.getPath()).isEqualTo("/v2/open-ise");
        assertThat(query.get("host")).isEqualTo("ise-api.xfyun.cn");
        assertThat(query.get("date")).contains("Tue, 21 Jul 2026");
        assertThat(authorization).contains("api_key=\"api-key\"");
        assertThat(authorization).contains("algorithm=\"hmac-sha256\"");
    }

    @Test
    void buildsStartPayloadWithReferenceText() throws Exception {
        String payload = client.toStartPayload("Hello, nice to meet you.", properties());

        JsonNode root = objectMapper.readTree(payload);
        assertThat(root.path("common").path("app_id").asText()).isEqualTo("app-id");
        assertThat(root.path("business").path("sub").asText()).isEqualTo("ise");
        assertThat(root.path("business").path("ent").asText()).isEqualTo("en_vip");
        assertThat(root.path("business").path("category").asText()).isEqualTo("read_sentence");
        assertThat(root.path("business").path("auf").asText()).isEqualTo("audio/L16;rate=16000");
        assertThat(root.path("business").path("aue").asText()).isEqualTo("raw");

        String text = root.path("business").path("text").asText();
        assertThat(text).isEqualTo("\uFEFF[content]\nHello, nice to meet you.");
        assertThat(root.path("business").path("rst").asText()).isEqualTo("entirety");
        assertThat(root.path("business").path("ttp_skip").asBoolean()).isTrue();
    }

    @Test
    void buildsAudioPayload() throws Exception {
        String payload = client.toAudioPayload(new byte[] {1, 2, 3}, 2, 4);

        JsonNode root = objectMapper.readTree(payload);
        assertThat(root.path("business").path("cmd").asText()).isEqualTo("auw");
        assertThat(root.path("business").path("aus").asInt()).isEqualTo(4);
        assertThat(root.path("data").path("status").asInt()).isEqualTo(2);
        assertThat(Base64.getDecoder().decode(root.path("data").path("data").asText()))
                .containsExactly(1, 2, 3);
    }

    private Map<String, String> parseQuery(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                ));
    }

    private XfyunIseProperties properties() {
        return new XfyunIseProperties(
                true,
                "wss://ise-api.xfyun.cn/v2/open-ise",
                "app-id",
                "api-key",
                "api-secret",
                "en_vip",
                "read_sentence",
                16000,
                30000,
                1280,
                true,
                "ffmpeg"
        );
    }
}
