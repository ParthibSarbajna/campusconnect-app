package com.campusconnect.application;

import static com.campusconnect.drive.PlacementFixtures.NOW;
import static com.campusconnect.drive.PlacementFixtures.company;
import static com.campusconnect.drive.PlacementFixtures.drive;
import static com.campusconnect.drive.PlacementFixtures.profile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.application.ApplicationDtos.ApplicationResponse;
import com.campusconnect.auth.AuthUser;
import com.campusconnect.auth.Role;
import com.campusconnect.auth.UserRepository;
import com.campusconnect.common.ApiException;
import com.campusconnect.company.CompanyRepository;
import com.campusconnect.department.DepartmentRepository;
import com.campusconnect.drive.DriveRepository;
import com.campusconnect.drive.DriveStatus;
import com.campusconnect.drive.PlacementDrive;
import com.campusconnect.student.StudentProfileRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class ApplicationServiceTest {

    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final ApplicationHistoryRepository history = mock(ApplicationHistoryRepository.class);
    private final DriveRepository drives = mock(DriveRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final StudentProfileRepository profiles = mock(StudentProfileRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final ApplicationService service = new ApplicationService(applications, history, drives, companies, profiles,
            users, departments, Clock.fixed(NOW, ZoneOffset.UTC));

    private final AuthUser student = new AuthUser(10L, "s@example.edu", "Student", Role.STUDENT, 1L);
    private final AuthUser admin = new AuthUser(1L, "a@example.edu", "Admin", Role.PLACEMENT_ADMIN, null);

    private static HttpStatus statusOf(Throwable t) {
        return ((ApiException) t).getStatus();
    }

    private PlacementDrive openDrive() {
        PlacementDrive d = drive(5, 1, DriveStatus.OPEN);
        when(drives.findById(5L)).thenReturn(Optional.of(d));
        when(companies.findById(1L)).thenReturn(Optional.of(company(1, "Acme Corp", true)));
        return d;
    }

    private Application savedApplication(long id, long studentId, ApplicationStatus status) {
        Application a = new Application(5L, studentId, NOW);
        a.setStatus(status);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    // ---- apply ----

    @Test
    void eligibleStudentCanApplyAndTheFirstHistoryRowIsWritten() {
        openDrive();
        when(profiles.findByUserId(10L)).thenReturn(Optional.of(profile(10, "CSE", 2027, "8.00", 0)));
        when(applications.saveAndFlush(any(Application.class))).thenAnswer(i -> {
            Application a = i.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 77L);
            return a;
        });
        ApplicationResponse r = service.apply(student, 5L);
        assertThat(r.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(r.companyName()).isEqualTo("Acme Corp");
        ArgumentCaptor<ApplicationStatusHistory> h = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(history).save(h.capture());
        assertThat(h.getValue().getFromStatus()).isNull();
        assertThat(h.getValue().getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(h.getValue().getChangedBy()).isEqualTo(10L);
    }

    @Test
    void ineligibleStudentIsRejectedWithTheReasonsAndNothingIsSaved() {
        openDrive();
        when(profiles.findByUserId(10L)).thenReturn(Optional.of(profile(10, "CSE", 2027, "6.00", 0)));
        assertThatThrownBy(() -> service.apply(student, 5L)).satisfies(t -> {
            assertThat(statusOf(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(t.getMessage()).contains("below the required");
        });
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void studentWithoutAProfileCannotApply() {
        openDrive();
        when(profiles.findByUserId(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply(student, 5L)).satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void duplicateApplicationIsAConflict() {
        openDrive();
        when(applications.existsByDriveIdAndStudentUserId(5L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> service.apply(student, 5L)).satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void aRaceOnTheUniqueConstraintIsAlsoAConflict() {
        openDrive();
        when(profiles.findByUserId(10L)).thenReturn(Optional.of(profile(10, "CSE", 2027, "8.00", 0)));
        when(applications.saveAndFlush(any(Application.class))).thenThrow(new DataIntegrityViolationException("uq_applications_drive_student"));
        assertThatThrownBy(() -> service.apply(student, 5L)).satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void applicationsAfterTheDeadlineAreRejected() {
        PlacementDrive d = new PlacementDrive();
        d.applyDetails(1L, "Role", null, null, null, (short) 2027, null, null, java.util.Set.of(), NOW.minusSeconds(1));
        d.setStatus(DriveStatus.OPEN);
        when(drives.findById(5L)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.apply(student, 5L)).satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void draftClosedAndCancelledDrivesLookLikeTheyDoNotExist() {
        for (DriveStatus s : List.of(DriveStatus.DRAFT, DriveStatus.CLOSED, DriveStatus.CANCELLED)) {
            when(drives.findById(5L)).thenReturn(Optional.of(drive(5, 1, s)));
            assertThatThrownBy(() -> service.apply(student, 5L)).satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ---- withdraw (ownership) ----

    @Test
    void studentCanWithdrawTheirOwnUndecidedApplication() {
        Application a = savedApplication(7, 10, ApplicationStatus.APPLIED);
        when(applications.findByIdAndStudentUserId(7L, 10L)).thenReturn(Optional.of(a));
        when(drives.findById(5L)).thenReturn(Optional.of(drive(5, 1, DriveStatus.OPEN)));
        when(companies.findById(1L)).thenReturn(Optional.of(company(1, "Acme Corp", true)));
        assertThat(service.withdraw(student, 7L).status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        verify(history).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void anotherStudentsApplicationLooksLikeItDoesNotExist() {
        when(applications.findByIdAndStudentUserId(7L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.withdraw(student, 7L)).satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.NOT_FOUND));
        verify(applications, never()).save(any());
    }

    @Test
    void decidedApplicationsCannotBeWithdrawn() {
        for (ApplicationStatus s : List.of(ApplicationStatus.SELECTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN)) {
            when(applications.findByIdAndStudentUserId(7L, 10L)).thenReturn(Optional.of(savedApplication(7, 10, s)));
            assertThatThrownBy(() -> service.withdraw(student, 7L)).satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    // ---- administrator status changes ----

    @Test
    void adminMovesAnApplicationThroughTheAllowedPath() {
        Application a = savedApplication(7, 10, ApplicationStatus.APPLIED);
        when(applications.findById(7L)).thenReturn(Optional.of(a));
        service.changeStatus(admin, 7L, ApplicationStatus.SHORTLISTED, " good fit ");
        service.changeStatus(admin, 7L, ApplicationStatus.SELECTED, null);
        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.SELECTED);
        ArgumentCaptor<ApplicationStatusHistory> h = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(history, org.mockito.Mockito.times(2)).save(h.capture());
        assertThat(h.getAllValues().get(0).getNote()).isEqualTo("good fit");
        assertThat(h.getAllValues().get(1).getFromStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
    }

    @Test
    void adminCannotSkipShortlistingOrReviveAFinalApplication() {
        Application fresh = savedApplication(7, 10, ApplicationStatus.APPLIED);
        when(applications.findById(7L)).thenReturn(Optional.of(fresh));
        assertThatThrownBy(() -> service.changeStatus(admin, 7L, ApplicationStatus.SELECTED, null))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
        Application done = savedApplication(8, 10, ApplicationStatus.REJECTED);
        when(applications.findById(8L)).thenReturn(Optional.of(done));
        assertThatThrownBy(() -> service.changeStatus(admin, 8L, ApplicationStatus.SHORTLISTED, null)).isInstanceOf(ApiException.class);
        verify(history, never()).save(any());
    }

    // ---- listing scope ----

    @Test
    void aSpocWithoutADepartmentAndOtherRolesCannotListApplications() {
        AuthUser orphanSpoc = new AuthUser(3L, "p@example.edu", "SPOC", Role.DEPARTMENT_SPOC, null);
        assertThatThrownBy(() -> service.list(orphanSpoc, null, null, 0, 25)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.list(student, null, null, 0, 25)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminStateTransitionTableMatchesThePolicy() {
        assertThat(ApplicationStatus.APPLIED.adminTargets()).containsExactlyInAnyOrder(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED);
        assertThat(ApplicationStatus.SHORTLISTED.adminTargets()).containsExactlyInAnyOrder(ApplicationStatus.SELECTED, ApplicationStatus.REJECTED);
        assertThat(ApplicationStatus.SELECTED.adminTargets()).isEmpty();
        assertThat(ApplicationStatus.APPLIED.canWithdraw()).isTrue();
        assertThat(ApplicationStatus.SELECTED.canWithdraw()).isFalse();
    }
}
