package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class XfyunIseResultParser {

    private final ObjectMapper objectMapper;

    public XfyunIseResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PronunciationScore parse(String responseJson) {
        String xml = extractXml(responseJson);
        return parseXml(xml);
    }

    String extractXml(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode data = root.path("data");
            String encoded = firstText(data, List.of("data", "result", "xml_result", "result_string"));
            if (encoded.isBlank()) {
                encoded = firstText(root, List.of("data", "result", "xml_result", "result_string"));
            }
            if (encoded.isBlank()) {
                throw new XfyunIseException("XFYUN ISE response did not include evaluation XML.");
            }
            String decoded = decodeIfBase64(encoded);
            return decoded.trim();
        } catch (XfyunIseException e) {
            throw e;
        } catch (Exception e) {
            throw new XfyunIseException("Failed to parse XFYUN ISE response JSON.", e);
        }
    }

    PronunciationScore parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();

            double total = findScore(root, List.of("total_score", "rec_paper", "overall_score", "score"));
            double accuracy = findScore(root, List.of("accuracy_score", "phone_score", "pron_score"));
            double fluency = findScore(root, List.of("fluency_score"));
            double integrity = findScore(root, List.of("integrity_score", "standard_score"));
            double speed = findRaw(root, List.of("except_info", "time_len", "beg_pos", "end_pos"));

            if (Double.isNaN(total)) {
                throw new XfyunIseException("XFYUN ISE XML did not include a total pronunciation score.");
            }

            total = normalizeScore(total);
            accuracy = normalizeOrFallback(accuracy, total);
            fluency = normalizeOrFallback(fluency, total);
            integrity = normalizeOrFallback(integrity, total);
            speed = Double.isNaN(speed) ? 0 : speed;

            return new PronunciationScore(round1(total), round1(accuracy), round1(fluency), round1(integrity), round1(speed));
        } catch (XfyunIseException e) {
            throw e;
        } catch (Exception e) {
            throw new XfyunIseException("Failed to parse XFYUN ISE evaluation XML.", e);
        }
    }

    private String firstText(JsonNode node, List<String> fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return "";
    }

    private String decodeIfBase64(String value) {
        if (value.trim().startsWith("<")) {
            return value;
        }
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private double findScore(Element element, List<String> names) {
        double raw = findRaw(element, names);
        return Double.isNaN(raw) ? Double.NaN : raw;
    }

    private double findRaw(Element element, List<String> names) {
        for (String name : names) {
            if (element.hasAttribute(name)) {
                return parseDouble(element.getAttribute(name));
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                double value = findRaw(childElement, names);
                if (!Double.isNaN(value)) {
                    return value;
                }
            }
        }
        return Double.NaN;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private double normalizeScore(double score) {
        if (score <= 5) {
            return score * 20;
        }
        if (score <= 10) {
            return score * 10;
        }
        return Math.min(score, 100);
    }

    private double normalizeOrFallback(double score, double fallback) {
        return Double.isNaN(score) ? fallback : normalizeScore(score);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
