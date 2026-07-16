package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.SpeakingSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

    List<SpeakingSession> findByUserUsernameOrderByStartedAtDesc(String username);
}
