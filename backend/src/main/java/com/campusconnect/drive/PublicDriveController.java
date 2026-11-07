package com.campusconnect.drive;

import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.drive.DriveDtos.DriveResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/drives")
public class PublicDriveController {

    private final DriveService drives;

    public PublicDriveController(DriveService drives) {
        this.drives = drives;
    }

    @GetMapping
    public PageResponse<DriveResponse> listOpenDrives(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return drives.list(DriveStatus.OPEN, null, null, page, size);
    }
}