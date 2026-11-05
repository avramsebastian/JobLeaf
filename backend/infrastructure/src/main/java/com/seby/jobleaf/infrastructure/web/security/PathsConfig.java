package com.seby.jobleaf.infrastructure.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PathsConfig {

    @Bean(name = "publicPaths")
    public List<String> publicPaths() {
        return List.of(
                "/jobleaf/companies/public",
                "/jobleaf/contacts/public",
                "/jobleaf/auth/login/public",
                "/jobleaf/swagger-ui.html",
                "/swagger-ui/**",
                "/jobleaf/v3/api-docs/**",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/webjars/**"
        );
    }

    @Bean(name = "securedPaths")
    public List<String> securedPaths() {
        return List.of(
                "/jobleaf/**"
        );
    }

    @Bean(name = "infrastructurePaths")
    public List<String> infrastructurePaths() {
        return List.of(
                "/actuator/health/**",
                "/actuator/prometheus",
                "/error"
        );
    }

}
