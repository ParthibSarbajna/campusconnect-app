package com.campusconnect.common;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/** Single password rule used by registration, admin account creation, password change and bootstrap. */
public final class PasswordPolicy {
    public static final String MESSAGE = "Password must be 10-72 characters and include a letter and a digit.";
    private static final Pattern RULE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{10,72}$");

    private PasswordPolicy() {}

    public static void validate(String password) {
        // 72 BYTES is BCrypt's hard input limit, so long non-ASCII passwords are rejected rather than truncated.
        if (password == null || !RULE.matcher(password).matches()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, MESSAGE);
        }
    }
}
