package com.seby.jobleaf.application.service.user;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.port.in.user.SearchUserUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Application service implementing {@link SearchUserUseCase}.
 */
@Service
@RequiredArgsConstructor
public class SearchUserService implements SearchUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(SearchUserService.class);

    private final UserRepositoryPort userRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public JobLeafUser searchByEmail(String email) {
        Objects.requireNonNull(email, "email");
        var user = userRepositoryPort.findByEmail(email).orElseThrow(() -> {
            log.info("No user found for the searched email");
            return UserNotFoundException.byEmail(email.trim());
        });
        log.info("Found user {} with role {}", user.id(), user.role());
        return user;
    }
}
