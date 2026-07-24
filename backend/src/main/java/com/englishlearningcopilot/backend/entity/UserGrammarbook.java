package com.englishlearningcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_grammarbook",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_grammarbook_question",
                        columnNames = {"user_id", "grammar_question_id"}
                )
        }
)
public class UserGrammarbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "grammar_question_id")
    private Integer grammarQuestionId;

    @Column(nullable = false, name = "favorited", columnDefinition = "boolean default false")
    private boolean favorited = false;

    @Column(nullable = false, name = "incorrect", columnDefinition = "boolean default false")
    private boolean incorrect = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getGrammarQuestionId() {
        return grammarQuestionId;
    }

    public void setGrammarQuestionId(Integer grammarQuestionId) {
        this.grammarQuestionId = grammarQuestionId;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public boolean isIncorrect() {
        return incorrect;
    }

    public void setIncorrect(boolean incorrect) {
        this.incorrect = incorrect;
    }
}
