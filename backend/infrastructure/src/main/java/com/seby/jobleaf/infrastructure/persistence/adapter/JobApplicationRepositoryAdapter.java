package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.JobApplicationEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobApplicationPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.JobApplicationJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.JobJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.ProfileJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.ProfileMetadataRow;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Secondary adapter that implements {@link JobApplicationRepositoryPort} using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class JobApplicationRepositoryAdapter implements JobApplicationRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JobApplicationRepositoryAdapter.class);

    private final JobApplicationJpaRepository jobApplicationJpaRepository;
    private final JobLeafUserJpaRepository userJpaRepository;
    private final JobJpaRepository jobJpaRepository;
    private final ProfileJpaRepository profileJpaRepository;
    private final JobApplicationPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<JobApplication> findByUserAndJob(UUID userId, UUID jobId) {
        return jobApplicationJpaRepository.findByUserAndJob(userId, jobId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplication> findAllByUser(UUID userId) {
        return jobApplicationJpaRepository.findAllByUser(userId).stream().map(mapper::toDomain).toList();
    }

    /**
     * Two queries in total, however many applications: the applications with job and user, then the profiles of all
     * those users at once (without the files).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReceivedApplication> findReceivedByJob(UUID jobId) {
        List<JobApplicationEntity> entities =
                jobApplicationJpaRepository.findAllByJobExcept(jobId, ApplicationStatus.WITHDRAWN);
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<UUID> userIds = entities.stream().map(entity -> entity.getUser().getId()).collect(Collectors.toSet());
        Map<UUID, ProfileMetadataRow> profilesByUser = profileJpaRepository.findMetadataByUserIds(userIds).stream()
                .collect(Collectors.toMap(ProfileMetadataRow::userId, Function.identity()));
        return entities.stream()
                .map(entity -> toReceived(entity, profilesByUser.get(entity.getUser().getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReceivedApplication> findReceivedById(UUID applicationId) {
        return jobApplicationJpaRepository.findWithJobAndUserById(applicationId)
                .map(entity -> toReceived(entity,
                        profileJpaRepository.findMetadataByUserId(entity.getUser().getId()).orElse(null)));
    }

    private ReceivedApplication toReceived(JobApplicationEntity entity, ProfileMetadataRow profile) {
        return new ReceivedApplication(mapper.toDomain(entity), mapper.toApplicant(entity.getUser(), profile));
    }

    /**
     * Updates the stored row when it exists (re-apply, withdraw), otherwise creates it with references to the user and
     * the job (getReferenceById: only their ids are needed for the foreign keys). saveAndFlush: the SQL runs now, so
     * auditing fills in the timestamps and the unique key (user, job) is checked here.
     */
    @Override
    public JobApplication save(JobApplication application) {
        JobApplicationEntity entity = jobApplicationJpaRepository.findById(application.id()).orElseGet(() -> {
            var created = new JobApplicationEntity();
            created.setId(application.id());
            created.setUser(userJpaRepository.getReferenceById(application.userId()));
            created.setJob(jobJpaRepository.getReferenceById(application.job().id()));
            created.restoreAuditFields(null, application.createdBy(), null, application.updatedBy());
            return created;
        });
        mapper.applyTo(application, entity);
        JobApplicationEntity saved = jobApplicationJpaRepository.saveAndFlush(entity);
        log.debug("Saved application {} with status {}", saved.getId(), saved.getStatus());
        return mapper.toDomain(saved);
    }
}
