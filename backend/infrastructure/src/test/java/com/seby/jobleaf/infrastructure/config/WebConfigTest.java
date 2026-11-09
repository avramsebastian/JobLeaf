package com.seby.jobleaf.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

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
}
