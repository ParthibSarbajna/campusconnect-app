package com.campusconnect.account;

import com.campusconnect.account.Dtos.ChangePasswordRequest;
import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.account.Dtos.UpdateProfileRequest;
import com.campusconnect.account.Dtos.UserResponse;
import com.campusconnect.auth.AuthUser;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PatchMapping("/users/me")
    public UserResponse updateMe(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest request) {
        return accounts.updateProfile(me, request);
    }

    @PostMapping("/users/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest request) {
        accounts.changePassword(me, request);
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return accounts.getUser(me, id);
    }

    @GetMapping("/departments/{departmentId}/users")
    @PreAuthorize("hasAnyRole('DEPARTMENT_SPOC','PLACEMENT_ADMIN')")
    public PageResponse<UserResponse> departmentUsers(@AuthenticationPrincipal AuthUser me,
                                                      @PathVariable Long departmentId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size) {
        return accounts.listDepartmentUsers(me, departmentId, page, size);
    }
}
