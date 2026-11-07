package com.campusconnect.account;

import com.campusconnect.account.Dtos.CreateUserRequest;
import com.campusconnect.account.Dtos.DepartmentRequest;
import com.campusconnect.account.Dtos.DepartmentResponse;
import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.account.Dtos.UpdateUserRequest;
import com.campusconnect.account.Dtos.UserResponse;
import com.campusconnect.auth.AuthUser;
import com.campusconnect.department.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Protected twice: by URL rule in SecurityConfig and by @PreAuthorize here. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('PLACEMENT_ADMIN')")
public class AdminController {

    private final AccountService accounts;
    private final DepartmentService departments;

    public AdminController(AccountService accounts, DepartmentService departments) {
        this.accounts = accounts;
        this.departments = departments;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> listUsers(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "25") int size) {
        return accounts.listUsers(page, size);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return accounts.createUser(request);
    }

    @PatchMapping("/users/{id}")
    public UserResponse updateUser(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                                   @RequestBody UpdateUserRequest request) {
        return accounts.updateUser(admin, id, request);
    }

    @GetMapping("/departments")
    public List<DepartmentResponse> listDepartments() {
        return departments.list();
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return departments.create(request);
    }
}
