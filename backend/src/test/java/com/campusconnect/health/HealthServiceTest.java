package com.campusconnect.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneOffset.UTC);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final HealthService service = new HealthService(jdbc, clock, "0.1.0");

    @Test
    void reportsUpWithSchemaVersionWhenDatabaseAnswers() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(String.class))).thenReturn("1");

        HealthStatus s = service.check();

        assertThat(s.status()).isEqualTo("UP");
        assertThat(s.database()).isEqualTo("UP");
        assertThat(s.schemaVersion()).isEqualTo("1");
        assertThat(s.version()).isEqualTo("0.1.0");
        assertThat(s.timestamp()).isEqualTo(Instant.parse("2026-10-15T00:00:00Z"));
    }

    @Test
    void reportsDownWhenDatabaseIsUnreachable() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new CannotGetJdbcConnectionException("no connection"));

        HealthStatus s = service.check();

        assertThat(s.status()).isEqualTo("DOWN");
        assertThat(s.database()).isEqualTo("DOWN");
        assertThat(s.schemaVersion()).isNull();
    }

    @Test
    void staysUpWithNullSchemaWhenNoMigrationRowExists() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(String.class))).thenThrow(new EmptyResultDataAccessException(1));

        HealthStatus s = service.check();

        assertThat(s.status()).isEqualTo("UP");
        assertThat(s.schemaVersion()).isNull();
    }
}
