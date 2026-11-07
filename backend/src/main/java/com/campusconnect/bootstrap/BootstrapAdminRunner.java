package com.campusconnect.bootstrap;

import com.campusconnect.auth.AuthService;
import com.campusconnect.auth.Role;
import com.campusconnect.auth.UserAccount;
import com.campusconnect.auth.UserRepository;
import com.campusconnect.common.ApiException;
import com.campusconnect.common.PasswordPolicy;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the FIRST Placement Admin from BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD, only when no admin exists.
 * It never logs the password. Remove the password from .env after the first successful start.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String email;
    private final String password;
    private final String name;

    public BootstrapAdminRunner(UserRepository users, PasswordEncoder encoder, Clock clock,
                                @Value("${app.bootstrap-admin.email:}") String email,
                                @Value("${app.bootstrap-admin.password:}") String password,
                                @Value("${app.bootstrap-admin.name:Placement Administrator}") String name) {
        this.users = users;
        this.encoder = encoder;
        this.clock = clock;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("Bootstrap admin skipped: set BOTH BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD.");
            return;
        }
        if (users.existsByRole(Role.PLACEMENT_ADMIN)) {
            log.info("Bootstrap admin skipped: a Placement Admin already exists. Remove BOOTSTRAP_ADMIN_PASSWORD from .env.");
            return;
        }
        try {
            PasswordPolicy.validate(password);
        } catch (ApiException e) {
            log.error("Bootstrap admin NOT created: {}", e.getMessage());
            return;
        }
        String normalized = AuthService.normalizeEmail(email);
        users.saveAndFlush(UserAccount.create(normalized, name.trim(), encoder.encode(password),
                Role.PLACEMENT_ADMIN, null, clock.instant()));
        log.info("Bootstrap Placement Admin created for {}. Remove BOOTSTRAP_ADMIN_PASSWORD from .env now.", normalized);
    }
}
