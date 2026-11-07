package com.campusconnect.student;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class StudentProfileDtos {

    private StudentProfileDtos() {
    }

    public record UpdateStudentProfileRequest(
            @Size(max = 40) String rollNumber,
            @Size(max = 20) String phone,
            @Past LocalDate dateOfBirth,
            @Size(max = 100) String degree,
            @Size(max = 120) String branch,
            @Min(2000) @Max(2100) Short graduationYear,
            @DecimalMin("0.00") @DecimalMax("10.00") BigDecimal cgpa,
            @PositiveOrZero Short activeBacklogs,
            @Size(max = 2048) String resumeUrl
    ) {
    }

    public record StudentProfileResponse(
            Long id,
            Long userId,
            String rollNumber,
            String phone,
            LocalDate dateOfBirth,
            String degree,
            String branch,
            Short graduationYear,
            BigDecimal cgpa,
            Short activeBacklogs,
            String resumeUrl
    ) {
        public static StudentProfileResponse from(StudentProfile p) {
            return new StudentProfileResponse(
                    p.getId(),
                    p.getUserId(),
                    p.getRollNumber(),
                    p.getPhone(),
                    p.getDateOfBirth(),
                    p.getDegree(),
                    p.getBranch(),
                    p.getGraduationYear(),
                    p.getCgpa(),
                    p.getActiveBacklogs(),
                    p.getResumeUrl()
            );
        }
    }
}