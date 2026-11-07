package com.campusconnect.student;

import com.campusconnect.auth.AuthUser;
import com.campusconnect.student.StudentProfileDtos.StudentProfileResponse;
import com.campusconnect.student.StudentProfileDtos.UpdateStudentProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/students/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileController {

    private final StudentProfileService profiles;

    public StudentProfileController(StudentProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public StudentProfileResponse getMyProfile(
            @AuthenticationPrincipal AuthUser me) {
        return profiles.getMyProfile(me);
    }

    @PutMapping
    public StudentProfileResponse updateMyProfile(
            @AuthenticationPrincipal AuthUser me,
            @Valid @RequestBody UpdateStudentProfileRequest request) {
        return profiles.updateMyProfile(me, request);
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadResume(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam("file") MultipartFile file) {
        profiles.uploadResume(me, file);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resume/metadata")
    public StudentProfileService.ResumeMetadata resumeMetadata(
            @AuthenticationPrincipal AuthUser me) {
        return profiles.getResumeMetadata(me);
    }

    @GetMapping("/resume")
    public ResponseEntity<byte[]> downloadResume(
            @AuthenticationPrincipal AuthUser me) {
        StudentProfileService.ResumeFile resume = profiles.getMyResume(me);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(resume.fileName())
                                .build()
                                .toString())
                .body(resume.data());
    }

    @GetMapping("/resume/view")
    public ResponseEntity<byte[]> viewResume(
            @AuthenticationPrincipal AuthUser me) {
        StudentProfileService.ResumeFile resume = profiles.getMyResume(me);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(resume.fileName())
                                .build()
                                .toString())
                .body(resume.data());
    }

    @DeleteMapping("/resume")
    public ResponseEntity<Void> deleteResume(
            @AuthenticationPrincipal AuthUser me) {
        profiles.deleteMyResume(me);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resume/analyze")
    public StudentProfileService.ResumeAnalysis analyzeResume(
            @AuthenticationPrincipal AuthUser me,
            @RequestBody ResumeAnalysisRequest request) {
        return profiles.analyzeResume(me, request.jobDescription());
    }

    public record ResumeAnalysisRequest(String jobDescription) {
    }
}
