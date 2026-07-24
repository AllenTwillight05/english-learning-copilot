package com.englishlearningcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_daily_learning_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_daily_learning_progress",
                columnNames = {"user_id", "plan_date"}
        ),
        indexes = @Index(
                name = "idx_user_daily_learning_completed",
                columnList = "user_id, completed, plan_date"
        )
)
public class UserDailyLearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "vocabulary_completed", nullable = false)
    private Integer vocabularyCompleted = 0;

    @Column(name = "grammar_completed", nullable = false)
    private Integer grammarCompleted = 0;

    @Column(name = "vocabulary_goal", nullable = false)
    private Integer vocabularyGoal = UserLearningPlan.DEFAULT_VOCABULARY_GOAL;

    @Column(name = "grammar_goal", nullable = false)
    private Integer grammarGoal = UserLearningPlan.DEFAULT_GRAMMAR_GOAL;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

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

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public Integer getVocabularyCompleted() {
        return vocabularyCompleted;
    }

    public void setVocabularyCompleted(Integer vocabularyCompleted) {
        this.vocabularyCompleted = vocabularyCompleted;
    }

    public Integer getGrammarCompleted() {
        return grammarCompleted;
    }

    public void setGrammarCompleted(Integer grammarCompleted) {
        this.grammarCompleted = grammarCompleted;
    }

    public Integer getVocabularyGoal() {
        return vocabularyGoal;
    }

    public void setVocabularyGoal(Integer vocabularyGoal) {
        this.vocabularyGoal = vocabularyGoal;
    }

    public Integer getGrammarGoal() {
        return grammarGoal;
    }

    public void setGrammarGoal(Integer grammarGoal) {
        this.grammarGoal = grammarGoal;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
