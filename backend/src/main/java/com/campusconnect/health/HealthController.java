package com.campusconnect.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService service;

    public HealthController(HealthService service) {
        this.service = service;
    }

    /** 200 when the service and database are up, 503 otherwise. */
    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        HealthStatus status = service.check();
        return status.isUp() ? ResponseEntity.ok(status) : ResponseEntity.status(503).body(status);
    }
}
