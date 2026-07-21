package com.englishlearningcopilot.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpeakingAudioStorageServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void saveReturnsPublicUploadsPathAndCanLoadItBack() {
        SpeakingAudioStorageService storageService =
                new SpeakingAudioStorageService(tempDir.resolve("uploads/speaking").toString());

        String audioUrl = storageService.save(42L, 7L, new byte[]{1, 2, 3});

        assertThat(audioUrl).isEqualTo("/uploads/speaking/42/7.webm");
        assertThat(storageService.load(audioUrl)).containsExactly(1, 2, 3);
        assertThat(storageService.load("speaking/42/7.webm")).containsExactly(1, 2, 3);
    }
}
