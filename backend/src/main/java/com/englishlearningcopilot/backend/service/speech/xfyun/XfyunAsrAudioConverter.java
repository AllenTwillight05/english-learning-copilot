package com.englishlearningcopilot.backend.service.speech.xfyun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class XfyunAsrAudioConverter {

    public byte[] toPcm16kMono(byte[] audio, XfyunFileAsrProperties properties) {
        if (!properties.transcodeEnabled()) {
            return audio;
        }
        if (audio == null || audio.length == 0) {
            throw new XfyunAsrException("Uploaded audio is empty.");
        }

        Path input = null;
        Path output = null;
        try {
            input = Files.createTempFile("speaking-asr-input-", ".webm");
            output = Files.createTempFile("speaking-asr-output-", ".pcm");
            Files.write(input, audio);

            List<String> command = List.of(
                    properties.transcodeCommand(),
                    "-y",
                    "-i", input.toString(),
                    "-ac", "1",
                    "-ar", "16000",
                    "-f", "s16le",
                    "-acodec", "pcm_s16le",
                    output.toString()
            );
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(properties.transcodeTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new XfyunAsrException("FFmpeg audio transcoding timed out.");
            }
            if (process.exitValue() != 0) {
                throw new XfyunAsrException("FFmpeg failed to convert the browser recording: "
                        + summarize(process.getInputStream().readAllBytes()));
            }

            byte[] pcm = Files.readAllBytes(output);
            if (pcm.length == 0) {
                throw new XfyunAsrException("FFmpeg produced empty PCM audio.");
            }
            return pcm;
        } catch (IOException e) {
            throw new XfyunAsrException(
                    "FFmpeg is required to convert browser recordings for XFYUN ASR. "
                            + "Install FFmpeg or set XFYUN_ASR_TRANSCODE_ENABLED=false for already supported audio.",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XfyunAsrException("FFmpeg audio transcoding was interrupted.", e);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private String summarize(byte[] output) {
        String text = new String(output);
        return text.length() <= 500 ? text : text.substring(0, 500);
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
