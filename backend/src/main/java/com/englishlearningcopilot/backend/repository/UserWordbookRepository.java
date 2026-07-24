package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.UserWordbook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserWordbookRepository extends JpaRepository<UserWordbook, Long> {

    Optional<UserWordbook> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    List<UserWordbook> findByUserIdOrderByIdDesc(Long userId);

    @Query("""
            SELECT wordbook.vocabularyId AS vocabularyId,
                   COUNT(DISTINCT wordbook.userId) AS learnerCount
            FROM UserWordbook wordbook
            GROUP BY wordbook.vocabularyId
            ORDER BY COUNT(DISTINCT wordbook.userId) DESC, wordbook.vocabularyId ASC
            """)
    List<VocabularyLeaderboardProjection> findVocabularyLeaderboard(Pageable pageable);
}
