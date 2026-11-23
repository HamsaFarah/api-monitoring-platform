package com.hamsa.apimonitoringplatform.controller;

import com.hamsa.apimonitoringplatform.dto.CreateEndpointRequestDTO;
import com.hamsa.apimonitoringplatform.entity.HealthCheck;
import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import com.hamsa.apimonitoringplatform.repository.HealthCheckRepository;
import com.hamsa.apimonitoringplatform.repository.IncidentRepository;
import com.hamsa.apimonitoringplatform.service.HealthCheckService;
import com.hamsa.apimonitoringplatform.service.MonitoredEndpointService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/endpoints")
public class MonitoredEndpointController {
    private final MonitoredEndpointService service;
    private final HealthCheckService healthCheckService;
    private final HealthCheckRepository healthCheckRepository;
    private final IncidentRepository incidentRepository;

    public MonitoredEndpointController(MonitoredEndpointService service,
                                       HealthCheckService healthCheckService,
                                       HealthCheckRepository healthCheckRepository,
                                       IncidentRepository incidentRepository) {
        this.service = service;
        this.healthCheckService = healthCheckService;
        this.healthCheckRepository = healthCheckRepository;
        this.incidentRepository = incidentRepository;
    }

    @PostMapping
    public ResponseEntity<MonitoredEndpoint> createEndpoint(@Valid @RequestBody CreateEndpointRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEndpoint(dto));
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<HealthCheck> triggerHealthCheck(@PathVariable Long id) {
        return ResponseEntity.ok(healthCheckService.performCheck(service.getEndpointById(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitoredEndpoint> getEndpointById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEndpointById(id));
    }

    @GetMapping
    public ResponseEntity<List<MonitoredEndpoint>> getAllEndpoints() {
        return ResponseEntity.ok(service.getAllEndpoints());
    }

    @GetMapping("/{id}/checks")
    public ResponseEntity<Page<HealthCheck>> getHealthCheckHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(healthCheckService.findByEndpointId(id, page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitoredEndpoint> updateEndpoint(
            @PathVariable Long id, @Valid @RequestBody CreateEndpointRequestDTO dto) {
        return ResponseEntity.ok(service.updateEndpoint(id, dto));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<MonitoredEndpoint> setEnabled(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        if (body == null || body.get("enabled") == null) {
            throw new IllegalArgumentException("Request body must include an enabled boolean");
        }
        MonitoredEndpoint endpoint = service.getEndpointById(id);
        endpoint.setEnabled(body.get("enabled"));
        // Reuse update flow with all existing values.
        CreateEndpointRequestDTO dto = new CreateEndpointRequestDTO();
        dto.setName(endpoint.getName());
        dto.setUrl(endpoint.getUrl());
        dto.setMethod(endpoint.getMethod());
        dto.setExpectedStatus(endpoint.getExpectedStatus());
        dto.setIntervalSeconds(endpoint.getIntervalSeconds());
        dto.setTimeoutMs(endpoint.getTimeoutMs());
        dto.setLatencyThresholdMs(endpoint.getLatencyThresholdMs());
        dto.setEnabled(body.get("enabled"));
        return ResponseEntity.ok(service.updateEndpoint(id, dto));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long total = service.getAllEndpoints().size();
        long openIncidents = incidentRepository.countByStatus("OPEN");
        long totalChecks = healthCheckRepository.count();
        long successfulChecks = healthCheckRepository.countBySuccess(true);
        return ResponseEntity.ok(Map.of(
                "totalEndpoints", total,
                "openIncidents", openIncidents,
                "totalChecks", totalChecks,
                "successRate", totalChecks == 0 ? 100.0 : Math.round(successfulChecks * 10000.0 / totalChecks) / 100.0
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable Long id) {
        service.deleteEndpoint(id);
        return ResponseEntity.noContent().build();
    }
}
