package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class XfyunFileAsrClient {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final XfyunSignatureService signatureService;

    public XfyunFileAsrClient(ObjectMapper objectMapper, XfyunSignatureService signatureService) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.signatureService = signatureService;
    }

    public XfyunFileAsrOrder upload(byte[] audio, XfyunFileAsrProperties properties) {
        String signatureRandom = random();
        Map<String, String> params = uploadParams(properties, signatureRandom);
        params.put("fileName", properties.fileName());
        params.put("fileSize", String.valueOf(audio.length));
        params.put("language", properties.language());
        params.put("accent", properties.accent());
        params.put("durationCheckDisable", String.valueOf(properties.durationCheckDisable()));
        params.put("audioMode", "fileStream");

        JsonNode root = post(
                properties.baseUrl() + "/v2/upload",
                params,
                properties.apiSecret(),
                audio,
                "application/octet-stream"
        );
        ensureSuccess(root, "XFYUN ASR upload failed");

        JsonNode content = root.path("content");
        String orderId = content.path("orderId").asText(null);
        if (orderId == null || orderId.isBlank()) {
            throw new XfyunAsrException("XFYUN ASR upload response did not include orderId.");
        }
        return new XfyunFileAsrOrder(orderId, signatureRandom);
    }

    public XfyunFileAsrResult getResult(XfyunFileAsrOrder order, XfyunFileAsrProperties properties) {
        Map<String, String> params = resultParams(properties, order);

        JsonNode root = post(
                properties.baseUrl() + "/v2/getResult",
                params,
                properties.apiSecret(),
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "application/json"
        );
        ensureSuccess(root, "XFYUN ASR getResult failed");

        JsonNode content = root.path("content");
        JsonNode orderInfo = content.path("orderInfo");
        int orderStatus = orderInfo.path("status").asInt(-1);
        String orderResult = content.path("orderResult").asText(null);
        String failType = orderInfo.path("failType").asText(null);
        return new XfyunFileAsrResult(orderStatus, orderResult, failType);
    }

    private JsonNode post(
            String endpoint,
            Map<String, String> params,
            String apiSecret,
            byte[] body,
            String contentType
    ) {
        String signature = signatureService.sign(params, apiSecret);
        URI uri = URI.create(endpoint + "?" + signatureService.canonicalize(params));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", contentType)
                .header("signature", signature)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new XfyunAsrException("XFYUN ASR HTTP request failed: " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new XfyunAsrException("Failed to call XFYUN ASR service.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XfyunAsrException("XFYUN ASR request was interrupted.", e);
        }
    }

    private Map<String, String> uploadParams(XfyunFileAsrProperties properties, String signatureRandom) {
        validate(properties);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appId", properties.appId());
        params.put("accessKeyId", properties.apiKey());
        params.put("dateTime", now());
        params.put("signatureRandom", signatureRandom);
        return params;
    }

    private Map<String, String> resultParams(XfyunFileAsrProperties properties, XfyunFileAsrOrder order) {
        validate(properties);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("accessKeyId", properties.apiKey());
        params.put("dateTime", now());
        params.put("signatureRandom", order.signatureRandom());
        params.put("orderId", order.orderId());
        params.put("resultType", properties.resultType());
        return params;
    }

    private void validate(XfyunFileAsrProperties properties) {
        if (properties.appId() == null || properties.appId().isBlank()) {
            throw new XfyunAsrException("XFYUN APPID is not configured.");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new XfyunAsrException("XFYUN API key is not configured.");
        }
    }

    private void ensureSuccess(JsonNode root, String prefix) {
        String code = root.path("code").asText("");
        if (!"000000".equals(code) && !"0".equals(code)) {
            String message = root.path("descInfo").asText(root.path("message").asText(root.toString()));
            throw new XfyunAsrException(prefix + ": " + message);
        }
    }

    private String now() {
        return ZonedDateTime.now().format(DATE_TIME_FORMATTER);
    }

    private String random() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
