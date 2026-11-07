
package com.campusconnect.auth;

import static com.campusconnect.auth.TestAccounts.account;
import static com.campusconnect.auth.TestAccounts.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusconnect.account.AccountController;
import com.campusconnect.account.AccountService;
import com.campusconnect.account.AdminController;
import com.campusconnect.config.AppConfig;
import com.campusconnect.config.CorsConfig;
import com.campusconnect.config.PasswordConfig;
import com.campusconnect.config.SecurityConfig;
import com.campusconnect.company.CompanyController;
import com.campusconnect.company.CompanyDtos;
import com.campusconnect.company.CompanyService;
import com.campusconnect.department.DepartmentRepository;
import com.campusconnect.department.DepartmentService;
import com.campusconnect.student.StudentProfileController;
import com.campusconnect.student.StudentProfileDtos.StudentProfileResponse;
import com.campusconnect.student.StudentProfileDtos.UpdateStudentProfileRequest;
import com.campusconnect.student.StudentProfileService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Role, ownership and department-scope enforcement on the server. */
@WebMvcTest(controllers = {
        AuthController.class,
        AccountController.class,
        AdminController.class,
        StudentProfileController.class,
        CompanyController.class
})
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        PasswordConfig.class,
        AppConfig.class,
        AuthService.class,
        AccountService.class,
        DepartmentService.class,
        SessionLoginHelper.class
})
class AuthorizationTest {

    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired PasswordEncoder encoder;

    @MockBean UserRepository users;
    @MockBean DepartmentRepository departments;
    @MockBean StudentProfileService studentProfiles;
    @MockBean CompanyService companyService;

    private void emptyUserPage() {
        when(users.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));
        when(users.findByDepartmentId(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
    }

    // ---- authentication ----

    @Test
    void unauthenticatedRequestsToProtectedEndpointsGet401() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/departments/1/users"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/students/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/students/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- admin area ----

    @Test
    void studentAndSpocCannotUseAdminEndpoints() throws Exception {
        mvc.perform(get("/api/v1/admin/users")
                        .with(as(Role.STUDENT, 1L, null)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/admin/users")
                        .with(as(Role.DEPARTMENT_SPOC, 2L, 1L)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/admin/departments")
                        .with(as(Role.DEPARTMENT_SPOC, 2L, 1L))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CSE\",\"name\":\"Computer Science\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/admin/users/1")
                        .with(as(Role.STUDENT, 1L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"PLACEMENT_ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        emptyUserPage();

        mvc.perform(get("/api/v1/admin/users")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void adminCanPromoteAUserToSpocOnlyWithADepartment() throws Exception {
        UserAccount target = account(
                encoder, 5L, "kabir@example.edu", Role.STUDENT, null);

        when(users.findById(5L)).thenReturn(Optional.of(target));
        when(users.save(any(UserAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(departments.existsById(3L)).thenReturn(true);

        mvc.perform(patch("/api/v1/admin/users/5")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DEPARTMENT_SPOC\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/v1/admin/users/5")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DEPARTMENT_SPOC\",\"departmentId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DEPARTMENT_SPOC"))
                .andExpect(jsonPath("$.departmentId").value(3));
    }

    @Test
    void adminCannotDemoteOrDisableTheirOwnAccount() throws Exception {
        UserAccount self = account(
                encoder, 9L, "admin@example.edu", Role.PLACEMENT_ADMIN, null);

        when(users.findById(9L)).thenReturn(Optional.of(self));

        mvc.perform(patch("/api/v1/admin/users/9")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\"}"))
                .andExpect(status().isConflict());

        mvc.perform(patch("/api/v1/admin/users/9")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanCreateASpocAccount() throws Exception {
        when(departments.existsById(3L)).thenReturn(true);
        when(users.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/v1/admin/users")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "fullName", "Dept Coordinator",
                                "email", "spoc@example.edu",
                                "password", TestAccounts.PASSWORD,
                                "role", "DEPARTMENT_SPOC",
                                "departmentId", 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DEPARTMENT_SPOC"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // ---- ownership ----

    @Test
    void studentCanReadOwnRecordButNotAnotherStudents() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(
                account(encoder, 1L, "a@example.edu", Role.STUDENT, null)));
        when(users.findById(2L)).thenReturn(Optional.of(
                account(encoder, 2L, "b@example.edu", Role.STUDENT, null)));

        mvc.perform(get("/api/v1/users/1")
                        .with(as(Role.STUDENT, 1L, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@example.edu"));

        mvc.perform(get("/api/v1/users/2")
                        .with(as(Role.STUDENT, 1L, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyUser() throws Exception {
        when(users.findById(2L)).thenReturn(Optional.of(
                account(encoder, 2L, "b@example.edu", Role.STUDENT, null)));

        mvc.perform(get("/api/v1/users/2")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null)))
                .andExpect(status().isOk());
    }

    // ---- student profile authorization ----

    private StudentProfileResponse sampleProfile() {
        return new StudentProfileResponse(
                101L,
                1L,
                "CC001",
                "9876543210",
                null,
                "B.Tech",
                "CSE",
                (short) 2027,
                new BigDecimal("8.50"),
                (short) 0,
                null
        );
    }

    @Test
    void studentCanGetTheirOwnProfile() throws Exception {
        when(studentProfiles.getMyProfile(any(AuthUser.class)))
                .thenReturn(sampleProfile());

        mvc.perform(get("/api/v1/students/me")
                        .with(as(Role.STUDENT, 1L, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.rollNumber").value("CC001"))
                .andExpect(jsonPath("$.cgpa").value(8.50));
    }

    @Test
    void studentCanUpdateTheirOwnProfileWithCsrf() throws Exception {
        when(studentProfiles.updateMyProfile(
                any(AuthUser.class),
                any(UpdateStudentProfileRequest.class)))
                .thenReturn(sampleProfile());

        mvc.perform(put("/api/v1/students/me")
                        .with(as(Role.STUDENT, 1L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rollNumber": "CC001",
                                  "phone": "9876543210",
                                  "degree": "B.Tech",
                                  "branch": "CSE",
                                  "graduationYear": 2027,
                                  "cgpa": 8.50,
                                  "activeBacklogs": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.rollNumber").value("CC001"));

        verify(studentProfiles).updateMyProfile(
                any(AuthUser.class),
                any(UpdateStudentProfileRequest.class));
    }

    @Test
    void spocAndAdminCannotAccessStudentProfileEndpoints() throws Exception {
        mvc.perform(get("/api/v1/students/me")
                        .with(as(Role.DEPARTMENT_SPOC, 2L, 1L)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/students/me")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/students/me")
                        .with(as(Role.DEPARTMENT_SPOC, 2L, 1L))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/students/me")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(studentProfiles, never())
                .getMyProfile(any(AuthUser.class));
        verify(studentProfiles, never())
                .updateMyProfile(
                        any(AuthUser.class),
                        any(UpdateStudentProfileRequest.class));
    }

    @Test
    void updatingStudentProfileWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(put("/api/v1/students/me")
                        .with(as(Role.STUDENT, 1L, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(studentProfiles, never())
                .updateMyProfile(
                        any(AuthUser.class),
                        any(UpdateStudentProfileRequest.class));
    }

    // ---- department boundary ----

    @Test
    void spocSeesOnlyTheirOwnDepartment() throws Exception {
        emptyUserPage();

        mvc.perform(get("/api/v1/departments/1/users")
                        .with(as(Role.DEPARTMENT_SPOC, 2L, 1L)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/departments/2/users")
                        .with(as(Role.DEPARTMENT_SPOC, 2L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotListDepartmentUsers() throws Exception {
        mvc.perform(get("/api/v1/departments/1/users")
                        .with(as(Role.STUDENT, 1L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAnyExistingDepartment() throws Exception {
        emptyUserPage();
        when(departments.existsById(2L)).thenReturn(true);

        mvc.perform(get("/api/v1/departments/2/users")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/departments/77/users")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null)))
                .andExpect(status().isNotFound());
    }

    // ---- department assignment: PATCH semantics ----

    private UserAccount storedAccount(
            long id, Role role, Long departmentId) {
        UserAccount u = account(
                encoder, id, "user" + id + "@example.edu", role, departmentId);

        if (departmentId != null) {
            when(departments.existsById(departmentId)).thenReturn(true);
        }

        when(users.findById(id)).thenReturn(Optional.of(u));
        when(users.save(any(UserAccount.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        return u;
    }

    private org.springframework.test.web.servlet.ResultActions adminPatch(
            long id, String body) throws Exception {
        return mvc.perform(patch("/api/v1/admin/users/" + id)
                .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void explicitNullDepartmentClearsTheDepartment() throws Exception {
        UserAccount target = storedAccount(5L, Role.STUDENT, 1L);

        adminPatch(5L,
                "{\"role\":\"STUDENT\",\"departmentId\":null,\"enabled\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").isEmpty());

        assertThat(target.getDepartmentId()).isNull();
    }

    @Test
    void omittedDepartmentKeepsTheCurrentDepartment() throws Exception {
        UserAccount target = storedAccount(5L, Role.STUDENT, 1L);

        adminPatch(5L, "{\"enabled\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(1));

        assertThat(target.getDepartmentId()).isEqualTo(1L);
    }

    @Test
    void departmentCanBeChangedToAnotherExistingDepartment() throws Exception {
        UserAccount target = storedAccount(5L, Role.STUDENT, 1L);
        when(departments.existsById(2L)).thenReturn(true);

        adminPatch(5L, "{\"departmentId\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(2));

        assertThat(target.getDepartmentId()).isEqualTo(2L);
    }

    @Test
    void unknownDepartmentIsRejectedAndNothingIsSaved() throws Exception {
        UserAccount target = storedAccount(5L, Role.STUDENT, 1L);
        when(departments.existsById(77L)).thenReturn(false);

        adminPatch(5L, "{\"departmentId\":77}")
                .andExpect(status().isBadRequest());

        assertThat(target.getDepartmentId()).isEqualTo(1L);
        verify(users, never()).save(any(UserAccount.class));
    }

    @Test
    void clearingASpocsDepartmentIsRejectedAndTheAccountIsUnchanged()
            throws Exception {
        UserAccount spoc = storedAccount(6L, Role.DEPARTMENT_SPOC, 3L);

        adminPatch(6L, "{\"departmentId\":null}")
                .andExpect(status().isBadRequest());

        assertThat(spoc.getDepartmentId()).isEqualTo(3L);
        assertThat(spoc.getRole()).isEqualTo(Role.DEPARTMENT_SPOC);
        verify(users, never()).save(any(UserAccount.class));
    }

    // ---- company management authorization ----

    @Test
    void unauthenticatedCompanyRequestsGet401() throws Exception {
        mvc.perform(get("/api/v1/admin/companies"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/admin/companies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Example Company\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentsAndSpocsCannotManageCompanies() throws Exception {
        mvc.perform(get("/api/v1/admin/companies")
                        .with(as(Role.STUDENT, 1L, null)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/admin/companies")
                        .with(as(Role.DEPARTMENT_SPOC, 2L, 1L))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Example Company\"}"))
                .andExpect(status().isForbidden());

        verify(companyService, never()).list();
        verify(companyService, never()).create(any(CompanyDtos.CompanyRequest.class));
    }

    @Test
    void placementAdminCanListCompanies() throws Exception {
        when(companyService.list()).thenReturn(List.of());

        mvc.perform(get("/api/v1/admin/companies")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(companyService).list();
    }

    @Test
    void placementAdminCanCreateCompanyWithCsrf() throws Exception {
        when(companyService.create(any(CompanyDtos.CompanyRequest.class)))
                .thenReturn(new CompanyDtos.CompanyResponse(
                        1L,
                        "Example Company",
                        "https://example.com",
                        null,
                        "Technology",
                        true,
                        null,
                        null));

        mvc.perform(post("/api/v1/admin/companies")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Example Company",
                                  "website": "https://example.com",
                                  "industry": "Technology"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Example Company"))
                .andExpect(jsonPath("$.active").value(true));

        verify(companyService).create(any(CompanyDtos.CompanyRequest.class));
    }

    @Test
    void companyCreationWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/v1/admin/companies")
                        .with(as(Role.PLACEMENT_ADMIN, 9L, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Example Company\"}"))
                .andExpect(status().isForbidden());

        verify(companyService, never()).create(any(CompanyDtos.CompanyRequest.class));
    }

}
