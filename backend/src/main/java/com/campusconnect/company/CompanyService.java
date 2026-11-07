package com.campusconnect.company;

import com.campusconnect.common.ApiException;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companies;

    public CompanyService(CompanyRepository companies) {
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public List<CompanyDtos.CompanyResponse> list() {
        return companies.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(CompanyDtos.CompanyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyDtos.CompanyResponse get(Long id) {
        return CompanyDtos.CompanyResponse.from(findCompany(id));
    }

    public CompanyDtos.CompanyResponse create(CompanyDtos.CompanyRequest request) {
        String name = normalizeName(request.name());

        if (companies.existsByNameIgnoreCase(name)) {
            throw duplicateName();
        }

        Company company = new Company(
                name,
                normalizeOptional(request.website()),
                normalizeOptional(request.description()),
                normalizeOptional(request.industry()));

        try {
            return CompanyDtos.CompanyResponse.from(
                    companies.saveAndFlush(company));
        } catch (DataIntegrityViolationException e) {
            throw duplicateName();
        }
    }

    public CompanyDtos.CompanyResponse update(
            Long id, CompanyDtos.CompanyRequest request) {
        Company company = findCompany(id);
        String name = normalizeName(request.name());

        if (companies.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName();
        }

        company.updateDetails(
                name,
                normalizeOptional(request.website()),
                normalizeOptional(request.description()),
                normalizeOptional(request.industry()));

        try {
            return CompanyDtos.CompanyResponse.from(
                    companies.saveAndFlush(company));
        } catch (DataIntegrityViolationException e) {
            throw duplicateName();
        }
    }

    public CompanyDtos.CompanyResponse setActive(Long id, boolean active) {
        Company company = findCompany(id);
        company.setActive(active);
        return CompanyDtos.CompanyResponse.from(companies.save(company));
    }

    private Company findCompany(Long id) {
        return companies.findById(id)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Company not found."));
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ApiException duplicateName() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "A company with this name already exists.");
    }
}