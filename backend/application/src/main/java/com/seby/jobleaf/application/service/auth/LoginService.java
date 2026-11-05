package com.seby.jobleaf.application.service.auth;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.UserAccount;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase;
import com.seby.jobleaf.domain.port.out.auth.CredentialsVerifierPort;
import com.seby.jobleaf.domain.port.out.auth.TokenIssuerPort;
import com.seby.jobleaf.domain.port.out.auth.UserAccountPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Application service implementing {@link LoginUseCase}: check the credentials, load the account, issue a token.
 */
@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final CredentialsVerifierPort credentialsVerifierPort;
    private final UserAccountPort userAccountPort;
    private final TokenIssuerPort tokenIssuerPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginResult login(LoginCommand command) {
        Objects.requireNonNull(command, "command");

        try {
            credentialsVerifierPort.verify(command.email(), command.password());
        } catch (InvalidCredentialsException exception) {
            log.warn("Failed login attempt for {}", command.email());
            throw exception;
        }

        UserAccount user = userAccountPort.findByEmail(command.email())
                .orElseThrow(() -> {
                    log.error("Credentials accepted for {} but no user account exists", command.email());
                    return new InvalidCredentialsException();
                });

        AuthToken token = tokenIssuerPort.issue(user);
        log.info("User {} logged in with role {}", user.id(), user.role());
        return new LoginResult(user, token);
    }
}
