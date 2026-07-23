package com.englishlearningcopilot.backend.service.speech.xfyun;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class XfyunIseResultParserTest {

    private final XfyunIseResultParser parser = new XfyunIseResultParser(new ObjectMapper());

    @Test
    void parsesBase64EncodedEvaluationXml() {
        String xml = """
                <xml_result>
                  <read_sentence rec_paper="4.5" accuracy_score="4.0" fluency_score="3.5" integrity_score="5.0"/>
                </xml_result>
                """;
        String encoded = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
        String response = """
                {"code":0,"data":{"status":2,"data":"%s"}}
                """.formatted(encoded);

        var score = parser.parse(response);

        assertThat(score.totalScore()).isEqualTo(90.0);
        assertThat(score.accuracy()).isEqualTo(80.0);
        assertThat(score.fluency()).isEqualTo(70.0);
        assertThat(score.integrity()).isEqualTo(100.0);
    }

    @Test
    void parsesPlainXmlResultFields() {
        String response = """
                {"code":0,"data":{"status":2,"result":"<read_sentence total_score=\\"86\\" phone_score=\\"82\\" fluency_score=\\"78\\"/>"}}
                """;

        var score = parser.parse(response);

        assertThat(score.totalScore()).isEqualTo(86.0);
        assertThat(score.accuracy()).isEqualTo(82.0);
        assertThat(score.fluency()).isEqualTo(78.0);
        assertThat(score.integrity()).isEqualTo(86.0);
    }
}
