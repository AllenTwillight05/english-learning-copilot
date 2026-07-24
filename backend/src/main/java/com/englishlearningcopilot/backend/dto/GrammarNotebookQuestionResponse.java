package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.entity.GrammarQuestion;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public record GrammarNotebookQuestionResponse(
        Integer id,
        @JsonProperty("question_text") String questionText,
        List<String> options,
        String answer,
        @JsonProperty("grammar_category") String grammarCategory,
        String explanation,
        boolean wrong,
        boolean favorited
) {

    public static GrammarNotebookQuestionResponse from(
            GrammarQuestion question,
            boolean wrong,
            boolean favorited
    ) {
        List<String> options = new ArrayList<>();
        options.add(question.getOptionA());
        options.add(question.getOptionB());
        options.add(question.getOptionC());
        options.add(question.getOptionD());
        if (question.getOptionE() != null && !question.getOptionE().isBlank()) {
            options.add(question.getOptionE());
        }

        return new GrammarNotebookQuestionResponse(
                question.getId(),
                question.getQuestionText(),
                options,
                question.getAnswer(),
                question.getGrammarCategory(),
                question.getExplanation(),
                wrong,
                favorited
        );
    }
}
