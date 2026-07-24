package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.UserGrammarbook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGrammarbookRepository extends JpaRepository<UserGrammarbook, Long> {

    Optional<UserGrammarbook> findByUserIdAndGrammarQuestionId(Long userId, Integer grammarQuestionId);

    @Query("""
            SELECT grammarbook FROM UserGrammarbook grammarbook
            WHERE grammarbook.userId = :userId
              AND (grammarbook.incorrect = true OR grammarbook.favorited = true)
            ORDER BY grammarbook.id DESC
            """)
    List<UserGrammarbook> findNotebookRowsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT question.grammarCategory AS grammarCategory,
                   COUNT(DISTINCT grammarbook.userId) AS learnerCount
            FROM UserGrammarbook grammarbook
            JOIN GrammarQuestion question ON question.id = grammarbook.grammarQuestionId
            GROUP BY question.grammarCategory
            ORDER BY COUNT(DISTINCT grammarbook.userId) DESC, question.grammarCategory ASC
            """)
    List<GrammarLeaderboardProjection> findGrammarLeaderboard(Pageable pageable);
}
