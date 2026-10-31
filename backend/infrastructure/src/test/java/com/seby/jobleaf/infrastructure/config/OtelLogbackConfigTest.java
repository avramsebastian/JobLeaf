package com.seby.jobleaf.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mockStatic;

class OtelLogbackConfigTest {

    @Test
    @DisplayName("Should install the OpenTelemetry SDK into the Logback appender once the bean is initialized")
    void afterPropertiesSet_installsOpenTelemetryIntoAppender() {
        // Given
        OpenTelemetry openTelemetry = OpenTelemetry.noop();
        OtelLogbackConfig config = new OtelLogbackConfig(openTelemetry);

        try (MockedStatic<OpenTelemetryAppender> appender = mockStatic(OpenTelemetryAppender.class)) {
            // When
            config.afterPropertiesSet();

            // Then
            appender.verify(() -> OpenTelemetryAppender.install(openTelemetry));
        }
    }
}
