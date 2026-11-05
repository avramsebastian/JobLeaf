package com.seby.jobleaf.infrastructure.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class JobLeafSecurityConfigTest {

    @Test
    @DisplayName("Should allow the frontend origin with credentials for every path")
    void corsConfigurationSource_allowsFrontendOrigin() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobleaf/companies/public");

        // When
        CorsConfiguration config = new JobLeafSecurityConfig().corsConfigurationSource().getCorsConfiguration(request);

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(config.getAllowedMethods()).containsExactly("*");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Should keep health checks, Prometheus and the error page reachable without authentication")
    void infrastructurePaths_includeOperationalEndpoints() {
        assertThat(new PathsConfig().infrastructurePaths())
                .containsExactlyInAnyOrder("/actuator/health/**", "/actuator/prometheus", "/error");
    }
}
