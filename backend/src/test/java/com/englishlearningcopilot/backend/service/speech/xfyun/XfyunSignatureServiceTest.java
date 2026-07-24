package com.englishlearningcopilot.backend.service.speech.xfyun;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class XfyunSignatureServiceTest {

    private final XfyunSignatureService signatureService = new XfyunSignatureService();

    @Test
    void canonicalizeSortsAndUrlEncodesParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("fileName", "recording test.webm");
        params.put("appId", "demo-app");
        params.put("resultType", "transfer,predict");

        assertThat(signatureService.canonicalize(params))
                .isEqualTo("appId=demo-app&fileName=recording%20test.webm&resultType=transfer%2Cpredict");
    }

    @Test
    void signUsesHmacSha1Base64OverCanonicalParams() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("b", "two");
        params.put("a", "one");

        String expected = hmacSha1Base64("a=one&b=two", "secret");

        assertThat(signatureService.sign(params, "secret")).isEqualTo(expected);
    }

    private String hmacSha1Base64(String source, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(source.getBytes(StandardCharsets.UTF_8)));
    }
}
