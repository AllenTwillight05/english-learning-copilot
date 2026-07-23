package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
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
public class XfyunOnlineTtsClient {

    private static final DateTimeFormatter RFC1123 =
            DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);
    private static final int MAX_TEXT_BYTES = 8000;

    private final ObjectMapper objectMapper;

    public XfyunOnlineTtsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] synthesize(String text, XfyunOnlineTtsProperties properties) {
        validate(properties);

        URI uri = buildAuthenticatedUri(properties, ZonedDateTime.now(ZoneOffset.UTC));
        TtsWebSocketListener listener = new TtsWebSocketListener(objectMapper);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .build();

        WebSocket webSocket = null;
        try {
            webSocket = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofMillis(properties.timeoutMs()))
                    .buildAsync(uri, listener)
                    .get(properties.timeoutMs(), TimeUnit.MILLISECONDS);
            webSocket.sendText(toRequestPayload(text, properties), true).join();
            byte[] audio = listener.await(properties.timeoutMs());
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            return audio;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XfyunTtsException("XFYUN TTS request was interrupted.", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof XfyunTtsException ttsException) {
                throw ttsException;
            }
            throw new XfyunTtsException("Failed to call XFYUN TTS service.", e.getCause());
        } catch (TimeoutException e) {
            throw new XfyunTtsException("XFYUN TTS request timed out.", e);
        } finally {
            if (webSocket != null && !listener.isDone()) {
                webSocket.abort();
            }
        }
    }

    String toRequestPayload(String text, XfyunOnlineTtsProperties properties) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        if (textBytes.length > MAX_TEXT_BYTES) {
            throw new XfyunTtsException("XFYUN Super Smart TTS text is too long.");
        }

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode header = root.putObject("header");
        header.put("app_id", properties.appId());
        header.put("status", 2);

        ObjectNode parameter = root.putObject("parameter");
        ObjectNode tts = parameter.putObject("tts");
        tts.put("vcn", properties.voice());
        tts.put("speed", properties.speed());
        tts.put("pitch", properties.pitch());
        tts.put("volume", properties.volume());
        tts.put("bgs", 0);
        tts.put("reg", 0);
        tts.put("rdn", 0);
        tts.put("rhy", 0);

        ObjectNode audio = tts.putObject("audio");
        audio.put("encoding", properties.audioEncoding());
        audio.put("sample_rate", properties.sampleRate());
        audio.put("channels", 1);
        audio.put("bit_depth", 16);
        audio.put("frame_size", 0);

        ObjectNode payload = root.putObject("payload");
        ObjectNode payloadText = payload.putObject("text");
        payloadText.put("encoding", properties.textEncoding());
        payloadText.put("compress", "raw");
        payloadText.put("format", "plain");
        payloadText.put("status", 2);
        payloadText.put("seq", 0);
        payloadText.put("text", Base64.getEncoder().encodeToString(textBytes));

        try {
            return objectMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new XfyunTtsException("Failed to build XFYUN TTS request payload.", e);
        }
    }

    URI buildAuthenticatedUri(XfyunOnlineTtsProperties properties, ZonedDateTime dateTime) {
        validate(properties);
        URI baseUri = URI.create(properties.url());
        if (!"wss".equalsIgnoreCase(baseUri.getScheme())) {
            throw new XfyunTtsException("XFYUN Super Smart TTS URL must use wss.");
        }

        String host = toSignedHost(baseUri);
        String path = baseUri.getRawPath() == null || baseUri.getRawPath().isBlank()
                ? "/v1/private/mcd9m97e6"
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

    private String hmacSha256(String source, String apiSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new XfyunTtsException("Failed to generate XFYUN TTS signature.", e);
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

    private void validate(XfyunOnlineTtsProperties properties) {
        if (properties.appId() == null || properties.appId().isBlank()) {
            throw new XfyunTtsException("XFYUN Super Smart TTS APPID is not configured.");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new XfyunTtsException("XFYUN Super Smart TTS API key is not configured.");
        }
        if (properties.apiSecret() == null || properties.apiSecret().isBlank()) {
            throw new XfyunTtsException("XFYUN Super Smart TTS API secret is not configured.");
        }
    }

    private static class TtsWebSocketListener implements WebSocket.Listener {

        private final ObjectMapper objectMapper;
        private final CompletableFuture<byte[]> completed = new CompletableFuture<>();
        private final ByteArrayOutputStream audio = new ByteArrayOutputStream();
        private final StringBuilder messageBuffer = new StringBuilder();

        private TtsWebSocketListener(ObjectMapper objectMapper) {
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
                        new XfyunTtsException("XFYUN Super Smart TTS connection closed before synthesis completed.")
                );
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            completed.completeExceptionally(new XfyunTtsException("XFYUN Super Smart TTS websocket failed.", error));
        }

        private void handleMessage(String message) {
            try {
                JsonNode root = objectMapper.readTree(message);
                JsonNode header = root.path("header");
                int code = header.has("code")
                        ? header.path("code").asInt(0)
                        : root.path("code").asInt(0);
                if (code != 0) {
                    String errorMessage = header.path("message").asText(root.path("message").asText(root.toString()));
                    completed.completeExceptionally(
                            new XfyunTtsException("XFYUN Super Smart TTS failed: " + errorMessage)
                    );
                    return;
                }

                JsonNode audioNode = root.path("payload").path("audio");
                String audioChunk = audioNode.path("audio").asText("");
                if (!audioChunk.isBlank()) {
                    audio.write(Base64.getDecoder().decode(audioChunk));
                }
                int headerStatus = header.path("status").asInt(-1);
                int audioStatus = audioNode.path("status").asInt(-1);
                if (headerStatus == 2 || audioStatus == 2) {
                    completed.complete(audio.toByteArray());
                }
            } catch (Exception e) {
                completed.completeExceptionally(
                        new XfyunTtsException("Failed to parse XFYUN Super Smart TTS response.", e)
                );
            }
        }

        private byte[] await(int timeoutMs) throws InterruptedException, ExecutionException, TimeoutException {
            return completed.get(timeoutMs, TimeUnit.MILLISECONDS);
        }

        private boolean isDone() {
            return completed.isDone();
        }
    }
}
