package com.hamsa.apimonitoringplatform.controller;

import com.hamsa.apimonitoringplatform.entity.Incident;
import com.hamsa.apimonitoringplatform.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<List<Incident>> getIncidents(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(incidentService.getAllIncidents(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }
}
