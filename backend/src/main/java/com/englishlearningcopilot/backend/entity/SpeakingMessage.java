package com.englishlearningcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "speaking_messages")
@Getter
@Setter
@NoArgsConstructor
public class SpeakingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SpeakingSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpeakingMessageSender sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "spoken_text", columnDefinition = "TEXT")
    private String spokenText;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "audio_pending", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean audioPending;

    @Column(name = "transcribed_text", columnDefinition = "TEXT")
    private String transcribedText;

    @Column(name = "pronunciation_score")
    private Double pronunciationScore;

    @Column(name = "pronunciation_detail", columnDefinition = "TEXT")
    private String pronunciationDetail;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "instant_tip", columnDefinition = "TEXT")
    private String instantTip;

    @Column(nullable = false, name = "turn_index")
    private int turnIndex;

    @Column(nullable = false, name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
