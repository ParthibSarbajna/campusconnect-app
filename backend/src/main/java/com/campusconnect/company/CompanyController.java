package com.campusconnect.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/companies")
@PreAuthorize("hasRole('PLACEMENT_ADMIN')")
public class CompanyController {

    private final CompanyService companies;

    public CompanyController(CompanyService companies) {
        this.companies = companies;
    }

    @GetMapping
    public List<CompanyDtos.CompanyResponse> list() {
        return companies.list();
    }

    @GetMapping("/{id}")
    public CompanyDtos.CompanyResponse get(@PathVariable Long id) {
        return companies.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyDtos.CompanyResponse create(
            @Valid @RequestBody CompanyDtos.CompanyRequest request) {
        return companies.create(request);
    }

    @PutMapping("/{id}")
    public CompanyDtos.CompanyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CompanyDtos.CompanyRequest request) {
        return companies.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public CompanyDtos.CompanyResponse setActive(
            @PathVariable Long id,
            @Valid @RequestBody ActiveRequest request) {
        return companies.setActive(id, request.active());
    }

    public record ActiveRequest(@NotNull Boolean active) {}
}