package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.RoleEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobLeafUserPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter that implements {@link UserRepositoryPort} using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class UserRepositoryAdapter implements UserRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryAdapter.class);

    private final JobLeafUserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final JobLeafUserPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<JobLeafUser> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userJpaRepository.findWithRoleByEmail(normalizeEmail(email)).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(normalizeEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMobileNumber(String mobileNumber) {
        return userJpaRepository.existsByMobileNumber(mobileNumber);
    }

    @Override
    public JobLeafUser create(NewUser user, String passwordHash) {
        RoleEntity role = roleJpaRepository.findByName(user.role())
                .orElseThrow(() -> new IllegalStateException("Role " + user.role() + " is missing in the roles table"));

        var entity = new JobLeafUserEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(user.name());
        entity.setEmail(normalizeEmail(user.email()));
        entity.setMobileNumber(user.mobileNumber());
        entity.setPasswordHash(passwordHash);
        entity.setRole(role);

        JobLeafUserEntity saved = userJpaRepository.save(entity);
        log.debug("Saved user with id {}", saved.getId());
        return mapper.toDomain(saved);
    }

    /**
     * Emails are stored lower-case, which makes the unique constraint case-insensitive.
     */
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
