package com.campusconnect.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.auth.AuthUser;
import com.campusconnect.auth.Role;
import com.campusconnect.common.ApiException;
import com.campusconnect.student.StudentProfileDtos.StudentProfileResponse;
import com.campusconnect.student.StudentProfileDtos.UpdateStudentProfileRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StudentProfileServiceTest {

    private final StudentProfileRepository profiles =
            mock(StudentProfileRepository.class);

    private final StudentProfileService service =
            new StudentProfileService(profiles);

    private final AuthUser student =
            new AuthUser(7L, "student@example.edu", "Test Student",
                    Role.STUDENT, null);

    @Test
    void missingProfileReturnsNotFound() {
        when(profiles.findByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile(student))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void updateCreatesProfileForAuthenticatedUser() {
        when(profiles.findByUserId(7L)).thenReturn(Optional.empty());
        when(profiles.existsByRollNumberIgnoreCase("CC001"))
                .thenReturn(false);
        when(profiles.saveAndFlush(any(StudentProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStudentProfileRequest request =
                new UpdateStudentProfileRequest(
                        "CC001", "9876543210", null,
                        "B.Tech", "CSE", (short) 2027,
                        new BigDecimal("8.50"), (short) 0, null);

        StudentProfileResponse response =
                service.updateMyProfile(student, request);

        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.rollNumber()).isEqualTo("CC001");
        assertThat(response.cgpa()).isEqualByComparingTo("8.50");
        verify(profiles).saveAndFlush(any(StudentProfile.class));
    }

    @Test
    void duplicateRollNumberIsRejected() {
        StudentProfile existing = new StudentProfile(7L);
        existing.updateDetails(
                "CC001", null, null, null, null,
                null, null, (short) 0, null);

        when(profiles.findByUserId(7L)).thenReturn(Optional.of(existing));
        when(profiles.existsByRollNumberIgnoreCase("CC002"))
                .thenReturn(true);

        UpdateStudentProfileRequest request =
                new UpdateStudentProfileRequest(
                        "CC002", null, null, null, null,
                        null, null, (short) 0, null);

        assertThatThrownBy(() -> service.updateMyProfile(student, request))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(profiles, never()).saveAndFlush(any(StudentProfile.class));
    }

    @Test
    void blankRollNumberIsNormalizedToNull() {
        when(profiles.findByUserId(7L)).thenReturn(Optional.empty());
        when(profiles.saveAndFlush(any(StudentProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStudentProfileRequest request =
                new UpdateStudentProfileRequest(
                        "   ", null, null, null, null,
                        null, null, null, null);

        StudentProfileResponse response =
                service.updateMyProfile(student, request);

        assertThat(response.rollNumber()).isNull();
        assertThat(response.activeBacklogs()).isZero();
    }
}