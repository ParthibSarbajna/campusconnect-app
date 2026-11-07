package com.campusconnect.application;

import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.application.ApplicationDtos.ApplicationResponse;
import com.campusconnect.application.ApplicationDtos.ApplicationRow;
import com.campusconnect.auth.AuthUser;
import com.campusconnect.auth.Role;
import com.campusconnect.auth.UserAccount;
import com.campusconnect.auth.UserRepository;
import com.campusconnect.common.ApiException;
import com.campusconnect.company.CompanyRepository;
import com.campusconnect.department.DepartmentRepository;
import com.campusconnect.drive.DriveRepository;
import com.campusconnect.drive.DriveService;
import com.campusconnect.drive.DriveStatus;
import com.campusconnect.drive.EligibilityEvaluator;
import com.campusconnect.drive.EligibilityResult;
import com.campusconnect.drive.PlacementDrive;
import com.campusconnect.student.StudentProfile;
import com.campusconnect.student.StudentProfileRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applications;
    private final ApplicationHistoryRepository history;
    private final DriveRepository drives;
    private final CompanyRepository companies;
    private final StudentProfileRepository profiles;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final Clock clock;

    public ApplicationService(
            ApplicationRepository applications,
            ApplicationHistoryRepository history,
            DriveRepository drives,
            CompanyRepository companies,
            StudentProfileRepository profiles,
            UserRepository users,
            DepartmentRepository departments,
            Clock clock) {

        this.applications = applications;
        this.history = history;
        this.drives = drives;
        this.companies = companies;
        this.profiles = profiles;
        this.users = users;
        this.departments = departments;
        this.clock = clock;
    }

    // ---------------- student actions ----------------

    /** Applies the student to an OPEN drive. */
    public ApplicationResponse apply(AuthUser student, Long driveId) {

        PlacementDrive drive = drives.findById(driveId)
                .filter(d -> d.getStatus() == DriveStatus.OPEN)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Drive not found."));

        Instant now = clock.instant();

        if (!drive.getApplicationDeadline().isAfter(now)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "The application deadline for this drive has passed.");
        }

        if (applications.existsByDriveIdAndStudentUserId(
                driveId, student.id())) {
            throw duplicate();
        }

        StudentProfile profile =
                profiles.findByUserId(student.id()).orElse(null);

        EligibilityResult eligibility =
                EligibilityEvaluator.evaluate(profile, drive);

        if (!eligibility.eligible()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "You are not eligible for this drive. "
                            + String.join(" ", eligibility.reasons()));
        }

        Application saved;

        try {
            saved = applications.saveAndFlush(
                    new Application(driveId, student.id(), now));
        } catch (DataIntegrityViolationException e) {
            throw duplicate();
        }

        history.save(new ApplicationStatusHistory(
                saved.getId(),
                null,
                ApplicationStatus.APPLIED,
                student.id(),
                now,
                null));

        return toResponse(
                saved,
                drive,
                companyName(drive.getCompanyId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> mine(
            AuthUser student, int page, int size) {

        Page<Application> result =
                applications.findByStudentUserId(
                        student.id(),
                        DriveService.pageable(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "appliedAt")));

        Map<Long, PlacementDrive> driveById =
                drivesById(result.getContent());

        Map<Long, String> names =
                companyNamesFor(driveById.values());

        List<ApplicationResponse> items =
                result.getContent().stream().map(a -> {
                    PlacementDrive d = driveById.get(a.getDriveId());

                    return toResponse(
                            a,
                            d,
                            names.get(d.getCompanyId()));
                }).toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    /** A student can withdraw only their own application. */
    public ApplicationResponse withdraw(
            AuthUser student, Long applicationId) {

        Application a =
                applications.findByIdAndStudentUserId(
                        applicationId,
                        student.id())
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "Application not found."));

        if (!a.getStatus().canWithdraw()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "An application that is "
                            + a.getStatus()
                            + " cannot be withdrawn.");
        }

        transition(
                a,
                ApplicationStatus.WITHDRAWN,
                student.id(),
                "Withdrawn by student");

        PlacementDrive d =
                drives.findById(a.getDriveId()).orElseThrow();

        return toResponse(
                a,
                d,
                companyName(d.getCompanyId()));
    }

    // ---------------- administrator / SPOC views ----------------

    /**
     * Admins see all applications.
     * A SPOC sees only applications belonging to students whose profile
     * branch matches the SPOC's department name.
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationRow> list(
            AuthUser actor,
            Long driveId,
            ApplicationStatus status,
            int page,
            int size) {

        String departmentName = null;

        if (actor.role() == Role.DEPARTMENT_SPOC) {

            if (actor.departmentId() == null) {
                throw new AccessDeniedException(
                        "SPOC has no department");
            }

            departmentName = departments
                    .findById(actor.departmentId())
                    .map(department -> department.getName())
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "SPOC department not found"));

        } else if (actor.role() != Role.PLACEMENT_ADMIN) {

            throw new AccessDeniedException("Not allowed");
        }

        Page<Application> result =
                applications.findAll(
                        filter(driveId, status, departmentName),
                        DriveService.pageable(
                                page,
                                size,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "appliedAt")));

        Map<Long, PlacementDrive> driveById =
                drivesById(result.getContent());

        Map<Long, String> names =
                companyNamesFor(driveById.values());

        List<Long> studentIds =
                result.getContent().stream()
                        .map(Application::getStudentUserId)
                        .distinct()
                        .toList();

        Map<Long, UserAccount> userById =
                users.findAllById(studentIds).stream()
                        .collect(Collectors.toMap(
                                UserAccount::getId,
                                Function.identity()));

        Map<Long, StudentProfile> profileByUser =
                profiles.findByUserIdIn(studentIds).stream()
                        .collect(Collectors.toMap(
                                StudentProfile::getUserId,
                                Function.identity()));

        List<ApplicationRow> rows =
                result.getContent().stream().map(a -> {

                    PlacementDrive d =
                            driveById.get(a.getDriveId());

                    UserAccount u =
                            userById.get(a.getStudentUserId());

                    StudentProfile p =
                            profileByUser.get(a.getStudentUserId());

                    return new ApplicationRow(
                            a.getId(),
                            d.getId(),
                            names.get(d.getCompanyId()),
                            d.getTitle(),
                            a.getStudentUserId(),
                            u == null ? null : u.getFullName(),
                            u == null ? null : u.getEmail(),
                            p == null ? null : p.getBranch(),
                            p == null || p.getGraduationYear() == null
                                    ? null
                                    : p.getGraduationYear().intValue(),
                            p == null ? null : p.getCgpa(),
                            a.getStatus(),
                            a.getAppliedAt());

                }).toList();

        return new PageResponse<>(
                rows,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    /** Administrator decision. */
    public void changeStatus(
            AuthUser admin,
            Long applicationId,
            ApplicationStatus target,
            String note) {

        Application a =
                applications.findById(applicationId)
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "Application not found."));

        if (!a.getStatus().adminTargets().contains(target)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "An application cannot move from "
                            + a.getStatus()
                            + " to "
                            + target
                            + ".");
        }

        transition(
                a,
                target,
                admin.id(),
                note == null || note.isBlank()
                        ? null
                        : note.trim());
    }

    // ---------------- helpers ----------------

    private void transition(
            Application a,
            ApplicationStatus to,
            Long actorId,
            String note) {

        ApplicationStatus from = a.getStatus();

        a.setStatus(to);
        applications.save(a);

        history.save(new ApplicationStatusHistory(
                a.getId(),
                from,
                to,
                actorId,
                clock.instant(),
                note));
    }

    private static Specification<Application> filter(
            Long driveId,
            ApplicationStatus status,
            String departmentName) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (driveId != null) {
                predicates.add(
                        cb.equal(root.get("driveId"), driveId));
            }

            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status));
            }

            if (departmentName != null) {

                Subquery<Long> profileQuery =
                        query.subquery(Long.class);

                Root<StudentProfile> profile =
                        profileQuery.from(StudentProfile.class);

                profileQuery
                        .select(profile.<Long>get("userId"))
                        .where(
                                cb.equal(
                                        profile.get("branch"),
                                        departmentName));

                predicates.add(
                        root.get("studentUserId")
                                .in(profileQuery));
            }

            return cb.and(
                    predicates.toArray(new Predicate[0]));
        };
    }

    private Map<Long, PlacementDrive> drivesById(
            List<Application> list) {

        Map<Long, PlacementDrive> map = new HashMap<>();

        if (!list.isEmpty()) {
            drives.findAllById(
                    list.stream()
                            .map(Application::getDriveId)
                            .collect(Collectors.toCollection(
                                    LinkedHashSet::new)))
                    .forEach(d -> map.put(d.getId(), d));
        }

        return map;
    }

    private Map<Long, String> companyNamesFor(
            java.util.Collection<PlacementDrive> drivesList) {

        Map<Long, String> names = new HashMap<>();

        if (!drivesList.isEmpty()) {
            companies.findAllById(
                    drivesList.stream()
                            .map(PlacementDrive::getCompanyId)
                            .collect(Collectors.toCollection(
                                    LinkedHashSet::new)))
                    .forEach(c -> names.put(
                            c.getId(),
                            c.getName()));
        }

        return names;
    }

    private String companyName(Long companyId) {
        return companies.findById(companyId)
                .map(c -> c.getName())
                .orElse(null);
    }

    private static ApplicationResponse toResponse(
            Application a,
            PlacementDrive d,
            String companyName) {

        return new ApplicationResponse(
                a.getId(),
                d.getId(),
                companyName,
                d.getTitle(),
                a.getStatus(),
                a.getAppliedAt(),
                a.getUpdatedAt(),
                d.getApplicationDeadline());
    }

    private static ApiException duplicate() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "You have already applied to this drive.");
    }
}