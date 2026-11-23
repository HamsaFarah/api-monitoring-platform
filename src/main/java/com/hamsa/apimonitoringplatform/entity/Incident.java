package com.hamsa.apimonitoringplatform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint_id", nullable = false)
    private Long endpointId;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount;

    @Column
    private String reason;

    public Incident() {
    }

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(Long endpointId) {
        this.endpointId = endpointId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}