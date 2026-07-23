package com.englishlearningcopilot.backend.service.speech.xfyun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class XfyunIseAudioConverter {

    public byte[] toPcm16kMono(byte[] audio, XfyunIseProperties properties) {
        if (!properties.transcodeEnabled()) {
            return audio;
        }
        if (audio == null || audio.length == 0) {
            throw new XfyunIseException("Uploaded audio is empty.");
        }

        Path input = null;
        Path output = null;
        try {
            input = Files.createTempFile("speaking-ise-input-", ".webm");
            output = Files.createTempFile("speaking-ise-output-", ".pcm");
            Files.write(input, audio);

            List<String> command = List.of(
                    properties.transcodeCommand(),
                    "-y",
                    "-i", input.toString(),
                    "-ac", "1",
                    "-ar", String.valueOf(properties.sampleRate()),
                    "-f", "s16le",
                    "-acodec", "pcm_s16le",
                    output.toString()
            );
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(properties.timeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new XfyunIseException("FFmpeg audio transcoding timed out.");
            }
            if (process.exitValue() != 0) {
                String log = new String(process.getInputStream().readAllBytes());
                throw new XfyunIseException("FFmpeg failed to transcode audio for XFYUN ISE: " + summarize(log));
            }
            byte[] pcm = Files.readAllBytes(output);
            if (pcm.length == 0) {
                throw new XfyunIseException("FFmpeg produced empty PCM audio for XFYUN ISE.");
            }
            return pcm;
        } catch (IOException e) {
            throw new XfyunIseException(
                    "Failed to run FFmpeg for XFYUN ISE. Install ffmpeg or set XFYUN_ISE_TRANSCODE_ENABLED=false.",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XfyunIseException("FFmpeg audio transcoding was interrupted.", e);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private String summarize(String log) {
        if (log == null || log.isBlank()) {
            return "no ffmpeg output";
        }
        return log.length() <= 500 ? log : log.substring(0, 500);
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temporary files are best-effort cleanup.
        }
    }
}
