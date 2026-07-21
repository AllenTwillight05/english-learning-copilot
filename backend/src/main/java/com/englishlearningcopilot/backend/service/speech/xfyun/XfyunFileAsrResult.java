package com.englishlearningcopilot.backend.service.speech.xfyun;

public record XfyunFileAsrResult(
        int orderStatus,
        String orderResult,
        String failType
) {

    public boolean isProcessing() {
        return orderStatus == 1 || orderStatus == 2 || orderStatus == 3;
    }

    public boolean isCompleted() {
        return orderStatus == 4;
    }

    public boolean isFailed() {
        return orderStatus == -1;
    }
}
