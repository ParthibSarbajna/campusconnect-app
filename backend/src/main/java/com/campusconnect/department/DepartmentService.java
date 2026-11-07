package com.campusconnect.department;

import com.campusconnect.account.Dtos.DepartmentRequest;
import com.campusconnect.account.Dtos.DepartmentResponse;
import com.campusconnect.common.ApiException;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {
    private final DepartmentRepository departments;

    public DepartmentService(DepartmentRepository departments) {
        this.departments = departments;
    }

    public List<DepartmentResponse> list() {
        return departments.findAll(Sort.by("code")).stream().map(DepartmentResponse::from).toList();
    }

    public DepartmentResponse create(DepartmentRequest r) {
        String code = r.code().trim().toUpperCase(Locale.ROOT);
        if (departments.existsByCode(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "A department with this code already exists.");
        }
        try {
            return DepartmentResponse.from(departments.saveAndFlush(new Department(code, r.name().trim())));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "A department with this code already exists.");
        }
    }
}
