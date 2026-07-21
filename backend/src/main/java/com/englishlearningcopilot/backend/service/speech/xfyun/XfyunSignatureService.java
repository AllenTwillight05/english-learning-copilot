package com.englishlearningcopilot.backend.service.speech.xfyun;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class XfyunSignatureService {

    public String sign(Map<String, String> params, String apiSecret) {
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new XfyunAsrException("XFYUN API secret is not configured.");
        }
        try {
            String source = canonicalize(params);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new XfyunAsrException("Failed to generate XFYUN ASR signature.", e);
        }
    }

    public String canonicalize(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder builder = new StringBuilder();
        sorted.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(urlEncode(key)).append('=').append(urlEncode(value));
        });
        return builder.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
