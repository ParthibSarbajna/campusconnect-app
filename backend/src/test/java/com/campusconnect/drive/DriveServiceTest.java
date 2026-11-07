
package com.campusconnect.drive;

import static com.campusconnect.drive.PlacementFixtures.NOW;
import static com.campusconnect.drive.PlacementFixtures.company;
import static com.campusconnect.drive.PlacementFixtures.drive;
import static com.campusconnect.drive.PlacementFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.application.ApplicationRepository;
import com.campusconnect.common.ApiException;
import com.campusconnect.company.CompanyRepository;
import com.campusconnect.drive.DriveDtos.DriveRequest;
import com.campusconnect.drive.DriveDtos.DriveResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DriveServiceTest {

    private final DriveRepository drives = mock(DriveRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final ApplicationRepository applications = mock(ApplicationRepository.class);

    private final DriveService service = new DriveService(
            drives,
            companies,
            applications,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private void activeCompany() {
        when(companies.findById(1L))
                .thenReturn(Optional.of(company(1, "Acme Corp", true)));

        when(companies.findAllById(any()))
                .thenReturn(List.of(company(1, "Acme Corp", true)));
    }

    private static HttpStatus statusOf(Throwable t) {
        return ((ApiException) t).getStatus();
    }

    @Test
    void createStoresATrimmedDraftWithDeduplicatedBranches() {
        activeCompany();

        when(drives.saveAndFlush(any(PlacementDrive.class)))
                .thenAnswer(i -> i.getArgument(0));

        DriveResponse r = service.create(request(1));

        assertThat(r.status()).isEqualTo(DriveStatus.DRAFT);
        assertThat(r.title()).isEqualTo("Software Engineer");
        assertThat(r.branches()).containsExactly("CSE", "IT");
        assertThat(r.companyName()).isEqualTo("Acme Corp");
    }

    @Test
    void createRejectsUnknownOrInactiveCompany() {
        when(companies.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(1)))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        when(companies.findById(2L))
                .thenReturn(Optional.of(company(2, "Dormant", false)));

        assertThatThrownBy(() -> service.create(request(2)))
                .isInstanceOf(ApiException.class);

        verify(drives, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsADeadlineInThePast() {
        activeCompany();

        DriveRequest past = new DriveRequest(
                1L, "Role", null, null, null, 2027,
                null, null, null, NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.create(past))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void lifecycleMovesAreLimitedToDraftOpenClosedAndCancelled() {
        assertThat(DriveService.canMove(DriveStatus.DRAFT, DriveStatus.OPEN)).isTrue();
        assertThat(DriveService.canMove(DriveStatus.DRAFT, DriveStatus.CANCELLED)).isTrue();
        assertThat(DriveService.canMove(DriveStatus.OPEN, DriveStatus.CLOSED)).isTrue();
        assertThat(DriveService.canMove(DriveStatus.OPEN, DriveStatus.CANCELLED)).isTrue();

        assertThat(DriveService.canMove(DriveStatus.DRAFT, DriveStatus.CLOSED)).isFalse();
        assertThat(DriveService.canMove(DriveStatus.OPEN, DriveStatus.DRAFT)).isFalse();
        assertThat(DriveService.canMove(DriveStatus.CLOSED, DriveStatus.OPEN)).isFalse();
        assertThat(DriveService.canMove(DriveStatus.CANCELLED, DriveStatus.OPEN)).isFalse();
    }

    @Test
    void publishingADraftOpensIt() {
        activeCompany();

        PlacementDrive d = drive(5, 1, DriveStatus.DRAFT);

        when(drives.findById(5L)).thenReturn(Optional.of(d));
        when(drives.save(any(PlacementDrive.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.changeStatus(5L, DriveStatus.OPEN).status())
                .isEqualTo(DriveStatus.OPEN);
    }

    @Test
    void publishingIsBlockedForAnInactiveCompanyOrAPastDeadline() {
        PlacementDrive d = drive(5, 1, DriveStatus.DRAFT);

        when(drives.findById(5L)).thenReturn(Optional.of(d));
        when(companies.findById(1L))
                .thenReturn(Optional.of(company(1, "Dormant", false)));

        assertThatThrownBy(() -> service.changeStatus(5L, DriveStatus.OPEN))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.CONFLICT));

        PlacementDrive stale = new PlacementDrive();

        stale.applyDetails(
                1L, "Role", null, null, null, (short) 2027,
                null, null, java.util.Set.of(), NOW.minusSeconds(5));

        when(companies.findById(1L))
                .thenReturn(Optional.of(company(1, "Acme Corp", true)));

        when(drives.findById(6L)).thenReturn(Optional.of(stale));

        assertThatThrownBy(() -> service.changeStatus(6L, DriveStatus.OPEN))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void illegalTransitionIsAConflict() {
        PlacementDrive d = drive(5, 1, DriveStatus.CLOSED);

        when(drives.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.changeStatus(5L, DriveStatus.OPEN))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(drives, never()).save(any());
    }

    @Test
    void openDriveFreezesEligibilityCriteriaButAllowsADeadlineExtension() {
        activeCompany();

        PlacementDrive d = drive(5, 1, DriveStatus.OPEN);

        when(drives.findById(5L)).thenReturn(Optional.of(d));
        when(drives.saveAndFlush(any(PlacementDrive.class)))
                .thenAnswer(i -> i.getArgument(0));

        DriveRequest changedCgpa = new DriveRequest(
                1L, "Software Engineer", null, null,
                new BigDecimal("8.50"), 2027,
                new BigDecimal("6.00"), 0,
                List.of("CSE", "IT"), NOW.plusSeconds(86_400));

        assertThatThrownBy(() -> service.update(5L, changedCgpa))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.CONFLICT));

        DriveRequest extended = new DriveRequest(
                1L, "Software Engineer", "New text", null,
                new BigDecimal("8.50"), 2027,
                new BigDecimal("7.50"), 0,
                List.of("it", "cse"), NOW.plusSeconds(5 * 86_400));

        DriveResponse r = service.update(5L, extended);

        assertThat(r.applicationDeadline()).isEqualTo(NOW.plusSeconds(5 * 86_400));
        assertThat(r.description()).isEqualTo("New text");
    }

    @Test
    void closedDrivesCannotBeEdited() {
        when(drives.findById(5L))
                .thenReturn(Optional.of(drive(5, 1, DriveStatus.CLOSED)));

        assertThatThrownBy(() -> service.update(5L, request(1)))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void missingDriveIs404() {
        when(drives.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(9L))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void cancelledDriveWithoutApplicationsCanBeDeleted() {
        PlacementDrive d = drive(5, 1, DriveStatus.CANCELLED);

        when(drives.findById(5L)).thenReturn(Optional.of(d));
        when(applications.existsByDriveId(5L)).thenReturn(false);

        service.deleteCancelled(5L);

        verify(drives).delete(d);
        verify(drives).flush();
    }

    @Test
    void nonCancelledDriveCannotBeDeleted() {
        PlacementDrive d = drive(5, 1, DriveStatus.OPEN);

        when(drives.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.deleteCancelled(5L))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(applications, never()).existsByDriveId(5L);
        verify(drives, never()).delete(any(PlacementDrive.class));
        verify(drives, never()).flush();
    }

    @Test
    void cancelledDriveWithApplicationsCannotBeDeleted() {
        PlacementDrive d = drive(5, 1, DriveStatus.CANCELLED);

        when(drives.findById(5L)).thenReturn(Optional.of(d));
        when(applications.existsByDriveId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCancelled(5L))
                .satisfies(t -> assertThat(statusOf(t))
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(drives, never()).delete(any(PlacementDrive.class));
        verify(drives, never()).flush();
    }
}
