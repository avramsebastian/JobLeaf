package com.seby.jobleaf.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebConfigTest {

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig();
    }

    @Test
    @DisplayName("Should configure API versioning with vendor media type and supported versions")
    void configureApiVersioning_configuresSupportedAndDefaultVersions() {
        // Given
        ApiVersionConfigurer configurer = mock(ApiVersionConfigurer.class);
        when(configurer.useMediaTypeParameter(any(MediaType.class), eq("v"))).thenReturn(configurer);
        when(configurer.addSupportedVersions("1.0", "2.0")).thenReturn(configurer);

        // When
        webConfig.configureApiVersioning(configurer);

        // Then
        verify(configurer).useMediaTypeParameter(MediaType.parseMediaType("application/vnd.jobleaf+json"), "v");
        verify(configurer).addSupportedVersions("1.0", "2.0");
        verify(configurer).setDefaultVersion("1.0");
    }

    @Test
    @DisplayName("Should configure path prefix for all endpoints with /jobleaf")
    void configurePathMatch_addsJobLeafPrefix() {
        // Given
        PathMatchConfigurer configurer = mock(PathMatchConfigurer.class);

        // When
        webConfig.configurePathMatch(configurer);

        // Then
        verify(configurer).addPathPrefix(eq("/jobleaf"), any());
    }

    private static class TestCorsRegistry extends CorsRegistry {
        @Override
        public Map<String, CorsConfiguration> getCorsConfigurations() {
            return super.getCorsConfigurations();
        }
    }

    @Test
    @DisplayName("Should configure CORS mappings for /jobleaf/** with localhost origin and credentials enabled")
    void addCorsMappings_configuresCorsRegistry() {
        // Given
        TestCorsRegistry registry = new TestCorsRegistry();

        // When
        webConfig.addCorsMappings(registry);

        // Then
        Map<String, CorsConfiguration> corsConfigurations = registry.getCorsConfigurations();
        assertThat(corsConfigurations).containsKey("/jobleaf/**");

        CorsConfiguration config = corsConfigurations.get("/jobleaf/**");
        assertThat(config.getAllowedOrigins()).contains("http://localhost:5173");
        assertThat(config.getAllowedMethods()).contains("*");
        assertThat(config.getAllowedHeaders()).contains("*");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }
}
