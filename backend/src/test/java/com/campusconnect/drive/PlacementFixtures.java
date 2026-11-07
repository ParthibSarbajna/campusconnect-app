package com.campusconnect.drive;

import com.campusconnect.company.Company;
import com.campusconnect.student.StudentProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.test.util.ReflectionTestUtils;

/** Synthetic fixtures shared by drive and application tests. */
public final class PlacementFixtures {
    public static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    private PlacementFixtures() {}

    public static PlacementDrive drive(long id, long companyId, DriveStatus status) {
        PlacementDrive d = new PlacementDrive();
        d.applyDetails(companyId, "Software Engineer", "Build things", "Kolkata", new BigDecimal("8.50"),
                (short) 2027, new BigDecimal("7.50"), (short) 0, Set.of("CSE", "IT"), NOW.plusSeconds(86_400));
        d.setStatus(status);
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }

    public static Company company(long id, String name, boolean active) {
        Company c = new Company(name, null, null, null);
        c.setActive(active);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    public static StudentProfile profile(long userId, String branch, Integer year, String cgpa, Integer backlogs) {
        StudentProfile p = new StudentProfile(userId);
        p.updateDetails("R" + userId, null, null, "B.Tech", branch,
                year == null ? null : year.shortValue(), cgpa == null ? null : new BigDecimal(cgpa),
                backlogs == null ? null : backlogs.shortValue(), null);
        return p;
    }

    public static DriveDtos.DriveRequest request(long companyId) {
        return new DriveDtos.DriveRequest(companyId, "  Software Engineer ", " Build things ", "Kolkata",
                new BigDecimal("8.50"), 2027, new BigDecimal("7.50"), 0, List.of("CSE", "cse", " IT ", ""),
                NOW.plusSeconds(86_400));
    }
}
