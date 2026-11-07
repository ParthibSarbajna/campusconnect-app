package com.campusconnect.application;

import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.application.ApplicationDtos.ApplicationRow;
import com.campusconnect.application.ApplicationDtos.StatusRequest;
import com.campusconnect.auth.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApplicationManagementController {

    private final ApplicationService applications;

    public ApplicationManagementController(ApplicationService applications) {
        this.applications = applications;
    }

    /** Admin: all applications. SPOC: only students of their own department (enforced in the service query). */
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('DEPARTMENT_SPOC','PLACEMENT_ADMIN')")
    public PageResponse<ApplicationRow> list(@AuthenticationPrincipal AuthUser me,
                                             @RequestParam(required = false) Long driveId,
                                             @RequestParam(required = false) ApplicationStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "25") int size) {
        return applications.list(me, driveId, status, page, size);
    }

    @PostMapping("/admin/applications/{id}/status")
    @PreAuthorize("hasRole('PLACEMENT_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                             @Valid @RequestBody StatusRequest request) {
        applications.changeStatus(me, id, request.status(), request.note());
    }
}
