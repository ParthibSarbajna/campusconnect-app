package com.campusconnect.auth;

import com.campusconnect.account.Dtos.RegisterRequest;
import com.campusconnect.common.ApiException;
import com.campusconnect.common.PasswordPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Registration and credential checks. Deliberately NOT @Transactional: failed-login counters must be
 * saved even though the method then throws.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final int maxFailedLogins;
    private final Duration lockout;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, Clock clock,
                       @Value("${app.security.max-failed-logins:5}") int maxFailedLogins,
                       @Value("${app.security.lockout-minutes:15}") long lockoutMinutes) {
        this.users = users;
        this.encoder = encoder;
        this.clock = clock;
        this.maxFailedLogins = maxFailedLogins;
        this.lockout = Duration.ofMinutes(lockoutMinutes);
        // Used to spend similar time when the email is unknown, so timing does not reveal which emails exist.
        this.dummyHash = encoder.encode("unused-password-for-timing-0");
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Public registration can only ever create a STUDENT. */
    public UserAccount registerStudent(RegisterRequest r) {
        PasswordPolicy.validate(r.password());
        String email = normalizeEmail(r.email());
        if (users.existsByEmail(email)) {
            throw emailTaken();
        }
        UserAccount account = UserAccount.create(
                email, r.fullName().trim(), encoder.encode(r.password()), Role.STUDENT, null, clock.instant());
        try {
            return users.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            throw emailTaken(); // lost a race with another registration of the same email
        }
    }

    /** Returns the account on success; otherwise throws the same generic 401 for every failure reason. */
    public UserAccount authenticate(String rawEmail, String password) {
        Optional<UserAccount> found = users.findByEmail(normalizeEmail(rawEmail));
        if (found.isEmpty()) {
            encoder.matches(password, dummyHash);
            throw badCredentials();
        }
        UserAccount user = found.get();
        Instant now = clock.instant();
        if (user.isLockedAt(now)) {
            throw badCredentials();
        }
        if (!encoder.matches(password, user.getPasswordHash())) {
            user.registerFailure(now, maxFailedLogins, lockout);
            users.save(user);
            throw badCredentials();
        }
        if (!user.isEnabled()) {
            throw badCredentials(); // checked after the password so a disabled state is not revealed to guessers
        }
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.clearFailures();
            users.save(user);
        }
        return user;
    }

    private static ApiException badCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
    }
}
