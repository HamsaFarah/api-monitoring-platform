package com.hamsa.apimonitoringplatform.service;

import com.hamsa.apimonitoringplatform.entity.Incident;
import com.hamsa.apimonitoringplatform.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {
    @Mock private IncidentRepository repository;
    private IncidentService service;

    @BeforeEach void setUp() { service = new IncidentService(repository); }

    @Test void firstFailureCreatesOpenIncident() {
        when(repository.findByEndpointIdAndStatus(7L, "OPEN")).thenReturn(Optional.empty());
        when(repository.save(any(Incident.class))).thenAnswer(call -> call.getArgument(0));

        service.recordFailure(7L, "TIMEOUT");

        var captor = org.mockito.ArgumentCaptor.forClass(Incident.class);
        verify(repository).save(captor.capture());
        assertEquals(7L, captor.getValue().getEndpointId());
        assertEquals("OPEN", captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getFailureCount());
        assertEquals("TIMEOUT", captor.getValue().getReason());
    }

    @Test void repeatedFailureIncrementsSameIncident() {
        Incident incident = new Incident();
        incident.setEndpointId(7L);
        incident.setStatus("OPEN");
        incident.setFailureCount(2);
        when(repository.findByEndpointIdAndStatus(7L, "OPEN")).thenReturn(Optional.of(incident));

        service.recordFailure(7L, "CONNECTION_ERROR");

        assertEquals(3, incident.getFailureCount());
        verify(repository).save(incident);
    }

    @Test void successResolvesOpenIncident() {
        Incident incident = new Incident();
        incident.setEndpointId(7L);
        incident.setStatus("OPEN");
        incident.setFailureCount(2);
        when(repository.findByEndpointIdAndStatus(7L, "OPEN")).thenReturn(Optional.of(incident));

        service.recordSuccess(7L);

        assertEquals("RESOLVED", incident.getStatus());
        assertNotNull(incident.getResolvedAt());
        verify(repository).save(incident);
    }

    @Test void successDoesNothingWhenNoIncidentIsOpen() {
        when(repository.findByEndpointIdAndStatus(7L, "OPEN")).thenReturn(Optional.empty());
        service.recordSuccess(7L);
        verify(repository, never()).save(any());
    }

    @Test void invalidStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getAllIncidents("BROKEN"));
    }
}
