package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.service.speech.IseService;
import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpeakingPronunciationEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(SpeakingPronunciationEvaluationService.class);

    private final SpeakingMessageRepository messageRepository;
    private final IseService iseService;
    private final ObjectMapper objectMapper;

    public SpeakingPronunciationEvaluationService(
            SpeakingMessageRepository messageRepository,
            IseService iseService,
            ObjectMapper objectMapper
    ) {
        this.messageRepository = messageRepository;
        this.iseService = iseService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void evaluateUserMessageAsync(Long messageId, byte[] audioBytes, String referenceText) {
        evaluateUserMessage(messageId, audioBytes, referenceText);
    }

    @Transactional
    public Optional<PronunciationScore> evaluateUserMessage(Long messageId, byte[] audioBytes, String referenceText) {
        if (messageId == null || audioBytes == null || audioBytes.length == 0
                || referenceText == null || referenceText.isBlank()) {
            return Optional.empty();
        }

        Optional<SpeakingMessage> maybeMessage = messageRepository.findById(messageId);
        if (maybeMessage.isEmpty()) {
            log.warn("Skipping pronunciation evaluation because speaking message {} was not found.", messageId);
            return Optional.empty();
        }

        SpeakingMessage message = maybeMessage.get();
        if (message.getPronunciationScore() != null) {
            return Optional.of(readExistingScore(message));
        }

        try {
            PronunciationScore score = iseService.evaluate(audioBytes, referenceText);
            message.setPronunciationScore(score.totalScore());
            message.setPronunciationDetail(toJson(score));
            messageRepository.save(message);
            return Optional.of(score);
        } catch (RuntimeException e) {
            log.warn("Pronunciation evaluation failed for speaking message {}.", messageId, e);
            return Optional.empty();
        }
    }

    private PronunciationScore readExistingScore(SpeakingMessage message) {
        if (message.getPronunciationDetail() != null && !message.getPronunciationDetail().isBlank()) {
            try {
                return objectMapper.readValue(message.getPronunciationDetail(), PronunciationScore.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse stored pronunciation detail for speaking message {}.", message.getId(), e);
            }
        }
        double total = message.getPronunciationScore() != null ? message.getPronunciationScore() : 0;
        return new PronunciationScore(total, total, total, total, 0);
    }

    private String toJson(PronunciationScore score) {
        try {
            return objectMapper.writeValueAsString(score);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
