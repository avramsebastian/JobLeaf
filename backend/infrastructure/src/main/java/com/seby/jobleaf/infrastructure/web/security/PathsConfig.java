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

    @Bean(name = "adminPaths")
    public List<String> adminPaths() {
        return List.of(
                "/jobleaf/contacts/admin",
                "/jobleaf/contacts/sort/admin",
                "/jobleaf/contacts/page/admin",
                "/jobleaf/contacts/{id}/status/admin",
                "/jobleaf/companies/admin",
                "/jobleaf/companies/{id}/admin",
                "/jobleaf/users/search/admin",
                "/jobleaf/users/{userId}/role/employer/admin",
                "/jobleaf/users/{userId}/company/{companyId}/admin"
        );
    }

    @Bean(name = "employerPaths")
    public List<String> employerPaths() {
        return List.of(
                "/jobleaf/jobs/employer",
                "/jobleaf/jobs/{jobId}/status/employer",
                "/jobleaf/jobs/{jobId}/applications/employer",
                "/jobleaf/jobs/applications/{applicationId}/employer",
                "/jobleaf/jobs/applications/{applicationId}/picture/employer",
                "/jobleaf/jobs/applications/{applicationId}/resume/employer"
        );
    }

    @Bean(name = "jobSeekerPaths")
    public List<String> jobSeekerPaths() {
        return List.of(
                "/jobleaf/profile/jobseeker",
                "/jobleaf/profile/picture/jobseeker",
                "/jobleaf/profile/resume/jobseeker",
                "/jobleaf/saved-jobs/{jobId}/jobseeker",
                "/jobleaf/saved-jobs/jobseeker",
                "/jobleaf/job-applications/jobseeker",
                "/jobleaf/job-applications/{jobId}/jobseeker"
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
