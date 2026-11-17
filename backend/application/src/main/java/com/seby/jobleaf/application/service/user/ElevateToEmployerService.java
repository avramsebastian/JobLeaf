package com.seby.jobleaf.application.service.user;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.in.user.ElevateToEmployerUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link ElevateToEmployerUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ElevateToEmployerService implements ElevateToEmployerUseCase {

    private static final Logger log = LoggerFactory.getLogger(ElevateToEmployerService.class);

    private final UserRepositoryPort userRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public JobLeafUser elevateToEmployer(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        var user = userRepositoryPort.findById(userId).orElseThrow(() -> {
            log.warn("Cannot elevate user {}: not found", userId);
            return UserNotFoundException.byId(userId);
        });

        user.checkCanBecomeEmployer();
        if (user.isEmployer()) {
            log.info("User {} already is an employer, nothing to change", userId);
            return user;
        }

        var elevated = userRepositoryPort.updateRole(userId, RoleName.ROLE_EMPLOYER);
        log.info("Elevated user {} from {} to {}", userId, user.role(), elevated.role());
        return elevated;
    }
}
