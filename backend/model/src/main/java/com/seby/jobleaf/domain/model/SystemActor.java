package com.seby.jobleaf.domain.model;

import lombok.experimental.UtilityClass;

import java.util.UUID;

/**
 * The built-in "system" user that owns seeded data and anonymous actions.
 * <p>
 * It will be the first row of the users table; until authentication exists every change is attributed to it.
 */
@UtilityClass
public class SystemActor {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
}
