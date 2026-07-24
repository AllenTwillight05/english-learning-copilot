package com.englishlearningcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "speaking_scenarios")
@Getter
@Setter
@NoArgsConstructor
public class SpeakingScenario {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 40)
    private String difficulty;

    @Column(nullable = false, length = 60)
    private String accent;

    @Column(nullable = false, length = 40)
    private String duration;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false, length = 40)
    private String tone;

    @Column(nullable = false, length = 1000)
    private String goal;

    @Column(nullable = false, length = 1000)
    private String keywords;

    @Column(nullable = false, name = "role_prompt", length = 2000)
    private String rolePrompt;

    @Column(nullable = false, name = "opening_message", length = 1000)
    private String openingMessage;

    @Column(name = "sample_dialogue", length = 4000)
    private String sampleDialogue;

    @Column(nullable = false, name = "target_turns")
    private int targetTurns;

    @Column(nullable = false, name = "scoring_rubric", length = 2000)
    private String scoringRubric;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false, name = "created_at")
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(nullable = false, name = "updated_at")
    @Setter(AccessLevel.NONE)
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
}
