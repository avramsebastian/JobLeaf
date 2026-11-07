package com.seby.jobleaf.application.service.auth;

import com.seby.jobleaf.domain.exceptions.auth.UserAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase;
import com.seby.jobleaf.domain.port.out.auth.PasswordBreachCheckerPort;
import com.seby.jobleaf.domain.port.out.auth.PasswordHasherPort;
import com.seby.jobleaf.domain.port.out.auth.TokenIssuerPort;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort.NewUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Application service implementing {@link RegisterUseCase}: check the email and mobile number are free, reject
 * passwords known from data breaches, store the user with a hashed password and log them in.
 * <p>
 * The role is assigned by the system ({@link RoleName#DEFAULT_FOR_NEW_USERS}), never taken from the request.
 */
@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterService.class);

    static final String COMPROMISED_PASSWORD_MESSAGE =
            "This password has appeared in a data breach and is not safe to use, please choose another one";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordBreachCheckerPort passwordBreachCheckerPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenIssuerPort tokenIssuerPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResult register(RegisterCommand command) {
        Objects.requireNonNull(command, "command");

        if (userRepositoryPort.existsByEmail(command.email())) {
            log.info("Registration rejected: email already registered");
            throw UserAlreadyExistsException.email();
        }
        if (userRepositoryPort.existsByMobileNumber(command.mobileNumber())) {
            log.info("Registration rejected: mobile number already registered");
            throw UserAlreadyExistsException.mobileNumber();
        }
        if (passwordBreachCheckerPort.isCompromised(command.password())) {
            log.info("Registration rejected: password found in data breaches");
            throw DomainValidationException.invalidField("password", COMPROMISED_PASSWORD_MESSAGE);
        }

        String passwordHash = passwordHasherPort.hash(command.password());
        JobLeafUser user = userRepositoryPort.create(
                new NewUser(command.name(), command.email(), command.mobileNumber(), RoleName.DEFAULT_FOR_NEW_USERS),
                passwordHash);

        AuthToken token = tokenIssuerPort.issue(user);
        log.info("Registered user {} with role {}", user.id(), user.role());
        return new LoginResult(user, token);
    }
}
