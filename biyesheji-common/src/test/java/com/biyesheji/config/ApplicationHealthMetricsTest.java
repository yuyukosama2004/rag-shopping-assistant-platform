package com.biyesheji.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationHealthMetricsTest {
    @Test
    void exportsAggregatedHealthAsGauge() {
        HealthEndpoint endpoint = mock(HealthEndpoint.class);
        when(endpoint.health()).thenReturn(Health.up().build(), Health.down().build());
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ApplicationHealthMetrics(endpoint).bindTo(registry);

        assertEquals(1D, registry.get("biyesheji.application.health").gauge().value());
        assertEquals(0D, registry.get("biyesheji.application.health").gauge().value());
    }
}
