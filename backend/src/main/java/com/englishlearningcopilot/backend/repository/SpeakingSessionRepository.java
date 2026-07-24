package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.SpeakingSession;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

    List<SpeakingSession> findByUserUsernameOrderByStartedAtDesc(String username);

    @Query("""
            SELECT speakingSession.scenario.id AS scenarioId,
                   speakingSession.scenario.title AS topic,
                   speakingSession.scenario.description AS description,
                   COUNT(DISTINCT speakingSession.user.id) AS learnerCount
            FROM SpeakingSession speakingSession
            WHERE speakingSession.scenario.active = true
            GROUP BY speakingSession.scenario.id,
                     speakingSession.scenario.title,
                     speakingSession.scenario.description
            ORDER BY COUNT(DISTINCT speakingSession.user.id) DESC,
                     speakingSession.scenario.id ASC
            """)
    List<SpeakingLeaderboardProjection> findSpeakingLeaderboard(Pageable pageable);
}
