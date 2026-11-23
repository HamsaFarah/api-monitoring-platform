package com.hamsa.apimonitoringplatform.service;

import com.hamsa.apimonitoringplatform.dto.CreateEndpointRequestDTO;
import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import com.hamsa.apimonitoringplatform.repository.HealthCheckRepository;
import com.hamsa.apimonitoringplatform.repository.IncidentRepository;
import com.hamsa.apimonitoringplatform.repository.MonitoredEndpointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MonitoredEndpointService {
    private final MonitoredEndpointRepository repository;
    private final HealthCheckRepository healthCheckRepository;
    private final IncidentRepository incidentRepository;

    public MonitoredEndpointService(MonitoredEndpointRepository repository,
                                    HealthCheckRepository healthCheckRepository,
                                    IncidentRepository incidentRepository) {
        this.repository = repository;
        this.healthCheckRepository = healthCheckRepository;
        this.incidentRepository = incidentRepository;
    }

    public MonitoredEndpoint createEndpoint(CreateEndpointRequestDTO dto) {
        MonitoredEndpoint endpoint = new MonitoredEndpoint();
        apply(endpoint, dto);
        return repository.save(endpoint);
    }

    public List<MonitoredEndpoint> getAllEndpoints() {
        return repository.findAll();
    }

    public MonitoredEndpoint getEndpointById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Endpoint not found with id: " + id));
    }

    public MonitoredEndpoint updateEndpoint(Long id, CreateEndpointRequestDTO dto) {
        MonitoredEndpoint existing = getEndpointById(id);
        apply(existing, dto);
        return repository.save(existing);
    }

    private void apply(MonitoredEndpoint endpoint, CreateEndpointRequestDTO dto) {
        endpoint.setName(dto.getName());
        endpoint.setUrl(dto.getUrl());
        endpoint.setMethod(dto.getMethod());
        endpoint.setExpectedStatus(dto.getExpectedStatus());
        endpoint.setIntervalSeconds(dto.getIntervalSeconds());
        endpoint.setTimeoutMs(dto.getTimeoutMs());
        endpoint.setLatencyThresholdMs(dto.getLatencyThresholdMs());
        endpoint.setEnabled(dto.isEnabled());
    }

    @Transactional
    public void deleteEndpoint(Long id) {
        MonitoredEndpoint existing = getEndpointById(id);
        healthCheckRepository.deleteByEndpointId(id);
        incidentRepository.deleteByEndpointId(id);
        repository.delete(existing);
    }
}
