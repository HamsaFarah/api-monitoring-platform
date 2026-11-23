package com.hamsa.apimonitoringplatform.service;

import com.hamsa.apimonitoringplatform.entity.Incident;
import com.hamsa.apimonitoringplatform.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public void recordFailure(Long endpointId, String reason) {
        Incident incident = incidentRepository.findByEndpointIdAndStatus(endpointId, "OPEN")
                .orElseGet(() -> {
                    Incident created = new Incident();
                    created.setEndpointId(endpointId);
                    created.setStatus("OPEN");
                    created.setFailureCount(0);
                    created.setReason(reason);
                    return created;
                });
        incident.setFailureCount(incident.getFailureCount() + 1);
        if (incident.getReason() == null) incident.setReason(reason);
        incidentRepository.save(incident);
    }

    @Transactional
    public void recordSuccess(Long endpointId) {
        incidentRepository.findByEndpointIdAndStatus(endpointId, "OPEN").ifPresent(incident -> {
            incident.setStatus("RESOLVED");
            incident.setResolvedAt(LocalDateTime.now());
            incidentRepository.save(incident);
        });
    }

    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents(String status) {
        if (status == null || status.isBlank()) return incidentRepository.findAllByOrderByStartedAtDesc();
        String normalized = status.trim().toUpperCase();
        if (!normalized.equals("OPEN") && !normalized.equals("RESOLVED")) {
            throw new IllegalArgumentException("Status must be OPEN or RESOLVED");
        }
        return incidentRepository.findByStatusOrderByStartedAtDesc(normalized);
    }

    @Transactional(readOnly = true)
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found with id: " + id));
    }
}
