package com.hamsa.apimonitoringplatform.service;

import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import com.hamsa.apimonitoringplatform.repository.HealthCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "monitoring.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class HealthCheckScheduler {
    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);
    private final MonitoredEndpointService endpointService;
    private final HealthCheckService healthCheckService;
    private final HealthCheckRepository healthCheckRepository;

    public HealthCheckScheduler(MonitoredEndpointService endpointService,
                                HealthCheckService healthCheckService,
                                HealthCheckRepository healthCheckRepository) {
        this.endpointService = endpointService;
        this.healthCheckService = healthCheckService;
        this.healthCheckRepository = healthCheckRepository;
    }

    @Scheduled(fixedDelayString = "${monitoring.scheduler-delay-ms:5000}")
    public void runDueChecks() {
        for (MonitoredEndpoint endpoint : endpointService.getAllEndpoints()) {
            if (!endpoint.isEnabled()) continue;
            try {
                LocalDateTime dueBefore = LocalDateTime.now().minusSeconds(endpoint.getIntervalSeconds());
                boolean due = healthCheckRepository.findTopByEndpointIdOrderByCheckedAtDesc(endpoint.getId())
                        .map(last -> last.getCheckedAt().isBefore(dueBefore))
                        .orElse(true);
                if (due) healthCheckService.performCheck(endpoint);
            } catch (Exception ex) {
                log.warn("Scheduled check failed for endpoint {}: {}", endpoint.getId(), ex.getMessage());
            }
        }
    }
}
