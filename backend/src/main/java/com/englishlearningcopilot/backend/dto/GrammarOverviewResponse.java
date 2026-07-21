package com.englishlearningcopilot.backend.dto;

import java.util.List;

public record GrammarOverviewResponse(
        int masteryRate,
        List<Stat> stats
) {

    public record Stat(
            String value,
            String label
    ) {
    }
}
