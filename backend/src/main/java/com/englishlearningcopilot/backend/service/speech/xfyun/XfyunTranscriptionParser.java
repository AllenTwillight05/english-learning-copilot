package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class XfyunTranscriptionParser {

    private final ObjectMapper objectMapper;

    public XfyunTranscriptionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String parse(String orderResult) {
        if (orderResult == null || orderResult.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(orderResult);
            List<Segment> segments = new ArrayList<>();
            JsonNode lattice = root.path("lattice");
            if (lattice.isArray()) {
                for (JsonNode item : lattice) {
                    int begin = item.path("begin").asInt(segments.size());
                    String jsonBest = item.path("json_1best").asText("");
                    String text = parseBestText(jsonBest);
                    if (!text.isBlank()) {
                        segments.add(new Segment(begin, text));
                    }
                }
            }
            segments.sort(Comparator.comparingInt(Segment::begin));
            StringBuilder builder = new StringBuilder();
            for (Segment segment : segments) {
                builder.append(segment.text());
            }
            return normalizeSpaces(builder.toString());
        } catch (Exception e) {
            throw new XfyunAsrException("Failed to parse XFYUN ASR result.", e);
        }
    }

    private String parseBestText(String jsonBest) throws Exception {
        if (jsonBest == null || jsonBest.isBlank()) {
            return "";
        }
        JsonNode best = objectMapper.readTree(jsonBest);
        StringBuilder builder = new StringBuilder();
        JsonNode rtArray = best.path("st").path("rt");
        if (!rtArray.isArray()) {
            return "";
        }
        for (JsonNode rt : rtArray) {
            JsonNode wsArray = rt.path("ws");
            if (!wsArray.isArray()) {
                continue;
            }
            for (JsonNode ws : wsArray) {
                JsonNode cwArray = ws.path("cw");
                if (cwArray.isArray() && !cwArray.isEmpty()) {
                    builder.append(cwArray.get(0).path("w").asText(""));
                }
            }
        }
        return builder.toString();
    }

    private String normalizeSpaces(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private record Segment(int begin, String text) {
    }
}
