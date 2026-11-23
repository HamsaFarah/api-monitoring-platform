package com.hamsa.apimonitoringplatform.repository;

import com.hamsa.apimonitoringplatform.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findByEndpointIdAndStatus(Long endpointId, String status);
    List<Incident> findByStatusOrderByStartedAtDesc(String status);
    List<Incident> findAllByOrderByStartedAtDesc();
    void deleteByEndpointId(Long endpointId);
    long countByStatus(String status);
}
