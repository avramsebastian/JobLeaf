package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.SavedJobApi;
import com.seby.jobleaf.apis.generated.model.JobResponse;
import com.seby.jobleaf.apis.generated.model.UnsaveJobResponse;
import com.seby.jobleaf.domain.port.in.savedjob.ListSavedJobsUseCase;
import com.seby.jobleaf.domain.port.in.savedjob.SaveJobUseCase;
import com.seby.jobleaf.domain.port.in.savedjob.UnsaveJobUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.JobWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter implementing generated {@link SavedJobApi}: a job seeker's saved jobs.
 * <p>
 * The security filter chain requires ROLE_JOB_SEEKER ({@code jobSeekerPaths}). The user always comes from the token,
 * so nobody can see or change someone else's saved jobs.
 */
@RestController
@RequiredArgsConstructor
public class SavedJobController implements SavedJobApi {

    private final SaveJobUseCase saveJobUseCase;
    private final UnsaveJobUseCase unsaveJobUseCase;
    private final ListSavedJobsUseCase listSavedJobsUseCase;
    private final JobWebMapper jobWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<JobResponse>> listSavedJobs() {
        var jobs = listSavedJobsUseCase.listSavedJobs(AuthenticatedUser.current().id());
        return ResponseEntity.ok(jobWebMapper.toListJobsResponse(jobs));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<JobResponse> saveJob(UUID jobId) {
        var job = saveJobUseCase.saveJob(AuthenticatedUser.current().id(), jobId);
        return ResponseEntity.ok(jobWebMapper.toJobResponse(job));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<UnsaveJobResponse> unsaveJob(UUID jobId) {
        unsaveJobUseCase.unsaveJob(AuthenticatedUser.current().id(), jobId);
        return ResponseEntity.ok(jobWebMapper.toUnsaveJobResponse());
    }
}
