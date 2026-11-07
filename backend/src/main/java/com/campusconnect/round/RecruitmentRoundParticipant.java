
package com.campusconnect.round;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "recruitment_round_participants")
public class RecruitmentRoundParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RoundParticipantOutcome outcome = RoundParticipantOutcome.PENDING;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecruitmentRoundParticipant() {}

    public RecruitmentRoundParticipant(Long roundId, Long applicationId) {
        this.roundId = roundId;
        this.applicationId = applicationId;
        this.outcome = RoundParticipantOutcome.PENDING;
    }

    @PrePersist
    void onCreate() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateOutcome(RoundParticipantOutcome outcome, String feedback) {
        this.outcome = outcome;
        this.feedback = feedback;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getRoundId() { return roundId; }
    public Long getApplicationId() { return applicationId; }
    public RoundParticipantOutcome getOutcome() { return outcome; }
    public String getFeedback() { return feedback; }
    public Instant getUpdatedAt() { return updatedAt; }
}
