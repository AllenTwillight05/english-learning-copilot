package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakingMessageRepository extends JpaRepository<SpeakingMessage, Long> {

    List<SpeakingMessage> findBySessionIdOrderByTurnIndexAscCreatedAtAsc(Long sessionId);
}
