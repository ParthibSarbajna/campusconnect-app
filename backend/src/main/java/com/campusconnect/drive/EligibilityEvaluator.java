package com.campusconnect.drive;

import com.campusconnect.student.StudentProfile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, deterministic eligibility rules. Every criterion lives on the drive
 * (nothing is hardcoded here), and every failure produces a plain-language
 * reason so the decision is explainable.
 */
public final class EligibilityEvaluator {

    private EligibilityEvaluator() {}

    public static EligibilityResult evaluate(StudentProfile profile, PlacementDrive drive) {
        if (profile == null) {
            return new EligibilityResult(
                    false,
                    List.of("Save your student profile before applying.")
            );
        }

        List<String> reasons = new ArrayList<>();

        // Graduation year eligibility
        if (profile.getGraduationYear() == null) {
            reasons.add("Add your graduation year to your profile.");
        } else if (!profile.getGraduationYear().equals(drive.getGraduationYear())) {
            reasons.add(
                    "This drive is for the " + drive.getGraduationYear()
                            + " graduating batch (your profile says "
                            + profile.getGraduationYear() + ")."
            );
        }

        // CGPA eligibility
        BigDecimal min = drive.getMinCgpa();

        if (min != null) {
            if (profile.getCgpa() == null) {
                reasons.add(
                        "Add your CGPA to your profile (minimum "
                                + min.toPlainString() + " required)."
                );
            } else if (profile.getCgpa().compareTo(min) < 0) {
                reasons.add(
                        "Your CGPA " + profile.getCgpa().toPlainString()
                                + " is below the required "
                                + min.toPlainString() + "."
                );
            }
        }

        // Active backlog eligibility
        Short maxBacklogs = drive.getMaxActiveBacklogs();

        if (maxBacklogs != null) {
            short have = profile.getActiveBacklogs() == null
                    ? 0
                    : profile.getActiveBacklogs();

            if (have > maxBacklogs) {
                reasons.add(
                        "You have " + have + " active backlog(s); at most "
                                + maxBacklogs + " allowed."
                );
            }
        }

        // Branch eligibility
        if (!drive.getBranches().isEmpty()) {
            String branch = profile.getBranch();

            boolean ok = branch != null
                    && drive.getBranches().stream()
                            .anyMatch(eligibleBranch ->
                                    normalizeBranch(eligibleBranch)
                                            .equalsIgnoreCase(normalizeBranch(branch))
                            );

            if (!ok) {
                reasons.add(
                        branch == null
                                ? "Add your branch to your profile. Eligible branches: "
                                        + String.join(", ", drive.getBranches()) + "."
                                : "Your branch (" + branch
                                        + ") is not eligible. Eligible branches: "
                                        + String.join(", ", drive.getBranches()) + "."
                );
            }
        }

        return new EligibilityResult(
                reasons.isEmpty(),
                List.copyOf(reasons)
        );
    }

    /**
     * Normalize branch names by removing whitespace and ignoring case.
     * Example: "CSE(AI & ML)" and "CSE (AI & ML)" become equivalent.
     */
    private static String normalizeBranch(String branch) {
        return branch == null
                ? ""
                : branch.replaceAll("\\s+", "").trim();
    }
}