package com.campusconnect.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "roll_number", length = 40, unique = true)
    private String rollNumber;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 100)
    private String degree;

    @Column(length = 120)
    private String branch;

    @Column(name = "graduation_year")
    private Short graduationYear;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(name = "active_backlogs", nullable = false)
    private Short activeBacklogs = 0;

    // Legacy resume URL retained for compatibility
    @Column(name = "resume_url", length = 2048)
    private String resumeUrl;

    // Resume PDF storage
    @Column(name = "resume_file_name")
    private String resumeFileName;

    @Column(name = "resume_content_type")
    private String resumeContentType;

    @Column(name = "resume_data")
    private byte[] resumeData;

    @Column(name = "resume_uploaded_at")
    private Instant resumeUploadedAt;

    protected StudentProfile() {
    }

    public StudentProfile(Long userId) {
        this.userId = userId;
        this.activeBacklogs = 0;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDegree() {
        return degree;
    }

    public String getBranch() {
        return branch;
    }

    public Short getGraduationYear() {
        return graduationYear;
    }

    public BigDecimal getCgpa() {
        return cgpa;
    }

    public Short getActiveBacklogs() {
        return activeBacklogs;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public String getResumeContentType() {
        return resumeContentType;
    }

    public byte[] getResumeData() {
        return resumeData;
    }

    public Instant getResumeUploadedAt() {
        return resumeUploadedAt;
    }

    public boolean hasResume() {
        return resumeData != null && resumeData.length > 0;
    }

    public void storeResume(
            String fileName,
            String contentType,
            byte[] data) {

        this.resumeFileName = fileName;
        this.resumeContentType = contentType;
        this.resumeData = data;
        this.resumeUploadedAt = Instant.now();

        // New resume uploads supersede the old URL.
        this.resumeUrl = null;
    }

    public void removeResume() {
        this.resumeFileName = null;
        this.resumeContentType = null;
        this.resumeData = null;
        this.resumeUploadedAt = null;
        this.resumeUrl = null;
    }

    public void updateDetails(
            String rollNumber,
            String phone,
            LocalDate dateOfBirth,
            String degree,
            String branch,
            Short graduationYear,
            BigDecimal cgpa,
            Short activeBacklogs,
            String resumeUrl) {

        this.rollNumber = rollNumber;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.degree = degree;
        this.branch = branch;
        this.graduationYear = graduationYear;
        this.cgpa = cgpa;
        this.activeBacklogs = activeBacklogs == null ? 0 : activeBacklogs;

        // Preserve uploaded resume; only update the legacy URL if provided.
        if (resumeData == null) {
            this.resumeUrl = resumeUrl;
        }
    }
}