package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.JobApi;
import com.seby.jobleaf.apis.generated.model.CreateJobRequest;
import com.seby.jobleaf.apis.generated.model.JobResponse;
import com.seby.jobleaf.apis.generated.model.UpdateJobStatusRequest;
import com.seby.jobleaf.domain.port.in.job.CreateJobUseCase;
import com.seby.jobleaf.domain.port.in.job.ListEmployerJobsUseCase;
import com.seby.jobleaf.domain.port.in.job.UpdateJobStatusUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.JobWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter implementing generated {@link JobApi}: an employer managing the jobs of their company.
 * <p>
 * The security filter chain requires ROLE_EMPLOYER ({@code employerPaths}). Who the employer is comes from the
 * token ({@link AuthenticatedUser#current()}), never from the request; the services then check the stored user.
 */
@RestController
@RequiredArgsConstructor
public class JobController implements JobApi {

    private final ListEmployerJobsUseCase listEmployerJobsUseCase;
    private final CreateJobUseCase createJobUseCase;
    private final UpdateJobStatusUseCase updateJobStatusUseCase;
    private final JobWebMapper jobWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<JobResponse>> listEmployerJobs() {
        var jobs = listEmployerJobsUseCase.listJobs(AuthenticatedUser.current().id());
        return ResponseEntity.ok(jobWebMapper.toListJobsResponse(jobs));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<JobResponse> createJob(CreateJobRequest createJobRequest) {
        var job = createJobUseCase.createJob(AuthenticatedUser.current().id(),
                jobWebMapper.toJobDetails(createJobRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(jobWebMapper.toJobResponse(job));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<JobResponse> updateJobStatus(UUID jobId, UpdateJobStatusRequest updateJobStatusRequest) {
        var job = updateJobStatusUseCase.updateStatus(AuthenticatedUser.current().id(), jobId,
                jobWebMapper.toJobStatus(updateJobStatusRequest));
        return ResponseEntity.ok(jobWebMapper.toJobResponse(job));
    }
}
