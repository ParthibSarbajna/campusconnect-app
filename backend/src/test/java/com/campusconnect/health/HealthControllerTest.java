package com.campusconnect.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusconnect.config.CorsConfig;
import com.campusconnect.config.SecurityConfig;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class HealthControllerTest {

    @Autowired MockMvc mvc;
    @MockBean HealthService service;

    private static HealthStatus healthStatus(String overall, String db) {
        return new HealthStatus(overall, "campusconnect-backend", "0.1.0",
                Instant.parse("2026-10-15T00:00:00Z"), db, "1");
    }

    @Test
    void healthIsPublicAndReturns200WhenUp() throws Exception {
        when(service.check()).thenReturn(healthStatus("UP", "UP"));
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.schemaVersion").value("1"))
                .andExpect(jsonPath("$.up").doesNotExist());
    }

    @Test
    void healthReturns503WhenDatabaseIsDown() throws Exception {
        when(service.check()).thenReturn(healthStatus("DOWN", "DOWN"));
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void everyOtherEndpointIsRejectedUntilPhase2Auth() throws Exception {
        mvc.perform(get("/api/v1/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/v1/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/v1/health")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
