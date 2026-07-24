package com.englishlearningcopilot.backend.service.speech.xfyun;

public class XfyunAsrException extends RuntimeException {

    public XfyunAsrException(String message) {
        super(message);
    }

    public XfyunAsrException(String message, Throwable cause) {
        super(message, cause);
    }
}
