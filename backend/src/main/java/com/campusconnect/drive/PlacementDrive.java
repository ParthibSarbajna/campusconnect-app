package com.campusconnect.drive;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "placement_drives")
public class PlacementDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 160)
    private String location;

    @Column(name = "ctc_lpa", precision = 6, scale = 2)
    private BigDecimal ctcLpa;

    @Column(name = "graduation_year", nullable = false)
    private Short graduationYear;

    @Column(name = "min_cgpa", precision = 4, scale = 2)
    private BigDecimal minCgpa;

    @Column(name = "max_active_backlogs")
    private Short maxActiveBacklogs;

    /** Empty = every branch is eligible. SUBSELECT loads all branches of a result page in one extra query. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "drive_branches", joinColumns = @JoinColumn(name = "drive_id"))
    @Column(name = "branch", nullable = false, length = 120)
    @Fetch(FetchMode.SUBSELECT)
    private Set<String> branches = new LinkedHashSet<>();

    @Column(name = "application_deadline", nullable = false)
    private Instant applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DriveStatus status = DriveStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PlacementDrive() {}

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

    public void applyDetails(Long companyId, String title, String description, String location, BigDecimal ctcLpa,
                             Short graduationYear, BigDecimal minCgpa, Short maxActiveBacklogs,
                             Set<String> branches, Instant applicationDeadline) {
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.ctcLpa = ctcLpa;
        this.graduationYear = graduationYear;
        this.minCgpa = minCgpa;
        this.maxActiveBacklogs = maxActiveBacklogs;
        this.branches.clear();
        this.branches.addAll(branches);
        this.applicationDeadline = applicationDeadline;
    }

    public void setStatus(DriveStatus status) { this.status = status; }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public BigDecimal getCtcLpa() { return ctcLpa; }
    public Short getGraduationYear() { return graduationYear; }
    public BigDecimal getMinCgpa() { return minCgpa; }
    public Short getMaxActiveBacklogs() { return maxActiveBacklogs; }
    public Set<String> getBranches() { return branches; }
    public Instant getApplicationDeadline() { return applicationDeadline; }
    public DriveStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
