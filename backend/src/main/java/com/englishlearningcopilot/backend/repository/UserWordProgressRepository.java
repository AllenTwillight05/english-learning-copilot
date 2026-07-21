package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.UserWordProgress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWordProgressRepository extends JpaRepository<UserWordProgress, Long> {

    Optional<UserWordProgress> findByUserIdAndQuestionIdAndQuestionType(
            Long userId,
            String questionId,
            String questionType
    );

    List<UserWordProgress> findByUserIdAndQuestionType(Long userId, String questionType);

    List<UserWordProgress> findByUserIdAndQuestionTypeAndDueBeforeOrderByDueAsc(
            Long userId,
            String questionType,
            Instant due
    );
}
