package com.hamsa.apimonitoringplatform;

import com.hamsa.apimonitoringplatform.entity.Incident;
import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import com.hamsa.apimonitoringplatform.repository.HealthCheckRepository;
import com.hamsa.apimonitoringplatform.repository.IncidentRepository;
import com.hamsa.apimonitoringplatform.repository.MonitoredEndpointRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiEndpointWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MonitoredEndpointRepository endpointRepository;
    @Autowired private HealthCheckRepository healthCheckRepository;
    @Autowired private IncidentRepository incidentRepository;

    private HttpServer mockApi;
    private AtomicInteger mockApiStatus;
    private String mockApiUrl;

    @BeforeEach
    void setUp() throws Exception {
        healthCheckRepository.deleteAll();
        incidentRepository.deleteAll();
        endpointRepository.deleteAll();

        mockApiStatus = new AtomicInteger(200);
        mockApi = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockApi.createContext("/health", exchange -> {
            int status = mockApiStatus.get();
            byte[] body = "mock health response".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        mockApi.start();
        mockApiUrl = "http://127.0.0.1:" + mockApi.getAddress().getPort() + "/health";
    }

    @AfterEach
    void tearDown() {
        if (mockApi != null) {
            mockApi.stop(0);
        }
    }

    @Test
    void endpointCanBeCreatedReadUpdatedDisabledAndDeleted() throws Exception {
        mockMvc.perform(post("/api/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endpointJson("Workflow API", mockApiUrl, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Workflow API"))
                .andExpect(jsonPath("$.enabled").value(true));

        MonitoredEndpoint created = endpointRepository.findAll().get(0);
        long id = created.getId();

        mockMvc.perform(get("/api/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/endpoints/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(mockApiUrl));

        mockMvc.perform(put("/api/endpoints/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endpointJson("Updated API", mockApiUrl, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated API"));

        mockMvc.perform(patch("/api/endpoints/{id}/enabled", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(delete("/api/endpoints/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/endpoints/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void successfulHealthCheckIsSavedAndAppearsInHistoryAndStats() throws Exception {
        long endpointId = createEndpoint("Healthy API");

        mockMvc.perform(post("/api/endpoints/{id}/check", endpointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpointId").value(endpointId))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200));

        mockMvc.perform(get("/api/endpoints/{id}/checks", endpointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].success").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/endpoints/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEndpoints").value(1))
                .andExpect(jsonPath("$.totalChecks").value(1))
                .andExpect(jsonPath("$.openIncidents").value(0))
                .andExpect(jsonPath("$.successRate").value(100.0));
    }

    @Test
    void failedCheckOpensIncidentAndLaterSuccessfulCheckResolvesIt() throws Exception {
        long endpointId = createEndpoint("Incident Workflow API");

        mockApiStatus.set(503);
        mockMvc.perform(post("/api/endpoints/{id}/check", endpointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(503))
                .andExpect(jsonPath("$.errorType").value("INVALID_STATUS"));

        mockMvc.perform(get("/api/incidents").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].endpointId").value(endpointId))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].failureCount").value(1))
                .andExpect(jsonPath("$[0].reason").value("INVALID_STATUS"));

        Incident openIncident = incidentRepository.findByEndpointIdAndStatus(endpointId, "OPEN")
                .orElseThrow();

        mockApiStatus.set(200);
        mockMvc.perform(post("/api/endpoints/{id}/check", endpointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/incidents").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(openIncident.getId()))
                .andExpect(jsonPath("$[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$[0].resolvedAt").isNotEmpty());
    }

    @Test
    void invalidPayloadAndUnknownResourcesReturnHelpfulClientErrors() throws Exception {
        mockMvc.perform(post("/api/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","url":"ftp://not-allowed","method":"PATCH",
                                 "expectedStatus":999,"intervalSeconds":0,"timeoutMs":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/endpoints/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(get("/api/incidents/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(get("/api/incidents").param("status", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    private long createEndpoint(String name) throws Exception {
        mockMvc.perform(post("/api/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endpointJson(name, mockApiUrl, true)))
                .andExpect(status().isCreated());
        return endpointRepository.findAll().get(0).getId();
    }

    private String endpointJson(String name, String url, boolean enabled) {
        return """
                {
                  "name": "%s",
                  "url": "%s",
                  "method": "GET",
                  "expectedStatus": 200,
                  "intervalSeconds": 3600,
                  "timeoutMs": 3000,
                  "latencyThresholdMs": null,
                  "enabled": %s
                }
                """.formatted(name, url, enabled);
    }
}
