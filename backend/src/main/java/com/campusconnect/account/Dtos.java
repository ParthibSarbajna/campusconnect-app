
package com.campusconnect.account;

import com.campusconnect.auth.Role;
import com.campusconnect.auth.UserAccount;
import com.campusconnect.department.Department;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request/response DTOs. Entities are never serialized directly, so password hashes cannot leak. */
public final class Dtos {
    private Dtos() {}

    static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{10,72}$";
    static final String PASSWORD_MSG = "Password must be 10-72 characters and include a letter and a digit.";

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String password) {

        public RegisterRequest {
            if (email != null) {
                email = email.trim();
            }
        }
    }

    public record LoginRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String password) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName) {}

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 200) String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String newPassword) {}

    public record CreateUserRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String password,
            @NotNull Role role,
            Long departmentId) {}

    /**
     * PATCH body for admin account updates.
     * A field that is ABSENT from the JSON is left unchanged. An explicit {"departmentId": null}
     * CLEARS the department. A plain record cannot tell "absent" from "null" (both arrive as null),
     * which is why presence of departmentId is recorded by its setter.
     */
    public static final class UpdateUserRequest {
        private Role role;
        private Long departmentId;
        private boolean departmentIdProvided;
        private Boolean enabled;

        public UpdateUserRequest() {}

        public UpdateUserRequest(Role role, Long departmentId, boolean departmentIdProvided, Boolean enabled) {
            this.role = role;
            this.departmentId = departmentId;
            this.departmentIdProvided = departmentIdProvided;
            this.enabled = enabled;
        }

        @JsonProperty("role")
        public void setRole(Role role) { this.role = role; }

        @JsonProperty("departmentId")
        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
            this.departmentIdProvided = true; // Jackson calls this for an explicit null too, but not when absent
        }

        @JsonProperty("enabled")
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public Role role() { return role; }
        public Long departmentId() { return departmentId; }
        public boolean hasDepartmentId() { return departmentIdProvided; }
        public Boolean enabled() { return enabled; }
    }

    public record DepartmentRequest(
            @NotBlank @Pattern(
                    regexp = "^[A-Za-z0-9_-]{2,16}$",
                    message = "Code must be 2-16 letters, digits, - or _"
            ) String code,
            @NotBlank @Size(max = 120) String name) {}

    public record UserResponse(
            Long id,
            String email,
            String fullName,
            Role role,
            Long departmentId,
            boolean enabled) {

        public static UserResponse from(UserAccount u) {
            return new UserResponse(
                    u.getId(),
                    u.getEmail(),
                    u.getFullName(),
                    u.getRole(),
                    u.getDepartmentId(),
                    u.isEnabled()
            );
        }
    }

    public record DepartmentResponse(
            Long id,
            String code,
            String name,
            boolean active) {

        public static DepartmentResponse from(Department d) {
            return new DepartmentResponse(
                    d.getId(),
                    d.getCode(),
                    d.getName(),
                    d.isActive()
            );
        }
    }

    public record CsrfResponse(String headerName, String token) {}

    public record PageResponse<T>(
            List<T> items,
            int page,
            int size,
            long total) {}
}
