package com.englishlearningcopilot.backend.repository;

import com.englishlearningcopilot.backend.entity.GrammarQuestion;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GrammarQuestionRepository extends JpaRepository<GrammarQuestion, Integer> {

    @Query("""
            SELECT question FROM GrammarQuestion question
            WHERE question.grammarCategory = :category
              AND question.id NOT IN (
                SELECT grammarbook.grammarQuestionId FROM UserGrammarbook grammarbook
                WHERE grammarbook.userId = :userId
              )
            ORDER BY function('RAND')
            """)
    List<GrammarQuestion> findRandomUnpracticedQuestionsByCategory(
            @Param("userId") Long userId,
            @Param("category") String category,
            Pageable pageable
    );
}
