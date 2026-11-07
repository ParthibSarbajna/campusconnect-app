package com.campusconnect.application;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    APPLIED, SHORTLISTED, SELECTED, REJECTED, WITHDRAWN;

    /** Moves an administrator may make. SELECTED, REJECTED and WITHDRAWN are final. */
    public Set<ApplicationStatus> adminTargets() {
        return switch (this) {
            case APPLIED -> EnumSet.of(SHORTLISTED, REJECTED);
            case SHORTLISTED -> EnumSet.of(SELECTED, REJECTED);
            default -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    /** A student may withdraw only while the application is still undecided. */
    public boolean canWithdraw() {
        return this == APPLIED || this == SHORTLISTED;
    }
}
