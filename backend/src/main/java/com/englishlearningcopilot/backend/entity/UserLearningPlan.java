package com.englishlearningcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "user_learning_plan",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_learning_plan",
                columnNames = "user_id"
        )
)
public class UserLearningPlan {

    public static final int DEFAULT_VOCABULARY_GOAL = 20;
    public static final int DEFAULT_GRAMMAR_GOAL = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "daily_vocabulary_goal", nullable = false)
    private Integer dailyVocabularyGoal = DEFAULT_VOCABULARY_GOAL;

    @Column(name = "daily_grammar_goal", nullable = false)
    private Integer dailyGrammarGoal = DEFAULT_GRAMMAR_GOAL;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getDailyVocabularyGoal() {
        return dailyVocabularyGoal;
    }

    public void setDailyVocabularyGoal(Integer dailyVocabularyGoal) {
        this.dailyVocabularyGoal = dailyVocabularyGoal;
    }

    public Integer getDailyGrammarGoal() {
        return dailyGrammarGoal;
    }

    public void setDailyGrammarGoal(Integer dailyGrammarGoal) {
        this.dailyGrammarGoal = dailyGrammarGoal;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
