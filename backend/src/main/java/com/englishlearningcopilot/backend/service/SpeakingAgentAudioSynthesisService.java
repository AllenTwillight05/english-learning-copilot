package com.englishlearningcopilot.backend.service;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.service.speech.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpeakingAgentAudioSynthesisService {

    private static final Logger log = LoggerFactory.getLogger(SpeakingAgentAudioSynthesisService.class);

    private final SpeakingMessageRepository messageRepository;
    private final TtsService ttsService;
    private final SpeakingAudioStorageService audioStorageService;

    public SpeakingAgentAudioSynthesisService(
            SpeakingMessageRepository messageRepository,
            TtsService ttsService,
            SpeakingAudioStorageService audioStorageService
    ) {
        this.messageRepository = messageRepository;
        this.ttsService = ttsService;
        this.audioStorageService = audioStorageService;
    }

    @Async("speakingTtsExecutor")
    @Transactional
    public void synthesizeAgentMessageAsync(Long messageId) {
        SpeakingMessage message = messageRepository.findById(messageId).orElse(null);
        if (message == null || message.getSender() != SpeakingMessageSender.AGENT || !message.isAudioPending()) {
            return;
        }

        long startedAt = System.nanoTime();
        try {
            String spokenText = message.getSpokenText();
            if (spokenText == null || spokenText.isBlank()) {
                spokenText = message.getContent();
            }
            byte[] audioBytes = ttsService.synthesize(spokenText);
            if (audioBytes.length > 0) {
                message.setAudioUrl(audioStorageService.save(
                        message.getSession().getId(),
                        message.getId(),
                        audioBytes,
                        "mp3"
                ));
            } else {
                log.warn("Super Smart TTS returned no audio for speaking message {}.", messageId);
            }
        } catch (RuntimeException e) {
            log.warn("Super Smart TTS synthesis failed for speaking message {}.", messageId, e);
        } finally {
            message.setAudioPending(false);
            messageRepository.save(message);
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("Speaking TTS completed: messageId={}, durationMs={}, audioReady={}",
                    messageId, elapsedMs, message.getAudioUrl() != null);
        }
    }
}
