package com.campusconnect.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only audit row written for every application status change. */
@Entity
@Table(name = "application_status_history")
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 12)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 12)
    private ApplicationStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(length = 500)
    private String note;

    protected ApplicationStatusHistory() {}

    public ApplicationStatusHistory(Long applicationId, ApplicationStatus from, ApplicationStatus to,
                                    Long changedBy, Instant changedAt, String note) {
        this.applicationId = applicationId;
        this.fromStatus = from;
        this.toStatus = to;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.note = note;
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public ApplicationStatus getFromStatus() { return fromStatus; }
    public ApplicationStatus getToStatus() { return toStatus; }
    public Long getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getNote() { return note; }
}
