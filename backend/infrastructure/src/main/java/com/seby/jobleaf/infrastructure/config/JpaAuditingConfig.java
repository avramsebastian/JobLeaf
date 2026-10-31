package com.seby.jobleaf.infrastructure.config;

import com.seby.jobleaf.infrastructure.persistence.audit.CurrentActor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * Enables Spring Data JPA auditing: {@code @CreatedDate}/{@code @LastModifiedDate} come from the clock and
 * {@code @CreatedBy}/{@code @LastModifiedBy} from {@link CurrentActor}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> Optional.of(CurrentActor.id());
    }
}
