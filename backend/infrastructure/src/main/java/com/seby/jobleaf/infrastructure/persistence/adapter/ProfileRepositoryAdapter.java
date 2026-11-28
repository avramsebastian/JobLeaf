package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.ProfileEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.ProfilePersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.ProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter that implements {@link ProfileRepositoryPort} using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class ProfileRepositoryAdapter implements ProfileRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(ProfileRepositoryAdapter.class);

    private final ProfileJpaRepository profileJpaRepository;
    private final JobLeafUserJpaRepository userJpaRepository;
    private final ProfilePersistenceMapper mapper;

    /**
     * Without binary data a query that does not select the file columns is used, so the database does not even read
     * them; with binary data the whole row is loaded.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Profile> findByUserId(UUID userId, boolean includeBinaryData) {
        if (!includeBinaryData) {
            return profileJpaRepository.findMetadataByUserId(userId).map(mapper::toDomain);
        }
        return profileJpaRepository.findByUser_Id(userId).map(entity -> mapper.toDomain(entity, true));
    }

    /**
     * Updates the stored row when there is one (files without new content stay as they are), otherwise creates it.
     * saveAndFlush: the SQL runs now, so auditing fills in the timestamps before the result is mapped.
     */
    @Override
    public Profile save(Profile profile) {
        ProfileEntity entity = profileJpaRepository.findByUser_Id(profile.userId()).orElseGet(() -> {
            var created = new ProfileEntity();
            created.setId(profile.id());
            // Only the foreign key is needed: a reference, no SELECT of the user
            created.setUser(userJpaRepository.getReferenceById(profile.userId()));
            created.restoreAuditFields(null, profile.createdBy(), null, profile.updatedBy());
            return created;
        });
        mapper.applyTo(profile, entity);
        ProfileEntity saved = profileJpaRepository.saveAndFlush(entity);
        log.debug("Saved profile {} of user {}", saved.getId(), profile.userId());
        return mapper.toDomain(saved, false);
    }
}
