package com.seby.jobleaf.domain.port.out.auth;

import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.UserAccount;

/**
 * Outbound port that creates a signed access token for a logged-in user.
 */
public interface TokenIssuerPort {

    AuthToken issue(UserAccount user);
}
