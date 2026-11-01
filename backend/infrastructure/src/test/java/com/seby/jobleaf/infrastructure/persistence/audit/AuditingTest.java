package com.seby.jobleaf.infrastructure.persistence.audit;

import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.infrastructure.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditingTest {

    @Test
    @DisplayName("Should attribute changes to the system actor until authentication exists")
    void currentActor_isSystemActor() {
        assertThat(CurrentActor.id()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(CurrentActor.id()).isEqualTo(SystemActor.ID);
    }

    @Test
    @DisplayName("Should provide the current actor to Spring Data auditing")
    void auditorAware_returnsCurrentActor() {
        assertThat(new JpaAuditingConfig().auditorAware().getCurrentAuditor()).contains(CurrentActor.id());
    }

    @Test
    @DisplayName("Should stamp every new Envers revision with the current actor")
    void revisionListener_setsUserId() {
        // Given
        AuditRevisionEntity revision = new AuditRevisionEntity();

        // When
        new ActorRevisionListener().newRevision(revision);

        // Then
        assertThat(revision.getUserId()).isEqualTo(CurrentActor.id());
    }
}
