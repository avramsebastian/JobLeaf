package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.JobApplicationApi;
import com.seby.jobleaf.apis.generated.model.ApplyJobRequest;
import com.seby.jobleaf.apis.generated.model.JobApplicationResponse;
import com.seby.jobleaf.domain.port.in.jobapplication.ApplyForJobUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.ListMyApplicationsUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.WithdrawApplicationUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.JobApplicationWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter implementing generated {@link JobApplicationApi}: a job seeker's applications.
 * <p>
 * The security filter chain requires ROLE_JOB_SEEKER ({@code jobSeekerPaths}). The user always comes from the token,
 * so nobody can apply, withdraw or read applications for someone else.
 */
@RestController
@RequiredArgsConstructor
public class JobApplicationController implements JobApplicationApi {

    private final ApplyForJobUseCase applyForJobUseCase;
    private final WithdrawApplicationUseCase withdrawApplicationUseCase;
    private final ListMyApplicationsUseCase listMyApplicationsUseCase;
    private final JobApplicationWebMapper jobApplicationWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<JobApplicationResponse>> listMyApplications() {
        var applications = listMyApplicationsUseCase.listMyApplications(AuthenticatedUser.current().id());
        return ResponseEntity.ok(jobApplicationWebMapper.toJobApplicationResponses(applications));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<JobApplicationResponse> applyForJob(ApplyJobRequest applyJobRequest) {
        var application = applyForJobUseCase.applyForJob(AuthenticatedUser.current().id(),
                applyJobRequest.getJobId(), applyJobRequest.getCoverLetter());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobApplicationWebMapper.toJobApplicationResponse(application));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<JobApplicationResponse> withdrawApplication(UUID jobId) {
        var application = withdrawApplicationUseCase.withdrawApplication(AuthenticatedUser.current().id(), jobId);
        return ResponseEntity.ok(jobApplicationWebMapper.toJobApplicationResponse(application));
    }
}
