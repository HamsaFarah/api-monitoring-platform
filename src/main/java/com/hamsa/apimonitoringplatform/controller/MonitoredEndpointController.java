package com.hamsa.apimonitoringplatform.controller;

import com.hamsa.apimonitoringplatform.dto.CreateEndpointRequestDTO;
import com.hamsa.apimonitoringplatform.service.MonitoredEndpointService;

import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/endpoints")
public class MonitoredEndpointController {

    private final MonitoredEndpointService service;

    public MonitoredEndpointController(MonitoredEndpointService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MonitoredEndpoint> createEndpoint(@Valid @RequestBody CreateEndpointRequestDTO dto) {
        MonitoredEndpoint created = service.createEndpoint(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<MonitoredEndpoint>> getAllEndpoints() {
        return ResponseEntity.ok(service.getAllEndpoints());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitoredEndpoint> getEndpointById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEndpointById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitoredEndpoint> updateEndpoint(
            @PathVariable Long id,
            @RequestBody MonitoredEndpoint endpoint) {
        return ResponseEntity.ok(service.updateEndpoint(id, endpoint));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable Long id) {
        service.deleteEndpoint(id);
        return ResponseEntity.noContent().build();
    }
}