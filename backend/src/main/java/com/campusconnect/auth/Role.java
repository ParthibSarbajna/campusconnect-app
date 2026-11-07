package com.campusconnect.auth;

public enum Role {
    STUDENT, DEPARTMENT_SPOC, PLACEMENT_ADMIN;

    /** Spring Security authority string, e.g. ROLE_STUDENT. */
    public String authority() {
        return "ROLE_" + name();
    }
}
