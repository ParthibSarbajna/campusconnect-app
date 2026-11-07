package com.campusconnect.account;

import com.campusconnect.account.Dtos.ChangePasswordRequest;
import com.campusconnect.account.Dtos.CreateUserRequest;
import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.account.Dtos.UpdateProfileRequest;
import com.campusconnect.account.Dtos.UpdateUserRequest;
import com.campusconnect.account.Dtos.UserResponse;
import com.campusconnect.auth.AuthService;
import com.campusconnect.auth.AuthUser;
import com.campusconnect.auth.Role;
import com.campusconnect.auth.UserAccount;
import com.campusconnect.auth.UserRepository;
import com.campusconnect.common.ApiException;
import com.campusconnect.common.PasswordPolicy;
import com.campusconnect.department.DepartmentRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Profile, password, ownership-checked reads, department-scoped reads and admin account management. */
@Service
public class AccountService {

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public AccountService(UserRepository users, DepartmentRepository departments, PasswordEncoder encoder, Clock clock) {
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
        this.clock = clock;
    }

    public Optional<UserAccount> findActive(Long id) {
        return users.findById(id).filter(UserAccount::isEnabled);
    }

    /** Only the display name can be changed here. Role, department and enabled state are never touched. */
    public UserResponse updateProfile(AuthUser actor, UpdateProfileRequest r) {
        UserAccount u = users.findById(actor.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not signed in."));
        u.setFullName(r.fullName().trim());
        return UserResponse.from(users.save(u));
    }

    public void changePassword(AuthUser actor, ChangePasswordRequest r) {
        UserAccount u = users.findById(actor.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not signed in."));
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        PasswordPolicy.validate(r.newPassword());
        u.setPasswordHash(encoder.encode(r.newPassword()));
        users.save(u);
    }

    /** A user may read only their own record; admins may read any. Everyone else gets 403. */
    public UserResponse getUser(AuthUser actor, Long id) {
        if (!actor.isAdmin() && !actor.id().equals(id)) {
            throw new AccessDeniedException("Not allowed to read another user's record");
        }
        return users.findById(id).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    /** SPOCs see only their own department; admins see any existing department. */
    public PageResponse<UserResponse> listDepartmentUsers(AuthUser actor, Long departmentId, int page, int size) {
        if (!actor.isAdmin()) {
            if (actor.role() != Role.DEPARTMENT_SPOC || !departmentId.equals(actor.departmentId())) {
                throw new AccessDeniedException("Outside your department");
            }
        } else if (!departments.existsById(departmentId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Department not found.");
        }
        return toPage(users.findByDepartmentId(departmentId, pageable(page, size)));
    }

    // ---------------- admin operations ----------------

    public PageResponse<UserResponse> listUsers(int page, int size) {
        return toPage(users.findAll(pageable(page, size)));
    }

    public UserResponse createUser(CreateUserRequest r) {
        PasswordPolicy.validate(r.password());
        String email = AuthService.normalizeEmail(r.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        Long dept = resolveDepartment(r.role(), r.departmentId());
        UserAccount u = UserAccount.create(email, r.fullName().trim(), encoder.encode(r.password()), r.role(), dept, clock.instant());
        try {
            return UserResponse.from(users.saveAndFlush(u));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
    }

    public UserResponse updateUser(AuthUser admin, Long id, UpdateUserRequest r) {
        UserAccount u = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        boolean self = admin.id().equals(id);
        boolean changesRole = r.role() != null && r.role() != u.getRole();
        if (self && (changesRole || Boolean.FALSE.equals(r.enabled()))) {
            throw new ApiException(HttpStatus.CONFLICT, "You cannot change your own role or disable your own account.");
        }
        Role role = r.role() != null ? r.role() : u.getRole();
        // Absent field = keep the current department. Explicit null = clear it.
        Long dept = r.hasDepartmentId() ? r.departmentId() : u.getDepartmentId();
        Long resolvedDept = resolveDepartment(role, dept); // validate first, mutate after
        u.setRole(role);
        u.setDepartmentId(resolvedDept);
        if (r.enabled() != null) {
            u.setEnabled(r.enabled());
            if (r.enabled()) {
                u.clearFailures();
            }
        }
        return UserResponse.from(users.save(u));
    }

    private Long resolveDepartment(Role role, Long departmentId) {
        if (role == Role.PLACEMENT_ADMIN) {
            return null; // administrators are not department-scoped
        }
        if (role == Role.DEPARTMENT_SPOC && departmentId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A department is required for a Department SPOC.");
        }
        if (departmentId != null && !departments.existsById(departmentId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown department.");
        }
        return departmentId;
    }

    static Pageable pageable(int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100); // hard cap: never return unbounded lists
        return PageRequest.of(p, s, Sort.by("fullName").and(Sort.by("id")));
    }

    private static PageResponse<UserResponse> toPage(Page<UserAccount> page) {
        return new PageResponse<>(page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
