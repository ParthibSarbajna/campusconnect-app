package com.campusconnect.health;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/** Public health payload. Contains no secrets, hostnames, or connection details. */
public record HealthStatus(
        String status,
        String service,
        String version,
        Instant timestamp,
        String database,
        String schemaVersion) {

    /** Internal helper; excluded from JSON so the payload has only the documented fields. */
    @JsonIgnore
    public boolean isUp() {
        return "UP".equals(status);
    }
}
