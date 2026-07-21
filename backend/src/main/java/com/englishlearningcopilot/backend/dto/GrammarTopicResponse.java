package com.englishlearningcopilot.backend.dto;

import java.util.List;

public record GrammarTopicResponse(
        String id,
        String title,
        String summary,
        List<String> examples,
        int progress,
        String tag
) {
}
