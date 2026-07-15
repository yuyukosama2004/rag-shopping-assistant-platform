package com.biyesheji.gateway.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationHealthMetrics implements MeterBinder {
    private final HealthEndpoint healthEndpoint;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("biyesheji.application.health", healthEndpoint,
                        endpoint -> Status.UP.equals(endpoint.health().getStatus()) ? 1D : 0D)
                .description("Aggregated Spring Boot health status where 1 is UP")
                .register(registry);
    }
}
