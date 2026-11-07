package com.campusconnect.health;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    static final String SERVICE_NAME = "campusconnect-backend";
    private static final String PING_SQL = "select 1";
    private static final String SCHEMA_SQL =
            "select version from flyway_schema_history where success = true order by installed_rank desc limit 1";

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final String version;

    public HealthService(JdbcTemplate jdbc, Clock clock, @Value("${app.version:unknown}") String version) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.version = version;
    }

    public HealthStatus check() {
        boolean dbUp = true;
        String schema = null;
        try {
            jdbc.queryForObject(PING_SQL, Integer.class);
        } catch (DataAccessException e) {
            dbUp = false;
        }
        if (dbUp) {
            try {
                schema = jdbc.queryForObject(SCHEMA_SQL, String.class);
            } catch (DataAccessException e) {
                schema = null; // database reachable but no applied migration found
            }
        }
        String dbState = dbUp ? "UP" : "DOWN";
        return new HealthStatus(dbUp ? "UP" : "DOWN", SERVICE_NAME, version, Instant.now(clock), dbState, schema);
    }
}
