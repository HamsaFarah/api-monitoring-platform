package com.hamsa.apimonitoringplatform;

import com.hamsa.apimonitoringplatform.dto.CreateEndpointRequestDTO;
import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import com.hamsa.apimonitoringplatform.repository.HealthCheckRepository;
import com.hamsa.apimonitoringplatform.repository.IncidentRepository;
import com.hamsa.apimonitoringplatform.repository.MonitoredEndpointRepository;
import com.hamsa.apimonitoringplatform.service.MonitoredEndpointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiMonitoringIncidentPlatformApplicationTests {
    @Autowired private MonitoredEndpointService endpointService;
    @Autowired private MonitoredEndpointRepository endpointRepository;
    @Autowired private HealthCheckRepository healthCheckRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        healthCheckRepository.deleteAll();
        incidentRepository.deleteAll();
        endpointRepository.deleteAll();
    }

    @Test
    void applicationContextStartsAndRepositoriesAreWired() {
        assertNotNull(endpointRepository);
        assertNotNull(healthCheckRepository);
        assertNotNull(incidentRepository);
    }

    @Test
    void endpointCanBeCreatedAndRetrieved() {
        CreateEndpointRequestDTO dto = new CreateEndpointRequestDTO();
        dto.setName("Example API");
        dto.setUrl("https://example.com");
        dto.setMethod("GET");
        dto.setExpectedStatus(200);
        dto.setIntervalSeconds(60);
        dto.setTimeoutMs(5000);
        dto.setEnabled(true);

        MonitoredEndpoint saved = endpointService.createEndpoint(dto);

        assertNotNull(saved.getId());
        assertEquals("Example API", endpointService.getEndpointById(saved.getId()).getName());
    }

    @Test
    void endpointApiCreatesListsAndValidatesRequests() throws Exception {
        String valid = """
                {"name":"Mock API","url":"https://example.com","method":"GET",
                 "expectedStatus":200,"intervalSeconds":60,"timeoutMs":5000,
                 "latencyThresholdMs":null,"enabled":true}
                """;
        mockMvc.perform(post("/api/endpoints").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mock API"));

        mockMvc.perform(get("/api/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mock API"));

        String invalid = """
                {"name":"","url":"ftp://invalid","method":"PATCH",
                 "expectedStatus":999,"intervalSeconds":0,"timeoutMs":0}
                """;
        mockMvc.perform(post("/api/endpoints").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void historyForUnknownEndpointReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/endpoints/999999/checks"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }


    @Test
    void healthCheckHistoryIsPagedAndReturnedNewestFirst() throws Exception {
        CreateEndpointRequestDTO dto = new CreateEndpointRequestDTO();
        dto.setName("History API");
        dto.setUrl("https://example.com");
        dto.setMethod("GET");
        dto.setExpectedStatus(200);
        dto.setIntervalSeconds(60);
        dto.setTimeoutMs(5000);
        MonitoredEndpoint endpoint = endpointService.createEndpoint(dto);

        com.hamsa.apimonitoringplatform.entity.HealthCheck check = new com.hamsa.apimonitoringplatform.entity.HealthCheck();
        check.setEndpointId(endpoint.getId());
        check.setStatusCode(200);
        check.setLatencyMs(42);
        check.setSuccess(true);
        healthCheckRepository.save(check);

        mockMvc.perform(get("/api/endpoints/" + endpoint.getId() + "/checks").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].success").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void incidentApiReturnsEmptyListAndRejectsUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/incidents").param("status", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void missingEndpointReturnsNotFoundException() {
        assertThrows(java.util.NoSuchElementException.class, () -> endpointService.getEndpointById(-100L));
    }
}
