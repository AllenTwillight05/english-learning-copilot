package com.englishlearningcopilot.backend.repository;

public interface SpeakingLeaderboardProjection {

    String getScenarioId();

    String getTopic();

    String getDescription();

    Long getLearnerCount();
}
