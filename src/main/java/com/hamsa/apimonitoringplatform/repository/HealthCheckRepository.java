package com.hamsa.apimonitoringplatform.repository;

import com.hamsa.apimonitoringplatform.entity.HealthCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {
    Page<HealthCheck> findByEndpointIdOrderByCheckedAtDesc(Long endpointId, Pageable pageable);
    Optional<HealthCheck> findTopByEndpointIdOrderByCheckedAtDesc(Long endpointId);
    long countBySuccess(boolean success);
    long countByEndpointIdAndSuccess(Long endpointId, boolean success);
    void deleteByEndpointId(Long endpointId);
}
