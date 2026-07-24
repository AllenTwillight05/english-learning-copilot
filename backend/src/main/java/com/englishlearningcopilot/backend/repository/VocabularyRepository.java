package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.Vocabulary;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    @Query("SELECT vocabulary FROM Vocabulary vocabulary ORDER BY function('RAND')")
    List<Vocabulary> findRandomPracticeWords(Pageable pageable);

    @Query("""
            SELECT vocabulary FROM Vocabulary vocabulary
            WHERE vocabulary.tag IS NOT NULL
              AND (
                concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag1), ' %')
                OR concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag2), ' %')
                OR concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag3), ' %')
                OR concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag4), ' %')
              )
            ORDER BY function('RAND')
            """)
    List<Vocabulary> findRandomPracticeWordsByTags(
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tag3") String tag3,
            @Param("tag4") String tag4,
            Pageable pageable
    );

    @Query("""
            SELECT vocabulary FROM Vocabulary vocabulary
            WHERE vocabulary.id NOT IN (
                SELECT wordbook.vocabularyId FROM UserWordbook wordbook
                WHERE wordbook.userId = :userId
            )
            ORDER BY function('RAND')
            """)
    List<Vocabulary> findRandomUnlearnedPracticeWords(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT vocabulary FROM Vocabulary vocabulary
            WHERE vocabulary.tag IS NOT NULL
              AND (
                concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag1), ' %')
                OR concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag2), ' %')
                OR concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag3), ' %')
                OR concat(concat(' ', lower(vocabulary.tag)), ' ') LIKE concat(concat('% ', :tag4), ' %')
              )
              AND vocabulary.id NOT IN (
                SELECT wordbook.vocabularyId FROM UserWordbook wordbook
                WHERE wordbook.userId = :userId
              )
            ORDER BY function('RAND')
            """)
    List<Vocabulary> findRandomUnlearnedPracticeWordsByTags(
            @Param("userId") Long userId,
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tag3") String tag3,
            @Param("tag4") String tag4,
            Pageable pageable
    );

    @Query("""
            SELECT vocabulary FROM Vocabulary vocabulary
            WHERE vocabulary.id IN (
                SELECT wordbook.vocabularyId FROM UserWordbook wordbook
                WHERE wordbook.userId = :userId
            )
            ORDER BY vocabulary.id
            """)
    List<Vocabulary> findWordbookWordsByUserId(@Param("userId") Long userId);
}
