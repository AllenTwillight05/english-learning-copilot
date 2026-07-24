package com.englishlearningcopilot.backend.service.speech.xfyun;

public class XfyunTtsException extends RuntimeException {

    public XfyunTtsException(String message) {
        super(message);
    }

    public XfyunTtsException(String message, Throwable cause) {
        super(message, cause);
    }
}
