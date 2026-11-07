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
@Table(name = "recruitment_rounds")
public class RecruitmentRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drive_id", nullable = false)
    private Long driveId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(length = 500)
    private String venue;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruitmentRoundStatus status = RecruitmentRoundStatus.SCHEDULED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecruitmentRound() {}

    public RecruitmentRound(Long driveId, String name, Integer sequenceNumber,
                           Instant scheduledAt, String venue, String instructions) {
        this.driveId = driveId;
        this.name = name;
        this.sequenceNumber = sequenceNumber;
        this.scheduledAt = scheduledAt;
        this.venue = venue;
        this.instructions = instructions;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDetails(String name, Integer sequenceNumber,
                              Instant scheduledAt, String venue, String instructions) {
        this.name = name;
        this.sequenceNumber = sequenceNumber;
        this.scheduledAt = scheduledAt;
        this.venue = venue;
        this.instructions = instructions;
    }

    public void setStatus(RecruitmentRoundStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getDriveId() { return driveId; }
    public String getName() { return name; }
    public Integer getSequenceNumber() { return sequenceNumber; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getVenue() { return venue; }
    public String getInstructions() { return instructions; }
    public RecruitmentRoundStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}