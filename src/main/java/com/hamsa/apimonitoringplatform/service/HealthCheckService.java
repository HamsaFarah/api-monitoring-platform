package com.hamsa.apimonitoringplatform.service;

import com.hamsa.apimonitoringplatform.entity.HealthCheck;
import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import com.hamsa.apimonitoringplatform.repository.HealthCheckRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;

@Service
public class HealthCheckService {
    private final HealthCheckRepository healthCheckRepository;
    private final MonitoredEndpointService monitoredEndpointService;
    private final IncidentService incidentService;

    public HealthCheckService(HealthCheckRepository healthCheckRepository,
                              MonitoredEndpointService monitoredEndpointService,
                              IncidentService incidentService) {
        this.healthCheckRepository = healthCheckRepository;
        this.monitoredEndpointService = monitoredEndpointService;
        this.incidentService = incidentService;
    }

    @Transactional
    public HealthCheck performCheck(MonitoredEndpoint endpoint) {
        HealthCheck check = new HealthCheck();
        check.setEndpointId(endpoint.getId());
        long startTime = System.nanoTime();

        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofMillis(endpoint.getTimeoutMs()));
            factory.setReadTimeout(Duration.ofMillis(endpoint.getTimeoutMs()));
            RestClient client = RestClient.builder().requestFactory(factory).build();

            int status = client.method(HttpMethod.valueOf(endpoint.getMethod()))
                    .uri(endpoint.getUrl())
                    .exchange((request, response) -> response.getStatusCode().value());
            int latency = elapsedMs(startTime);
            check.setStatusCode(status);
            check.setLatencyMs(latency);
            if (status != endpoint.getExpectedStatus()) {
                check.setSuccess(false);
                check.setErrorType("INVALID_STATUS");
            } else if (endpoint.getLatencyThresholdMs() != null && latency > endpoint.getLatencyThresholdMs()) {
                check.setSuccess(false);
                check.setErrorType("LATENCY_THRESHOLD");
            } else {
                check.setSuccess(true);
            }
        } catch (ResourceAccessException ex) {
            check.setStatusCode(null);
            check.setLatencyMs(elapsedMs(startTime));
            check.setSuccess(false);
            check.setErrorType(hasTimeoutCause(ex) ? "TIMEOUT" : "CONNECTION_ERROR");
        } catch (Exception ex) {
            check.setStatusCode(null);
            check.setLatencyMs(elapsedMs(startTime));
            check.setSuccess(false);
            check.setErrorType("UNKNOWN");
        }

        HealthCheck saved = healthCheckRepository.save(check);
        if (saved.isSuccess()) incidentService.recordSuccess(endpoint.getId());
        else incidentService.recordFailure(endpoint.getId(), saved.getErrorType());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<HealthCheck> findByEndpointId(Long endpointId, int page, int size) {
        monitoredEndpointService.getEndpointById(endpointId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return healthCheckRepository.findByEndpointIdOrderByCheckedAtDesc(endpointId,
                PageRequest.of(safePage, safeSize));
    }

    private int elapsedMs(long startNanos) {
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        return (int) Math.min(Integer.MAX_VALUE, elapsed);
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) return true;
            cause = cause.getCause();
        }
        return false;
    }
}
