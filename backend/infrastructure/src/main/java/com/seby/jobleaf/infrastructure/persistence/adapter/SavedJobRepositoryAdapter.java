package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.port.out.savedjob.SavedJobRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.JobJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Secondary adapter that implements {@link SavedJobRepositoryPort} with the {@code @ManyToMany} between users and
 * jobs. Writes go through the owning side ({@link JobLeafUserEntity#getSavedJobs()}): Hibernate turns adding to or
 * removing from that set into an INSERT or DELETE on {@code saved_jobs} when the transaction is flushed.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class SavedJobRepositoryAdapter implements SavedJobRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(SavedJobRepositoryAdapter.class);

    private final JobLeafUserJpaRepository userJpaRepository;
    private final JobJpaRepository jobJpaRepository;
    private final JobPersistenceMapper jobMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean isSaved(UUID userId, UUID jobId) {
        return jobJpaRepository.isSavedByUser(userId, jobId);
    }

    /**
     * getReferenceById: only the job's id is needed for the join row, so no SELECT of the job runs.
     */
    @Override
    public void save(UUID userId, UUID jobId) {
        JobLeafUserEntity user = loadUser(userId);
        user.getSavedJobs().add(jobJpaRepository.getReferenceById(jobId));
        log.debug("Saved job {} for user {}", jobId, userId);
    }

    @Override
    public void remove(UUID userId, UUID jobId) {
        JobLeafUserEntity user = loadUser(userId);
        user.getSavedJobs().removeIf(job -> job.getId().equals(jobId));
        log.debug("Removed saved job {} of user {}", jobId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findSavedJobs(UUID userId) {
        return jobJpaRepository.findSavedByUserId(userId).stream().map(jobMapper::toDomain).toList();
    }

    /**
     * The services check the user first, so a missing user here means it was deleted in between.
     */
    private JobLeafUserEntity loadUser(UUID userId) {
        return userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User " + userId + " disappeared during the update"));
    }
}
