
package com.campusconnect.drive;

import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.drive.DriveDtos.DriveRequest;
import com.campusconnect.drive.DriveDtos.DriveResponse;
import com.campusconnect.drive.DriveDtos.StatusRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/drives")
@PreAuthorize("hasRole('PLACEMENT_ADMIN')")
public class AdminDriveController {

    private final DriveService drives;

    public AdminDriveController(DriveService drives) {
        this.drives = drives;
    }

    @GetMapping
    public PageResponse<DriveResponse> list(
            @RequestParam(required = false) DriveStatus status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer graduationYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        return drives.list(status, companyId, graduationYear, page, size);
    }

    @GetMapping("/{id}")
    public DriveResponse get(@PathVariable Long id) {
        return drives.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriveResponse create(@Valid @RequestBody DriveRequest request) {
        return drives.create(request);
    }

    @PutMapping("/{id}")
    public DriveResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DriveRequest request) {

        return drives.update(id, request);
    }

    @PostMapping("/{id}/status")
    public DriveResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {

        return drives.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        drives.deleteCancelled(id);
    }
}
