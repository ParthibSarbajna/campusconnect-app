package com.campusconnect.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.common.ApiException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companies;

    @InjectMocks
    private CompanyService companyService;

    private CompanyDtos.CompanyRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CompanyDtos.CompanyRequest(
                "Example Company",
                "https://example.com",
                "Software company",
                "Technology");
    }

    @Test
    void listReturnsCompaniesSortedByName() {
        Company company = new Company(
                "Example Company",
                "https://example.com",
                null,
                "Technology");

        when(companies.findAll(any(Sort.class)))
                .thenReturn(List.of(company));

        List<CompanyDtos.CompanyResponse> result = companyService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Example Company");
        verify(companies).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Test
    void getReturnsCompanyWhenItExists() {
        Company company = new Company(
                "Example Company", null, null, null);

        when(companies.findById(1L)).thenReturn(Optional.of(company));

        CompanyDtos.CompanyResponse result = companyService.get(1L);

        assertThat(result.name()).isEqualTo("Example Company");
    }

    @Test
    void getThrowsNotFoundWhenCompanyDoesNotExist() {
        when(companies.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.get(99L))
                .isInstanceOf(ApiException.class)
                .satisfies(error ->
                        assertThat(((ApiException) error).getStatus())
                                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createNormalizesCompanyFieldsAndSavesCompany() {
        when(companies.existsByNameIgnoreCase("Example Company"))
                .thenReturn(false);

        when(companies.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDtos.CompanyResponse result =
                companyService.create(new CompanyDtos.CompanyRequest(
                        "  Example Company  ",
                        " https://example.com ",
                        "  Software company  ",
                        "  Technology  "));

        assertThat(result.name()).isEqualTo("Example Company");
        assertThat(result.website()).isEqualTo("https://example.com");
        assertThat(result.description()).isEqualTo("Software company");
        assertThat(result.industry()).isEqualTo("Technology");

        verify(companies).saveAndFlush(any(Company.class));
    }

    @Test
    void createRejectsDuplicateCompanyName() {
        when(companies.existsByNameIgnoreCase("Example Company"))
                .thenReturn(true);

        assertThatThrownBy(() -> companyService.create(validRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(error ->
                        assertThat(((ApiException) error).getStatus())
                                .isEqualTo(HttpStatus.CONFLICT));

        verify(companies, never()).saveAndFlush(any(Company.class));
    }

    @Test
    void createConvertsDatabaseIntegrityViolationToConflict() {
        when(companies.existsByNameIgnoreCase("Example Company"))
                .thenReturn(false);

        when(companies.saveAndFlush(any(Company.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate name"));

        assertThatThrownBy(() -> companyService.create(validRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(error ->
                        assertThat(((ApiException) error).getStatus())
                                .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateChangesCompanyDetails() {
        Company company = new Company(
                "Old Name", null, null, null);

        when(companies.findById(1L)).thenReturn(Optional.of(company));
        when(companies.existsByNameIgnoreCaseAndIdNot("Example Company", 1L))
                .thenReturn(false);
        when(companies.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDtos.CompanyResponse result =
                companyService.update(1L, validRequest);

        assertThat(result.name()).isEqualTo("Example Company");
        assertThat(result.website()).isEqualTo("https://example.com");
        assertThat(result.industry()).isEqualTo("Technology");
    }

    @Test
    void updateRejectsAnotherCompanyWithTheSameName() {
        Company company = new Company(
                "Old Name", null, null, null);

        when(companies.findById(1L)).thenReturn(Optional.of(company));
        when(companies.existsByNameIgnoreCaseAndIdNot("Example Company", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> companyService.update(1L, validRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(error ->
                        assertThat(((ApiException) error).getStatus())
                                .isEqualTo(HttpStatus.CONFLICT));

        verify(companies, never()).saveAndFlush(any(Company.class));
    }

    @Test
    void updateThrowsNotFoundWhenCompanyDoesNotExist() {
        when(companies.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.update(99L, validRequest))
                .isInstanceOf(ApiException.class)
                .satisfies(error ->
                        assertThat(((ApiException) error).getStatus())
                                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void setActiveChangesCompanyStatus() {
        Company company = new Company(
                "Example Company", null, null, null);

        when(companies.findById(1L)).thenReturn(Optional.of(company));
        when(companies.save(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDtos.CompanyResponse result =
                companyService.setActive(1L, false);

        assertThat(result.active()).isFalse();
        verify(companies).save(company);
    }

    @Test
    void setActiveThrowsNotFoundWhenCompanyDoesNotExist() {
        when(companies.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.setActive(99L, false))
                .isInstanceOf(ApiException.class)
                .satisfies(error ->
                        assertThat(((ApiException) error).getStatus())
                                .isEqualTo(HttpStatus.NOT_FOUND));

        verify(companies, never()).save(any(Company.class));
    }
}