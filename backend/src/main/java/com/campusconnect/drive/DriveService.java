
package com.campusconnect.drive;

import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.application.ApplicationRepository;
import com.campusconnect.common.ApiException;
import com.campusconnect.company.Company;
import com.campusconnect.company.CompanyRepository;
import com.campusconnect.drive.DriveDtos.DriveRequest;
import com.campusconnect.drive.DriveDtos.DriveResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrator-side drive management and lifecycle. */
@Service
@Transactional
public class DriveService {

    /** DRAFT -> OPEN -> CLOSED, and DRAFT/OPEN -> CANCELLED. CLOSED and CANCELLED are final. */
    static boolean canMove(DriveStatus from, DriveStatus to) {
        return switch (from) {
            case DRAFT -> EnumSet.of(DriveStatus.OPEN, DriveStatus.CANCELLED).contains(to);
            case OPEN -> EnumSet.of(DriveStatus.CLOSED, DriveStatus.CANCELLED).contains(to);
            case CLOSED, CANCELLED -> false;
        };
    }

    private final DriveRepository drives;
    private final CompanyRepository companies;
    private final ApplicationRepository applications;
    private final Clock clock;

    public DriveService(
            DriveRepository drives,
            CompanyRepository companies,
            ApplicationRepository applications,
            Clock clock) {
        this.drives = drives;
        this.companies = companies;
        this.applications = applications;
        this.clock = clock;
    }

    public DriveResponse create(DriveRequest r) {
        Company company = activeCompany(r.companyId());
        requireFuture(r.applicationDeadline(), HttpStatus.BAD_REQUEST);

        PlacementDrive d = new PlacementDrive();
        apply(d, r);

        return DriveResponse.from(drives.saveAndFlush(d), company.getName());
    }

    public DriveResponse update(Long id, DriveRequest r) {
        PlacementDrive d = find(id);

        if (d.getStatus() == DriveStatus.DRAFT) {
            Company company = activeCompany(r.companyId());
            requireFuture(r.applicationDeadline(), HttpStatus.BAD_REQUEST);
            apply(d, r);

            return DriveResponse.from(drives.saveAndFlush(d), company.getName());
        }

        if (d.getStatus() == DriveStatus.OPEN) {
            // Eligibility and identity are frozen once students can see and apply to the drive.
            if (!d.getCompanyId().equals(r.companyId())
                    || !d.getTitle().equals(r.title().trim())
                    || !d.getGraduationYear().equals(r.graduationYear().shortValue())
                    || !sameNumber(d.getMinCgpa(), r.minCgpa())
                    || !sameNumber(d.getMaxActiveBacklogs(), r.maxActiveBacklogs())
                    || !sameBranches(d.getBranches(), normalizeBranches(r.branches()))) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Company, title, batch and eligibility criteria cannot change after a drive is published.");
            }

            requireFuture(r.applicationDeadline(), HttpStatus.BAD_REQUEST);
            apply(d, r);

            return DriveResponse.from(
                    drives.saveAndFlush(d),
                    companyName(d.getCompanyId()));
        }

        throw new ApiException(
                HttpStatus.CONFLICT,
                "A " + d.getStatus() + " drive cannot be edited.");
    }

    public DriveResponse changeStatus(Long id, DriveStatus target) {
        PlacementDrive d = find(id);

        if (!canMove(d.getStatus(), target)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "A drive cannot move from " + d.getStatus() + " to " + target + ".");
        }

        if (target == DriveStatus.OPEN) {
            Company c = companies.findById(d.getCompanyId()).orElse(null);

            if (c == null || !c.isActive()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "The company is inactive. Activate it before publishing.");
            }

            requireFuture(d.getApplicationDeadline(), HttpStatus.CONFLICT);
        }

        d.setStatus(target);

        return DriveResponse.from(
                drives.save(d),
                companyName(d.getCompanyId()));
    }

    /**
     * Permanently removes a cancelled drive only if no applications reference it.
     * Application history must be preserved.
     */
    public void deleteCancelled(Long id) {
        PlacementDrive d = find(id);

        if (d.getStatus() != DriveStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Only cancelled drives can be removed.");
        }

        if (applications.existsByDriveId(id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "This drive has applications and cannot be removed. Keep it cancelled to preserve placement history.");
        }

        drives.delete(d);
        drives.flush();
    }

    @Transactional(readOnly = true)
    public DriveResponse get(Long id) {
        PlacementDrive d = find(id);
        return DriveResponse.from(d, companyName(d.getCompanyId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<DriveResponse> list(
            DriveStatus status,
            Long companyId,
            Integer graduationYear,
            int page,
            int size) {

        Specification<PlacementDrive> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();

            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }

            if (companyId != null) {
                ps.add(cb.equal(root.get("companyId"), companyId));
            }

            if (graduationYear != null) {
                ps.add(cb.equal(root.get("graduationYear"), graduationYear.shortValue()));
            }

            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<PlacementDrive> result = drives.findAll(
                spec,
                pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<Long, String> names = companyNames(
                result.getContent().stream()
                        .map(PlacementDrive::getCompanyId)
                        .toList());

        return new PageResponse<>(
                result.getContent().stream()
                        .map(d -> DriveResponse.from(d, names.get(d.getCompanyId())))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    // ------------- helpers shared with the student-facing service -------------

    public static PageRequest pageable(int page, int size, Sort sort) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                sort);
    }

    public Map<Long, String> companyNames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();

        if (!ids.isEmpty()) {
            companies.findAllById(new LinkedHashSet<>(ids))
                    .forEach(c -> names.put(c.getId(), c.getName()));
        }

        return names;
    }

    private String companyName(Long id) {
        return companyNames(List.of(id)).get(id);
    }

    private PlacementDrive find(Long id) {
        return drives.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Drive not found."));
    }

    private Company activeCompany(Long companyId) {
        return companies.findById(companyId)
                .filter(Company::isActive)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Choose an existing, active company."));
    }

    private void requireFuture(Instant deadline, HttpStatus status) {
        if (!deadline.isAfter(clock.instant())) {
            throw new ApiException(
                    status,
                    "The application deadline must be in the future.");
        }
    }

    private void apply(PlacementDrive d, DriveRequest r) {
        d.applyDetails(
                r.companyId(),
                r.title().trim(),
                blankToNull(r.description()),
                blankToNull(r.location()),
                r.ctcLpa(),
                r.graduationYear().shortValue(),
                r.minCgpa(),
                r.maxActiveBacklogs() == null
                        ? null
                        : r.maxActiveBacklogs().shortValue(),
                normalizeBranches(r.branches()),
                r.applicationDeadline());
    }

    static Set<String> normalizeBranches(List<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        Set<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        if (raw != null) {
            for (String b : raw) {
                String t = b == null ? "" : b.trim();

                if (!t.isEmpty() && seen.add(t)) {
                    out.add(t);
                }
            }
        }

        return out;
    }

    private static boolean sameBranches(Set<String> a, Set<String> b) {
        Set<String> x = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> y = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        x.addAll(a);
        y.addAll(b);

        return x.equals(y);
    }

    private static boolean sameNumber(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }

        return new BigDecimal(a.toString())
                .compareTo(new BigDecimal(b.toString())) == 0;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
