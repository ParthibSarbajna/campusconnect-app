package com.campusconnect.auth;

import java.io.Serializable;

/** The authenticated principal stored in the server-side session. Contains no credentials. */
public record AuthUser(Long id, String email, String fullName, Role role, Long departmentId) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static AuthUser from(UserAccount u) {
        return new AuthUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getDepartmentId());
    }

    public boolean isAdmin() {
        return role == Role.PLACEMENT_ADMIN;
    }
}
