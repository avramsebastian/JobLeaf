package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.infrastructure.persistence.entity.JobEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.JobJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The adapter writes the join table through the owning side of the many-to-many: the user's savedJobs set.
 */
@ExtendWith(MockitoExtension.class)
class SavedJobRepositoryAdapterTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private JobLeafUserJpaRepository userJpaRepository;

    @Mock
    private JobJpaRepository jobJpaRepository;

    @Mock
    private JobPersistenceMapper jobMapper;

    @InjectMocks
    private SavedJobRepositoryAdapter adapter;

    private static JobEntity job(UUID id) {
        var job = new JobEntity();
        job.setId(id);
        return job;
    }

    @Test
    @DisplayName("Should add the job to the user's saved jobs (Hibernate inserts the saved_jobs row)")
    void save_addsToOwningSide() {
        var user = new JobLeafUserEntity();
        var job = job(JOB_ID);
        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jobJpaRepository.getReferenceById(JOB_ID)).thenReturn(job);

        adapter.save(USER_ID, JOB_ID);

        assertThat(user.getSavedJobs()).containsExactly(job);
    }

    @Test
    @DisplayName("Should remove only that job from the user's saved jobs (Hibernate deletes the row)")
    void remove_removesFromOwningSide() {
        var user = new JobLeafUserEntity();
        var other = job(UUID.randomUUID());
        user.getSavedJobs().add(job(JOB_ID));
        user.getSavedJobs().add(other);
        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.remove(USER_ID, JOB_ID);

        assertThat(user.getSavedJobs()).containsExactly(other);
    }

    @Test
    @DisplayName("Should ask the database whether the job is saved")
    void isSaved_queries() {
        when(jobJpaRepository.isSavedByUser(USER_ID, JOB_ID)).thenReturn(true);

        assertThat(adapter.isSaved(USER_ID, JOB_ID)).isTrue();
    }
}
