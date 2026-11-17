package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.RoleEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobLeafUserPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.RoleJpaRepository;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.infrastructure.persistence.entity.CompanyEntity;
import com.seby.jobleaf.infrastructure.persistence.repository.CompanyJpaRepository;
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
    private final CompanyJpaRepository companyJpaRepository;
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
    @Override
    @Transactional(readOnly = true)
    public Optional<JobLeafUser> findById(UUID id) {
        return userJpaRepository.findWithRoleById(id).map(mapper::toDomain);
    }

    /**
     * Changes the managed entity, so only the role column is updated (and audited by Envers).
     * saveAndFlush: the UPDATE runs now, so a database error surfaces here and not at commit.
     */
    @Override
    public JobLeafUser updateRole(UUID userId, RoleName roleName) {
        JobLeafUserEntity user = loadUser(userId);
        RoleEntity role = roleJpaRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " is missing in the roles table"));
        user.setRole(role);
        JobLeafUserEntity saved = userJpaRepository.saveAndFlush(user);
        log.debug("Changed role of user {} to {}", userId, roleName);
        return mapper.toDomain(saved);
    }

    @Override
    public JobLeafUser assignCompany(UUID userId, UUID companyId) {
        JobLeafUserEntity user = loadUser(userId);
        CompanyEntity company = companyJpaRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException("Company " + companyId + " disappeared during assignment"));
        user.setCompany(company);
        JobLeafUserEntity saved = userJpaRepository.saveAndFlush(user);
        log.debug("Assigned company {} to user {}", companyId, userId);
        return mapper.toDomain(saved);
    }

    /**
     * The services check that the user exists first, so a missing user here means it was deleted in between.
     */
    private JobLeafUserEntity loadUser(UUID userId) {
        return userJpaRepository.findWithRoleById(userId)
                .orElseThrow(() -> new IllegalStateException("User " + userId + " disappeared during the update"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
