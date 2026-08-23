package com.hamsa.apimonitoringplatform.service;

import com.hamsa.apimonitoringplatform.entity.HealthCheck;
import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import com.hamsa.apimonitoringplatform.repository.HealthCheckRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Service
public class HealthCheckService {

    private final HealthCheckRepository healthCheckRepository;
    private final RestClient restClient;

    public HealthCheckService(HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository;
        this.restClient = RestClient.builder().build();
    }

    public HealthCheck performCheck(MonitoredEndpoint endpoint) {
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.setEndpointId(endpoint.getId());

        long startTime = System.currentTimeMillis();

        try {
            RestClient.ResponseSpec response = restClient
                    .method(org.springframework.http.HttpMethod.valueOf(endpoint.getMethod()))
                    .uri(endpoint.getUrl())
                    .retrieve();

            var entity = response.toBodilessEntity();
            int actualStatus = entity.getStatusCode().value();

            long latency = System.currentTimeMillis() - startTime;

            healthCheck.setStatusCode(actualStatus);
            healthCheck.setLatencyMs((int) latency);
            healthCheck.setSuccess(actualStatus == endpoint.getExpectedStatus());

            if (actualStatus != endpoint.getExpectedStatus()) {
                healthCheck.setErrorType("INVALID_STATUS");
            }

        } catch (RestClientResponseException ex) {
            long latency = System.currentTimeMillis() - startTime;
            healthCheck.setStatusCode(ex.getStatusCode().value());
            healthCheck.setLatencyMs((int) latency);
            healthCheck.setSuccess(false);
            healthCheck.setErrorType("INVALID_STATUS");

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - startTime;
            healthCheck.setStatusCode(null);
            healthCheck.setLatencyMs((int) latency);
            healthCheck.setSuccess(false);
            healthCheck.setErrorType("CONNECTION_ERROR");
        }

        return healthCheckRepository.save(healthCheck);
    }
}