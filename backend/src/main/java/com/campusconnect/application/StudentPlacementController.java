package com.campusconnect.application;

import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.application.ApplicationDtos.ApplicationResponse;
import com.campusconnect.auth.AuthUser;
import com.campusconnect.drive.DriveDtos.StudentDriveView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Everything a student can do with drives and their own applications. STUDENT role only. */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('STUDENT')")
public class StudentPlacementController {

    private final StudentDriveService driveViews;
    private final ApplicationService applications;

    public StudentPlacementController(StudentDriveService driveViews, ApplicationService applications) {
        this.driveViews = driveViews;
        this.applications = applications;
    }

    @GetMapping("/drives")
    public PageResponse<StudentDriveView> drives(@AuthenticationPrincipal AuthUser me,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return driveViews.list(me, page, size);
    }

    @GetMapping("/drives/{id}")
    public StudentDriveView drive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return driveViews.get(me, id);
    }

    @PostMapping("/drives/{id}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return applications.apply(me, id);
    }

    @GetMapping("/applications/me")
    public PageResponse<ApplicationResponse> mine(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return applications.mine(me, page, size);
    }

    @PostMapping("/applications/{id}/withdraw")
    public ApplicationResponse withdraw(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return applications.withdraw(me, id);
    }
}
