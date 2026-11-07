
package com.campusconnect.auth;

import static com.campusconnect.auth.TestAccounts.PASSWORD;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusconnect.account.AccountController;
import com.campusconnect.account.AccountService;
import com.campusconnect.account.AdminController;
import com.campusconnect.config.AppConfig;
import com.campusconnect.config.CorsConfig;
import com.campusconnect.config.PasswordConfig;
import com.campusconnect.config.SecurityConfig;
import com.campusconnect.department.DepartmentRepository;
import com.campusconnect.department.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Registration, login, session, logout, CSRF and profile behavior (repositories are mocked, no database needed). */
@WebMvcTest(controllers = {AuthController.class, AccountController.class, AdminController.class})
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
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;

    @MockBean UserRepository users;
    @MockBean DepartmentRepository departments;

    private String body(Map<String, ?> m) throws Exception {
        return json.writeValueAsString(m);
    }

    private void stubSaveAssignsId() {
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 42L);
            return a;
        });
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult r = mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) r.getRequest().getSession(false);
    }

    // ---- registration ----

    @Test
    void registrationCreatesStudentWithBcryptHashAndNeverReturnsIt() throws Exception {
        stubSaveAssignsId();

        mvc.perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "fullName", "Asha Roy",
                        "email", "  Asha.Roy@Example.EDU ",
                        "password", PASSWORD))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.email").value("asha.roy@example.edu"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<UserAccount> saved =
            ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());

        assertThat(saved.getValue().getPasswordHash())
            .startsWith("$2")
            .isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(
            PASSWORD, saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void anonymousUserCannotChooseRoleOrDepartmentAtRegistration()
            throws Exception {
        stubSaveAssignsId();

        mvc.perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "fullName", "Mallory",
                        "email", "m@example.edu",
                        "password", PASSWORD,
                        "role", "PLACEMENT_ADMIN",
                        "departmentId", 1))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.departmentId").doesNotExist());

        ArgumentCaptor<UserAccount> saved =
            ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());

        assertThat(saved.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getValue().getDepartmentId()).isNull();
    }

    @Test
    void duplicateEmailIsRejectedWith409() throws Exception {
        when(users.existsByEmail("asha@example.edu")).thenReturn(true);

        mvc.perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "fullName", "Asha Roy",
                        "email", "ASHA@example.edu",
                        "password", PASSWORD))))
            .andExpect(status().isConflict());

        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void invalidRegistrationInputIsRejectedWith400() throws Exception {
        for (Map<String, String> bad : java.util.List.of(
                Map.of("fullName", "A", "email", "a@example.edu",
                    "password", PASSWORD),
                Map.of("fullName", "Asha Roy", "email", "not-an-email",
                    "password", PASSWORD),
                Map.of("fullName", "Asha Roy", "email", "a@example.edu",
                    "password", "short1"),
                Map.of("fullName", "Asha Roy", "email", "a@example.edu",
                    "password", "onlyletterslongenough"))) {

            mvc.perform(
                    post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(bad)))
                .andExpect(status().isBadRequest());
        }

        verify(users, never()).saveAndFlush(any());
    }

    // ---- department reassignment diagnostic ----

        @Test
    void existingSpocSessionUsesUpdatedDepartmentAfterAssignmentChanges()
            throws Exception {
        UserAccount spoc = account(
                encoder,
                3L,
                "spoc@example.edu",
                Role.DEPARTMENT_SPOC,
                1L);

        when(users.findByEmail("spoc@example.edu"))
                .thenReturn(Optional.of(spoc));
        when(users.findById(3L))
                .thenReturn(Optional.of(spoc));

        MockHttpSession session = login("spoc@example.edu", PASSWORD);

        // Simulate an administrator changing the persisted department.
        spoc.setDepartmentId(2L);

        // The profile endpoint must return the updated department.
        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(2));

        // Return a valid paginated result for the newly assigned department.
        when(users.findByDepartmentId(
                org.mockito.ArgumentMatchers.eq(2L),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 50),
                        0));

        // The refreshed session should authorize access to department 2.
        mvc.perform(get("/api/v1/departments/2/users").session(session))
                .andExpect(status().isOk());
    }
    // ---- login / me / logout ----

    @Test
    void loginSucceedsAndSessionGivesAccessToMe() throws Exception {
        UserAccount u = account(
            encoder, 1L, "asha@example.edu", Role.STUDENT, null);

        when(users.findByEmail("asha@example.edu"))
            .thenReturn(Optional.of(u));
        when(users.findById(1L))
            .thenReturn(Optional.of(u));

        MockHttpSession session = login("Asha@Example.edu", PASSWORD);

        mvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("asha@example.edu"))
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameGenericError()
            throws Exception {
        when(users.findByEmail("asha@example.edu"))
            .thenReturn(Optional.of(account(
                encoder, 1L, "asha@example.edu", Role.STUDENT, null)));
        when(users.findByEmail("ghost@example.edu"))
            .thenReturn(Optional.empty());

        for (String email :
                new String[] {"asha@example.edu", "ghost@example.edu"}) {
            mvc.perform(
                    post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                            "email", email,
                            "password", "Wrong-Passw0rd"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail")
                    .value("Invalid email or password."));
        }
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        UserAccount u = account(
            encoder, 1L, "asha@example.edu", Role.STUDENT, null);
        u.setEnabled(false);

        when(users.findByEmail("asha@example.edu"))
            .thenReturn(Optional.of(u));

        mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "email", "asha@example.edu",
                        "password", PASSWORD))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meEndsTheSessionWhenTheAccountWasDisabledAfterLogin()
            throws Exception {
        UserAccount u = account(
            encoder, 1L, "asha@example.edu", Role.STUDENT, null);

        when(users.findByEmail("asha@example.edu"))
            .thenReturn(Optional.of(u));
        when(users.findById(1L))
            .thenReturn(Optional.of(u));

        MockHttpSession session = login("asha@example.edu", PASSWORD);
        u.setEnabled(false);

        mvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        UserAccount u = account(
            encoder, 1L, "asha@example.edu", Role.STUDENT, null);

        when(users.findByEmail("asha@example.edu"))
            .thenReturn(Optional.of(u));
        when(users.findById(1L))
            .thenReturn(Optional.of(u));

        MockHttpSession session = login("asha@example.edu", PASSWORD);

        mvc.perform(
                post("/api/v1/auth/logout")
                    .with(csrf())
                    .session(session))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    // ---- CSRF ----

    @Test
    void stateChangingRequestsWithoutCsrfTokenAreRejected()
            throws Exception {
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "email", "a@example.edu",
                        "password", PASSWORD))))
            .andExpect(status().isForbidden());

        mvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "fullName", "Asha Roy",
                        "email", "a@example.edu",
                        "password", PASSWORD))))
            .andExpect(status().isForbidden());
    }

    @Test
    void csrfEndpointIsPublicAndReturnsHeaderNameAndToken()
            throws Exception {
        mvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // ---- profile / password ----

    @Test
    void profileUpdateChangesOnlyTheNameNeverTheRole() throws Exception {
        UserAccount u = account(
            encoder, 7L, "asha@example.edu", Role.STUDENT, null);

        when(users.findById(7L)).thenReturn(Optional.of(u));
        when(users.save(any(UserAccount.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(
                patch("/api/v1/users/me")
                    .with(as(Role.STUDENT, 7L, null))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "fullName", "New Name",
                        "role", "PLACEMENT_ADMIN",
                        "departmentId", 3,
                        "enabled", true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("New Name"))
            .andExpect(jsonPath("$.role").value("STUDENT"));

        assertThat(u.getRole()).isEqualTo(Role.STUDENT);
        assertThat(u.getDepartmentId()).isNull();
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() throws Exception {
        UserAccount u = account(
            encoder, 7L, "asha@example.edu", Role.STUDENT, null);

        when(users.findById(7L)).thenReturn(Optional.of(u));

        mvc.perform(
                post("/api/v1/users/me/password")
                    .with(as(Role.STUDENT, 7L, null))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "currentPassword", "Wrong-Passw0rd",
                        "newPassword", "Another-Passw0rd2"))))
            .andExpect(status().isBadRequest());

        mvc.perform(
                post("/api/v1/users/me/password")
                    .with(as(Role.STUDENT, 7L, null))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of(
                        "currentPassword", PASSWORD,
                        "newPassword", "Another-Passw0rd2"))))
            .andExpect(status().isNoContent());

        assertThat(encoder.matches(
            "Another-Passw0rd2", u.getPasswordHash())).isTrue();
    }
}
