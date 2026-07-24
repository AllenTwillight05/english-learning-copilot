package com.englishlearningcopilot.backend.service.speech.xfyun;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class XfyunTranscriptionParserTest {

    private final XfyunTranscriptionParser parser = new XfyunTranscriptionParser(new ObjectMapper());

    @Test
    void parsesLatticeJsonBestWordsInBeginOrder() throws Exception {
        String firstBest = """
                {
                  "st": {
                    "rt": [
                      {
                        "ws": [
                          { "cw": [{ "w": "Hello" }] },
                          { "cw": [{ "w": " " }] },
                          { "cw": [{ "w": "world" }] }
                        ]
                      }
                    ]
                  }
                }
                """;
        String secondBest = """
                {
                  "st": {
                    "rt": [
                      {
                        "ws": [
                          { "cw": [{ "w": "." }] }
                        ]
                      }
                    ]
                  }
                }
                """;
        String orderResult = """
                {
                  "lattice": [
                    { "begin": 1000, "json_1best": %s },
                    { "begin": 0, "json_1best": %s }
                  ]
                }
                """.formatted(quote(secondBest), quote(firstBest));

        assertThat(parser.parse(orderResult)).isEqualTo("Hello world.");
    }

    private String quote(String value) throws Exception {
        return new ObjectMapper().writeValueAsString(value);
    }
}
