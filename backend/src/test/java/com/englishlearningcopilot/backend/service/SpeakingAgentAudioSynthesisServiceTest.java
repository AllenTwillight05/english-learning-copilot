package com.englishlearningcopilot.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import com.englishlearningcopilot.backend.entity.SpeakingMessageSender;
import com.englishlearningcopilot.backend.entity.SpeakingSession;
import com.englishlearningcopilot.backend.repository.SpeakingMessageRepository;
import com.englishlearningcopilot.backend.service.speech.TtsService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpeakingAgentAudioSynthesisServiceTest {

    @Mock
    private SpeakingMessageRepository messageRepository;

    @Mock
    private TtsService ttsService;

    @Mock
    private SpeakingAudioStorageService audioStorageService;

    @Test
    void persistsHighQualityAudioAndClearsPendingState() {
        SpeakingSession session = new SpeakingSession();
        ReflectionTestUtils.setField(session, "id", 7L);
        SpeakingMessage message = new SpeakingMessage();
        ReflectionTestUtils.setField(message, "id", 11L);
        message.setSession(session);
        message.setSender(SpeakingMessageSender.AGENT);
        message.setContent("Where is your hometown?");
        message.setSpokenText("Where is your hometown?");
        message.setAudioPending(true);

        when(messageRepository.findById(11L)).thenReturn(Optional.of(message));
        when(ttsService.synthesize("Where is your hometown?")).thenReturn(new byte[] {1, 2, 3});
        when(audioStorageService.save(eq(7L), eq(11L), any(), eq("mp3")))
                .thenReturn("/uploads/speaking/7/11.mp3");

        SpeakingAgentAudioSynthesisService service = new SpeakingAgentAudioSynthesisService(
                messageRepository,
                ttsService,
                audioStorageService
        );
        service.synthesizeAgentMessageAsync(11L);

        assertThat(message.isAudioPending()).isFalse();
        assertThat(message.getAudioUrl()).isEqualTo("/uploads/speaking/7/11.mp3");
        verify(messageRepository).save(message);
    }
}
