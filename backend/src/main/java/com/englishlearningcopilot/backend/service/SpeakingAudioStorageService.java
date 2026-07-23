package com.englishlearningcopilot.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stores and retrieves speaking audio recordings on the local filesystem.
 */
@Service
public class SpeakingAudioStorageService {

    private final Path uploadRoot;

    public SpeakingAudioStorageService(
            @Value("${app.speaking.upload-dir:uploads/speaking}") String uploadDir
    ) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create speaking upload directory: " + uploadRoot, e);
        }
    }

    /**
     * Save audio bytes for a message and return a URL path.
     *
     * @param sessionId  the owning session ID
     * @param messageId  the message ID (used as filename stem)
     * @param audioBytes raw audio data
     * @return URL path suitable for storage in DB, e.g. "/uploads/speaking/42/5.webm"
     */
    public String save(Long sessionId, Long messageId, byte[] audioBytes) {
        return save(sessionId, messageId, audioBytes, "webm");
    }

    /**
     * Save audio bytes for a message with a specific file extension.
     *
     * @param sessionId  the owning session ID
     * @param messageId  the message ID (used as filename stem)
     * @param audioBytes raw audio data
     * @param extension  file extension without dot, e.g. "webm" or "mp3"
     * @return URL path suitable for storage in DB
     */
    public String save(Long sessionId, Long messageId, byte[] audioBytes, String extension) {
        Path sessionDir = uploadRoot.resolve(String.valueOf(sessionId));
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create session directory: " + sessionDir, e);
        }

        String filename = messageId + "." + normalizeExtension(extension);
        Path filePath = sessionDir.resolve(filename);
        try {
            Files.write(filePath, audioBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write audio file: " + filePath, e);
        }

        return "/uploads/" + uploadRoot.getFileName() + "/" + sessionId + "/" + filename;
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "webm";
        }
        String normalized = extension.startsWith(".") ? extension.substring(1) : extension;
        if (!normalized.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Audio file extension is invalid.");
        }
        return normalized.toLowerCase();
    }

    /**
     * Read audio bytes from a stored file.
     *
     * @param relativePath the path previously returned by {@link #save}
     * @return raw audio bytes
     */
    public byte[] load(String relativePath) {
        String normalizedPath = relativePath;
        if (normalizedPath.startsWith("/uploads/")) {
            normalizedPath = normalizedPath.substring("/uploads/".length());
        }
        Path filePath = uploadRoot.resolveSibling(normalizedPath).normalize();
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read audio file: " + filePath, e);
        }
    }
}
