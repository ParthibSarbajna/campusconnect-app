package com.campusconnect.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Shared fixtures. All data is synthetic. */
final class TestAccounts {
    static final String PASSWORD = "Passw0rd-Strong1";
    static final Instant NOW = Instant.parse("2026-10-16T00:00:00Z");

    private TestAccounts() {}

    static UserAccount account(PasswordEncoder encoder, long id, String email, Role role, Long departmentId) {
        UserAccount u = UserAccount.create(email, "Test User " + id, encoder.encode(PASSWORD), role, departmentId, NOW);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    /** Places an already-authenticated principal in the request (used for authorization tests). */
    static RequestPostProcessor as(Role role, long id, Long departmentId) {
        AuthUser user = new AuthUser(id, "user" + id + "@example.edu", "User " + id, role, departmentId);
        return authentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority(role.authority()))));
    }
}
