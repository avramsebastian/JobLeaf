package com.seby.jobleaf.infrastructure.persistence.audit;

import com.seby.jobleaf.domain.model.SystemActor;
import lombok.experimental.UtilityClass;

import java.util.UUID;

/**
 * Single source of "who is making this change" for JPA auditing ({@code created_by}/{@code updated_by})
 * and Envers revisions ({@code revinfo.user_id}).
 */
@UtilityClass
public class CurrentActor {

    /**
     * @return the id of the acting user; always the {@link SystemActor} until authentication exists
     */
    public static UUID id() {
        // TODO: resolve the authenticated user from the security context once JWT is added
        return SystemActor.ID;
    }
}
