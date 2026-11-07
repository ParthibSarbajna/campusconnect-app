package com.campusconnect.auth;

import static com.campusconnect.auth.TestAccounts.as;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusconnect.account.Dtos.PageResponse;
import com.campusconnect.application.ApplicationManagementController;
import com.campusconnect.application.ApplicationService;
import com.campusconnect.application.ApplicationStatus;
import com.campusconnect.application.StudentDriveService;
import com.campusconnect.application.StudentPlacementController;
import com.campusconnect.company.CompanyController;
import com.campusconnect.company.CompanyService;
import com.campusconnect.config.AppConfig;
import com.campusconnect.config.CorsConfig;
import com.campusconnect.config.PasswordConfig;
import com.campusconnect.config.SecurityConfig;
import com.campusconnect.drive.AdminDriveController;
import com.campusconnect.drive.DriveService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Role boundaries and request validation for drives, applications and companies (services are mocked). */
@WebMvcTest(controllers = {AdminDriveController.class, StudentPlacementController.class,
        ApplicationManagementController.class, CompanyController.class})
@Import({SecurityConfig.class, CorsConfig.class, PasswordConfig.class, AppConfig.class})
class PlacementAuthorizationTest {

    @Autowired MockMvc mvc;
    @MockBean UserRepository users;
    @MockBean DriveService driveService;
    @MockBean StudentDriveService studentDriveService;
    @MockBean ApplicationService applicationService;
    @MockBean CompanyService companyService;

    private static final String DRIVE_JSON = "{\"companyId\":1,\"title\":\"Engineer\",\"graduationYear\":2027,"
            + "\"applicationDeadline\":\"2030-01-01T00:00:00Z\"}";

    // ---- admin drives ----

    @Test
    void anonymousIs401AndStudentAndSpocAre403OnAdminDriveEndpoints() throws Exception {
        mvc.perform(get("/api/v1/admin/drives")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/drives").with(as(Role.STUDENT, 1L, null))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/drives").with(as(Role.DEPARTMENT_SPOC, 2L, 1L))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/drives").with(as(Role.STUDENT, 1L, null)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(DRIVE_JSON)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/drives/5/status").with(as(Role.DEPARTMENT_SPOC, 2L, 1L)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}")).andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAndCreateDrivesAndInvalidInputIs400() throws Exception {
        when(driveService.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 25, 0));
        mvc.perform(get("/api/v1/admin/drives").with(as(Role.PLACEMENT_ADMIN, 9L, null))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/drives").with(as(Role.PLACEMENT_ADMIN, 9L, null)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(DRIVE_JSON)).andExpect(status().isCreated());
        // missing title, impossible CGPA, graduation year out of range
        mvc.perform(post("/api/v1/admin/drives").with(as(Role.PLACEMENT_ADMIN, 9L, null)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":1,\"graduationYear\":1900,\"minCgpa\":11,\"applicationDeadline\":\"2030-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void driveStateChangesRequireCsrf() throws Exception {
        mvc.perform(post("/api/v1/admin/drives/5/status").with(as(Role.PLACEMENT_ADMIN, 9L, null))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}")).andExpect(status().isForbidden());
    }

    // ---- student endpoints ----

    @Test
    void studentEndpointsAreForStudentsOnly() throws Exception {
        mvc.perform(get("/api/v1/drives")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/drives").with(as(Role.PLACEMENT_ADMIN, 9L, null))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/drives").with(as(Role.DEPARTMENT_SPOC, 2L, 1L))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/drives/5/applications").with(as(Role.PLACEMENT_ADMIN, 9L, null)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/applications/me").with(as(Role.DEPARTMENT_SPOC, 2L, 1L))).andExpect(status().isForbidden());
    }

    @Test
    void studentCanBrowseApplyAndWithdrawThroughTheirOwnIdentityOnly() throws Exception {
        when(studentDriveService.list(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0));
        mvc.perform(get("/api/v1/drives").with(as(Role.STUDENT, 10L, 1L))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/drives/5/applications").with(as(Role.STUDENT, 10L, 1L)).with(csrf())).andExpect(status().isCreated());
        // the service receives the signed-in student's own id, never an id from the request
        verify(applicationService).apply(org.mockito.ArgumentMatchers.argThat(u -> u.id() == 10L), eq(5L));
        mvc.perform(post("/api/v1/applications/7/withdraw").with(as(Role.STUDENT, 10L, 1L)).with(csrf())).andExpect(status().isOk());
        verify(applicationService).withdraw(org.mockito.ArgumentMatchers.argThat(u -> u.id() == 10L), eq(7L));
    }

    @Test
    void applyingWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/v1/drives/5/applications").with(as(Role.STUDENT, 10L, 1L))).andExpect(status().isForbidden());
    }

    // ---- application management ----

    @Test
    void applicationListIsForSpocAndAdminOnly() throws Exception {
        when(applicationService.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 25, 0));
        mvc.perform(get("/api/v1/applications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/applications").with(as(Role.STUDENT, 10L, 1L))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/applications").with(as(Role.DEPARTMENT_SPOC, 2L, 1L))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/applications").with(as(Role.PLACEMENT_ADMIN, 9L, null))).andExpect(status().isOk());
    }

    @Test
    void onlyAdminsCanChangeApplicationStatus() throws Exception {
        String body = "{\"status\":\"SHORTLISTED\"}";
        mvc.perform(post("/api/v1/admin/applications/7/status").with(as(Role.DEPARTMENT_SPOC, 2L, 1L)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/applications/7/status").with(as(Role.STUDENT, 10L, 1L)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/applications/7/status").with(as(Role.PLACEMENT_ADMIN, 9L, null)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        verify(applicationService).changeStatus(any(), eq(7L), eq(ApplicationStatus.SHORTLISTED), any());
    }

    // ---- companies ----

    @Test
    void companyActivationRequiresAnExplicitFlag() throws Exception {
        mvc.perform(patch("/api/v1/admin/companies/3/active").with(as(Role.PLACEMENT_ADMIN, 9L, null)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/companies/3/active").with(as(Role.STUDENT, 10L, null)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isForbidden());
    }
}
