package com.englishlearningcopilot.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "debug=false")
@Transactional
class UserGrammarbookSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void hibernateCreatesUserGrammarbookWithExpectedColumnsAndConstraints() {
        List<String> columns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = 'user_grammarbook'
                """,
                String.class
        );

        assertThat(columns).containsExactlyInAnyOrder(
                "id",
                "user_id",
                "grammar_question_id",
                "favorited",
                "incorrect"
        );

        jdbcTemplate.update(
                "INSERT INTO user_grammarbook (user_id, grammar_question_id) VALUES (?, ?)",
                1L,
                42
        );

        Boolean favorited = jdbcTemplate.queryForObject(
                "SELECT favorited FROM user_grammarbook WHERE user_id = ? AND grammar_question_id = ?",
                Boolean.class,
                1L,
                42
        );
        assertThat(favorited).isFalse();

        Boolean incorrect = jdbcTemplate.queryForObject(
                "SELECT incorrect FROM user_grammarbook WHERE user_id = ? AND grammar_question_id = ?",
                Boolean.class,
                1L,
                42
        );
        assertThat(incorrect).isFalse();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO user_grammarbook (user_id, grammar_question_id) VALUES (?, ?)",
                1L,
                42
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
