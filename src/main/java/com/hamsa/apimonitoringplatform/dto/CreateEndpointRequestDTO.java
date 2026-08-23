package com.hamsa.apimonitoringplatform.dto;

import jakarta.validation.constraints.*;

public class CreateEndpointRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
    private String url;

    @NotBlank(message = "Method is required")
    private String method;

    @NotNull(message = "Expected status is required")
    @Min(value = 100, message = "Expected status must be a valid HTTP status code")
    @Max(value = 599, message = "Expected status must be a valid HTTP status code")
    private Integer expectedStatus;

    @NotNull(message = "Interval is required")
    @Min(value = 1, message = "Interval must be greater than zero")
    private Integer intervalSeconds;

    @NotNull(message = "Timeout is required")
    @Min(value = 1, message = "Timeout must be greater than zero")
    private Integer timeoutMs;

    @Min(value = 1, message = "Latency threshold must be greater than zero")
    private Integer latencyThresholdMs;

    private boolean enabled;

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getLatencyThresholdMs() {
        return latencyThresholdMs;
    }

    public void setLatencyThresholdMs(Integer latencyThresholdMs) {
        this.latencyThresholdMs = latencyThresholdMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}