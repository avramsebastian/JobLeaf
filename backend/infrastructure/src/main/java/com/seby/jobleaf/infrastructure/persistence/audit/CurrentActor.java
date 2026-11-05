package com.seby.jobleaf.infrastructure.persistence.audit;

import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Single source of "who is making this change" for JPA auditing ({@code created_by}/{@code updated_by})
 * and Envers revisions ({@code revinfo.user_id}).
 */
@UtilityClass
public class CurrentActor {

    /**
     * @return the id of the user logged in with a JWT, or the {@link SystemActor} for anonymous requests
     * (e.g. the public contact form) and work outside a request
     */
    public static UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.id();
        }
        return SystemActor.ID;
    }
}
