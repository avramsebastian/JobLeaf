package com.seby.jobleaf.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing for {@code @CreatedDate} / {@code @LastModifiedDate} on entities.
 * <p>
 * No {@code AuditorAware} is registered yet, so {@code @CreatedBy} / {@code @LastModifiedBy} keep the values
 * set by the domain until the authenticated user is available (JWT).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
