package com.hamsa.apimonitoringplatform.dto;

import jakarta.validation.constraints.*;

public class CreateEndpointRequestDTO {
    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be 120 characters or fewer")
    private String name;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
    @Size(max = 2048, message = "URL must be 2048 characters or fewer")
    private String url;

    @NotBlank(message = "Method is required")
    @Pattern(regexp = "(?i)GET|POST|HEAD|OPTIONS", message = "Method must be GET, POST, HEAD, or OPTIONS")
    private String method = "GET";

    @NotNull(message = "Expected status is required")
    @Min(value = 100, message = "Expected status must be a valid HTTP status code")
    @Max(value = 599, message = "Expected status must be a valid HTTP status code")
    private Integer expectedStatus = 200;

    @NotNull(message = "Interval is required")
    @Min(value = 1, message = "Interval must be greater than zero")
    @Max(value = 86400, message = "Interval cannot exceed 24 hours")
    private Integer intervalSeconds = 60;

    @NotNull(message = "Timeout is required")
    @Min(value = 100, message = "Timeout must be at least 100 ms")
    @Max(value = 120000, message = "Timeout cannot exceed 120000 ms")
    private Integer timeoutMs = 5000;

    @Min(value = 1, message = "Latency threshold must be greater than zero")
    private Integer latencyThresholdMs;

    private boolean enabled = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url == null ? null : url.trim(); }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method == null ? null : method.toUpperCase().trim(); }
    public Integer getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(Integer expectedStatus) { this.expectedStatus = expectedStatus; }
    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Integer getLatencyThresholdMs() { return latencyThresholdMs; }
    public void setLatencyThresholdMs(Integer latencyThresholdMs) { this.latencyThresholdMs = latencyThresholdMs; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
