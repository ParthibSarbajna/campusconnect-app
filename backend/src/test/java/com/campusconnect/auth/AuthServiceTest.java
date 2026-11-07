package com.campusconnect.auth;

import static com.campusconnect.auth.TestAccounts.NOW;
import static com.campusconnect.auth.TestAccounts.PASSWORD;
import static com.campusconnect.auth.TestAccounts.account;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusconnect.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Credential checks and lockout, with a real BCrypt encoder (low cost for speed) and a mocked repository. */
class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRepository users = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AuthService service = new AuthService(users, encoder, clock, 3, 15);

    private UserAccount stored() {
        UserAccount u = account(encoder, 1L, "asha@example.edu", Role.STUDENT, null);
        when(users.findByEmail("asha@example.edu")).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void correctPasswordSucceedsAndEmailIsNormalized() {
        stored();
        assertThat(service.authenticate("  ASHA@Example.edu ", PASSWORD).getId()).isEqualTo(1L);
    }

    @Test
    void repeatedFailuresLockTheAccountEvenForTheCorrectPassword() {
        UserAccount u = stored();
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.authenticate("asha@example.edu", "Wrong-Passw0rd"))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
        assertThat(u.getLockedUntil()).isEqualTo(NOW.plusSeconds(15 * 60));
        assertThatThrownBy(() -> service.authenticate("asha@example.edu", PASSWORD)).isInstanceOf(ApiException.class);
    }

    @Test
    void lockExpiresAndSuccessfulLoginResetsCounters() {
        UserAccount u = stored();
        u.registerFailure(NOW.minusSeconds(3600), 1, java.time.Duration.ofMinutes(15)); // lock that ended 45 minutes ago
        assertThat(u.isLockedAt(NOW)).isFalse();
        service.authenticate("asha@example.edu", PASSWORD);
        assertThat(u.getLockedUntil()).isNull();
        assertThat(u.getFailedLoginAttempts()).isZero();
    }

    @Test
    void unknownEmailGetsTheSameGenericFailure() {
        when(users.findByEmail("ghost@example.edu")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.authenticate("ghost@example.edu", PASSWORD))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).isEqualTo("Invalid email or password.");
                });
    }

    @Test
    void passwordLongerThan72BytesIsRejectedAtRegistration() {
        String tooLong = "a1".repeat(40);
        assertThatThrownBy(() -> service.registerStudent(new com.campusconnect.account.Dtos.RegisterRequest("Asha Roy", "a@example.edu", tooLong)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
