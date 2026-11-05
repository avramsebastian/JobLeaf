package com.seby.jobleaf.infrastructure.persistence.audit;

import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.infrastructure.config.JpaAuditingConfig;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditingTest {

    private static final UUID USER_ID = UUID.fromString("6f1c2b7e-3d4a-4e5f-9a8b-1c2d3e4f5a02");

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void logIn() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(USER_ID, "admin@example.com"), null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Should attribute changes to the logged-in user")
    void currentActor_whenLoggedIn_isUserId() {
        logIn();

        assertThat(CurrentActor.id()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should attribute changes to the system actor when nobody is logged in")
    void currentActor_whenNoAuthentication_isSystemActor() {
        assertThat(CurrentActor.id()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(CurrentActor.id()).isEqualTo(SystemActor.ID);
    }

    @Test
    @DisplayName("Should attribute anonymous requests (e.g. the public contact form) to the system actor")
    void currentActor_whenAnonymous_isSystemActor() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(CurrentActor.id()).isEqualTo(SystemActor.ID);
    }

    @Test
    @DisplayName("Should provide the current actor to Spring Data auditing")
    void auditorAware_returnsCurrentActor() {
        logIn();

        assertThat(new JpaAuditingConfig().auditorAware().getCurrentAuditor()).contains(USER_ID);
    }

    @Test
    @DisplayName("Should stamp every new Envers revision with the current actor")
    void revisionListener_setsUserId() {
        // Given
        logIn();
        AuditRevisionEntity revision = new AuditRevisionEntity();

        // When
        new ActorRevisionListener().newRevision(revision);

        // Then
        assertThat(revision.getUserId()).isEqualTo(USER_ID);
    }
}
