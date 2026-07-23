package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class XfyunIseClient {

    private static final DateTimeFormatter RFC1123 =
            DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);
    private static final int MAX_REFERENCE_TEXT_BYTES = 1024;

    private final ObjectMapper objectMapper;
    private final XfyunIseResultParser parser;

    public XfyunIseClient(ObjectMapper objectMapper, XfyunIseResultParser parser) {
        this.objectMapper = objectMapper;
        this.parser = parser;
    }

    public PronunciationScore evaluate(byte[] pcmAudio, String referenceText, XfyunIseProperties properties) {
        validate(properties, referenceText);

        URI uri = buildAuthenticatedUri(properties, ZonedDateTime.now(ZoneOffset.UTC));
        IseWebSocketListener listener = new IseWebSocketListener(objectMapper);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .build();

        WebSocket webSocket = null;
        try {
            webSocket = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofMillis(properties.timeoutMs()))
                    .buildAsync(uri, listener)
                    .get(properties.timeoutMs(), TimeUnit.MILLISECONDS);
            webSocket.sendText(toStartPayload(referenceText, properties), true).join();
            sendAudio(webSocket, pcmAudio, properties);
            String response = listener.await(properties.timeoutMs());
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            return parser.parse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XfyunIseException("XFYUN ISE request was interrupted.", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof XfyunIseException iseException) {
                throw iseException;
            }
            throw new XfyunIseException("Failed to call XFYUN ISE service.", e.getCause());
        } catch (TimeoutException e) {
            throw new XfyunIseException("XFYUN ISE request timed out.", e);
        } finally {
            if (webSocket != null && !listener.isDone()) {
                webSocket.abort();
            }
        }
    }

    URI buildAuthenticatedUri(XfyunIseProperties properties, ZonedDateTime dateTime) {
        validateCredentials(properties);
        URI baseUri = URI.create(properties.url());
        if (!"wss".equalsIgnoreCase(baseUri.getScheme())) {
            throw new XfyunIseException("XFYUN ISE URL must use wss.");
        }

        String host = toSignedHost(baseUri);
        String path = baseUri.getRawPath() == null || baseUri.getRawPath().isBlank()
                ? "/v2/open-ise"
                : baseUri.getRawPath();
        String date = RFC1123.format(dateTime.withZoneSameInstant(ZoneOffset.UTC));
        String signatureSource = "host: " + host + "\n"
                + "date: " + date + "\n"
                + "GET " + path + " HTTP/1.1";
        String signature = hmacSha256(signatureSource, properties.apiSecret());
        String authorization = Base64.getEncoder().encodeToString((
                "api_key=\"" + properties.apiKey() + "\", "
                        + "algorithm=\"hmac-sha256\", "
                        + "headers=\"host date request-line\", "
                        + "signature=\"" + signature + "\""
        ).getBytes(StandardCharsets.UTF_8));

        String query = "authorization=" + urlEncode(authorization)
                + "&date=" + urlEncode(date)
                + "&host=" + urlEncode(host);
        return URI.create(baseUri.getScheme() + "://" + baseUri.getRawAuthority() + path + "?" + query);
    }

    String toStartPayload(String referenceText, XfyunIseProperties properties) {
        validate(properties, referenceText);

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode common = root.putObject("common");
        common.put("app_id", properties.appId());

        ObjectNode business = root.putObject("business");
        business.put("sub", "ise");
        business.put("ent", properties.language());
        business.put("category", properties.category());
        business.put("cmd", "ssb");
        business.put("auf", properties.audioFormat());
        business.put("aue", "raw");
        business.put("rstcd", "utf8");
        business.put("rst", "entirety");
        business.put("ise_unite", "1");
        business.put("plev", "0");
        business.put("ttp_skip", true);
        business.put("extra_ability", "multi_dimension");
        business.put("tte", "utf-8");
        business.put("text", toIseText(referenceText));

        ObjectNode data = root.putObject("data");
        data.put("status", 0);
        data.put("data", "");

        try {
            return objectMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new XfyunIseException("Failed to build XFYUN ISE start payload.", e);
        }
    }

    String toAudioPayload(byte[] chunk, int dataStatus, int audioStatus) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode business = root.putObject("business");
        business.put("cmd", "auw");
        business.put("aus", audioStatus);
        business.put("aue", "raw");

        ObjectNode data = root.putObject("data");
        data.put("status", dataStatus);
        data.put("data", Base64.getEncoder().encodeToString(chunk));

        try {
            return objectMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new XfyunIseException("Failed to build XFYUN ISE audio payload.", e);
        }
    }

    private void sendAudio(WebSocket webSocket, byte[] pcmAudio, XfyunIseProperties properties)
            throws InterruptedException {
        if (pcmAudio == null || pcmAudio.length == 0) {
            throw new XfyunIseException("XFYUN ISE audio is empty.");
        }
        int frameSize = properties.audioFrameBytes();
        for (int offset = 0; offset < pcmAudio.length; offset += frameSize) {
            int length = Math.min(frameSize, pcmAudio.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(pcmAudio, offset, chunk, 0, length);
            boolean first = offset == 0;
            boolean last = offset + length >= pcmAudio.length;
            int audioStatus = last ? 4 : first ? 1 : 2;
            int dataStatus = last ? 2 : 1;
            webSocket.sendText(toAudioPayload(chunk, dataStatus, audioStatus), true).join();
            if (!last) {
                Thread.sleep(40);
            }
        }
    }

    private String toIseText(String referenceText) {
        String wrapped = "[content]\n" + referenceText.trim();
        String withBom = "\uFEFF" + wrapped;
        byte[] bytes = withBom.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_REFERENCE_TEXT_BYTES) {
            throw new XfyunIseException("XFYUN ISE reference text is too long.");
        }
        return withBom;
    }

    private String hmacSha256(String source, String apiSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new XfyunIseException("Failed to generate XFYUN ISE signature.", e);
        }
    }

    private String toSignedHost(URI uri) {
        if (uri.getPort() == -1) {
            return uri.getHost();
        }
        return uri.getHost() + ":" + uri.getPort();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void validate(XfyunIseProperties properties, String referenceText) {
        validateCredentials(properties);
        if (referenceText == null || referenceText.isBlank()) {
            throw new XfyunIseException("XFYUN ISE reference text is empty.");
        }
    }

    private void validateCredentials(XfyunIseProperties properties) {
        if (properties.appId() == null || properties.appId().isBlank()) {
            throw new XfyunIseException("XFYUN ISE APPID is not configured.");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new XfyunIseException("XFYUN ISE API key is not configured.");
        }
        if (properties.apiSecret() == null || properties.apiSecret().isBlank()) {
            throw new XfyunIseException("XFYUN ISE API secret is not configured.");
        }
    }

    private static class IseWebSocketListener implements WebSocket.Listener {

        private final ObjectMapper objectMapper;
        private final CompletableFuture<String> completed = new CompletableFuture<>();
        private final StringBuilder messageBuffer = new StringBuilder();
        private String lastResultMessage;

        private IseWebSocketListener(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                handleMessage(messageBuffer.toString());
                messageBuffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!completed.isDone()) {
                completed.completeExceptionally(
                        new XfyunIseException("XFYUN ISE connection closed before evaluation completed.")
                );
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            completed.completeExceptionally(new XfyunIseException("XFYUN ISE websocket failed.", error));
        }

        private void handleMessage(String message) {
            try {
                JsonNode root = objectMapper.readTree(message);
                int code = root.path("code").asInt(0);
                if (code != 0) {
                    String errorMessage = root.path("message").asText(root.toString());
                    completed.completeExceptionally(new XfyunIseException("XFYUN ISE failed: " + errorMessage));
                    return;
                }

                JsonNode data = root.path("data");
                if (data.hasNonNull("data") || data.hasNonNull("result") || data.hasNonNull("xml_result")) {
                    lastResultMessage = message;
                }
                if (data.path("status").asInt(-1) == 2) {
                    if (lastResultMessage == null) {
                        completed.completeExceptionally(
                                new XfyunIseException("XFYUN ISE completed without evaluation result.")
                        );
                    } else {
                        completed.complete(lastResultMessage);
                    }
                }
            } catch (Exception e) {
                completed.completeExceptionally(new XfyunIseException("Failed to parse XFYUN ISE response.", e));
            }
        }

        private String await(int timeoutMs) throws InterruptedException, ExecutionException, TimeoutException {
            return completed.get(timeoutMs, TimeUnit.MILLISECONDS);
        }

        private boolean isDone() {
            return completed.isDone();
        }
    }
}
