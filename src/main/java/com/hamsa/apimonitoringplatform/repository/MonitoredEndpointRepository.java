package com.hamsa.apimonitoringplatform.repository;

import com.hamsa.apimonitoringplatform.entity.MonitoredEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoredEndpointRepository extends JpaRepository<MonitoredEndpoint, Long> {
}