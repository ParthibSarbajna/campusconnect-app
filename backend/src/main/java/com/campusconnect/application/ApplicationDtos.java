package com.campusconnect.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class ApplicationDtos {
    private ApplicationDtos() {}

    public record StatusRequest(@NotNull ApplicationStatus status, @Size(max = 500) String note) {}

    /** What a student sees about their own application. */
    public record ApplicationResponse(Long id, Long driveId, String companyName, String driveTitle,
                                      ApplicationStatus status, Instant appliedAt, Instant updatedAt,
                                      Instant applicationDeadline) {}

    /** What an administrator or the student's department SPOC sees. */
    public record ApplicationRow(Long id, Long driveId, String companyName, String driveTitle, Long studentUserId,
                                 String studentName, String studentEmail, String branch, Integer graduationYear,
                                 BigDecimal cgpa, ApplicationStatus status, Instant appliedAt) {}
}
