package com.hamsa.apimonitoringplatform.repository;

import com.hamsa.apimonitoringplatform.entity.HealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {

    List<HealthCheck> findByEndpointId(Long endpointId);
}