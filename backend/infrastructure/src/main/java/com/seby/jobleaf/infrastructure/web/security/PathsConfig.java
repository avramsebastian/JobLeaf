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
                "/jobleaf/auth/register/public",
                "/jobleaf/auth/logout/public",
                "/jobleaf/csrf-token/public",
                "/jobleaf/swagger-ui.html",
                "/swagger-ui/**",
                "/jobleaf/v3/api-docs/**",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/webjars/**"
        );
    }

    /**
     * Only users with {@code ROLE_ADMIN} may call these. Spring path patterns use {@code {id}} for a path variable
     * (not {@code ${id}}, which would be matched literally and never apply).
     */
    @Bean(name = "adminPaths")
    public List<String> adminPaths() {
        return List.of(
                "/jobleaf/contacts/admin",
                "/jobleaf/contacts/sort/admin",
                "/jobleaf/contacts/page/admin",
                "/jobleaf/contacts/{id}/status/admin"
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
