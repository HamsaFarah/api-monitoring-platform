package com.hamsa.apimonitoringplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiMonitoringIncidentPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiMonitoringIncidentPlatformApplication.class, args);
	}

}
