package com.hamsa.apimonitoringplatform.service;


import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;


import com.hamsa.apimonitoringplatform.dto.CreateEndpointRequestDTO;
import com.hamsa.apimonitoringplatform.repository.MonitoredEndpointRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MonitoredEndpointService {

    private final MonitoredEndpointRepository repository;

    public MonitoredEndpointService(MonitoredEndpointRepository repository) {
        this.repository = repository;
    }

    public MonitoredEndpoint createEndpoint(CreateEndpointRequestDTO dto) {
        MonitoredEndpoint monitoredEndpoint = new MonitoredEndpoint();
        monitoredEndpoint.setName(dto.getName());
        monitoredEndpoint.setEnabled(dto.isEnabled());
        monitoredEndpoint.setExpectedStatus(dto.getExpectedStatus());
        monitoredEndpoint.setIntervalSeconds(dto.getIntervalSeconds());
        monitoredEndpoint.setTimeoutMs(dto.getTimeoutMs());
        monitoredEndpoint.setLatencyThresholdMs(dto.getLatencyThresholdMs());
        monitoredEndpoint.setUrl(dto.getUrl());
        monitoredEndpoint.setMethod(dto.getMethod());
        return repository.save(monitoredEndpoint);
    }

    public List<MonitoredEndpoint> getAllEndpoints() {
        return repository.findAll();
    }

    public MonitoredEndpoint getEndpointById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Endpoint not found with id: " + id));
    }

    public MonitoredEndpoint updateEndpoint(Long id, MonitoredEndpoint updatedEndpoint) {
        MonitoredEndpoint existing = getEndpointById(id);

        existing.setName(updatedEndpoint.getName());
        existing.setUrl(updatedEndpoint.getUrl());
        existing.setMethod(updatedEndpoint.getMethod());
        existing.setExpectedStatus(updatedEndpoint.getExpectedStatus());
        existing.setIntervalSeconds(updatedEndpoint.getIntervalSeconds());
        existing.setTimeoutMs(updatedEndpoint.getTimeoutMs());
        existing.setLatencyThresholdMs(updatedEndpoint.getLatencyThresholdMs());
        existing.setEnabled(updatedEndpoint.isEnabled());

        return repository.save(existing);
    }

    public void deleteEndpoint(Long id) {
        MonitoredEndpoint existing = getEndpointById(id);
        repository.delete(existing);
    }
}
