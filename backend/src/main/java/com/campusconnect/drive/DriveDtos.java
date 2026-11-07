package com.campusconnect.drive;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DriveDtos {
    private DriveDtos() {}

    public record DriveRequest(
            @NotNull Long companyId,
            @NotBlank @Size(max = 160) String title,
            @Size(max = 4000) String description,
            @Size(max = 160) String location,
            @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal ctcLpa,
            @NotNull @Min(2000) @Max(2100) Integer graduationYear,
            @DecimalMin("0.00") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2) BigDecimal minCgpa,
            @Min(0) @Max(50) Integer maxActiveBacklogs,
            @Size(max = 30) List<@NotBlank @Size(max = 120) String> branches,
            @NotNull Instant applicationDeadline) {}

    public record StatusRequest(@NotNull DriveStatus status) {}

    public record DriveResponse(
            Long id, Long companyId, String companyName, String title, String description, String location,
            BigDecimal ctcLpa, Integer graduationYear, BigDecimal minCgpa, Integer maxActiveBacklogs,
            List<String> branches, Instant applicationDeadline, DriveStatus status, Instant createdAt, Instant updatedAt) {

        public static DriveResponse from(PlacementDrive d, String companyName) {
            return new DriveResponse(d.getId(), d.getCompanyId(), companyName, d.getTitle(), d.getDescription(),
                    d.getLocation(), d.getCtcLpa(), d.getGraduationYear() == null ? null : d.getGraduationYear().intValue(),
                    d.getMinCgpa(), d.getMaxActiveBacklogs() == null ? null : d.getMaxActiveBacklogs().intValue(),
                    d.getBranches().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                    d.getApplicationDeadline(), d.getStatus(), d.getCreatedAt(), d.getUpdatedAt());
        }
    }

    public record StudentDriveView(DriveResponse drive, EligibilityResult eligibility, Long applicationId, String applicationStatus) {}
}
