package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.EmployerJobApplicationApi;
import com.seby.jobleaf.apis.generated.model.ReceivedApplicationResponse;
import com.seby.jobleaf.apis.generated.model.UpdateJobApplicationRequest;
import com.seby.jobleaf.domain.port.in.jobapplication.ListReceivedApplicationsUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.ReadApplicantFilesUseCase;
import com.seby.jobleaf.domain.port.in.jobapplication.UpdateJobApplicationUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.ProfileWebMapper;
import com.seby.jobleaf.infrastructure.web.mapper.ReceivedApplicationWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter implementing generated {@link EmployerJobApplicationApi}: the applications an employer received.
 * <p>
 * The security filter chain requires ROLE_EMPLOYER ({@code employerPaths}); the services then check that the job
 * belongs to the employer's own company, so nobody reads or changes another company's applications.
 */
@RestController
@RequiredArgsConstructor
public class EmployerJobApplicationController implements EmployerJobApplicationApi {

    private final ListReceivedApplicationsUseCase listReceivedApplicationsUseCase;
    private final UpdateJobApplicationUseCase updateJobApplicationUseCase;
    private final ReadApplicantFilesUseCase readApplicantFilesUseCase;
    private final ReceivedApplicationWebMapper receivedApplicationWebMapper;
    private final ProfileWebMapper profileWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<ReceivedApplicationResponse>> getApplicationsByJobForEmployer(UUID jobId) {
        var applications = listReceivedApplicationsUseCase.listReceivedApplications(AuthenticatedUser.current().id(),
                jobId);
        return ResponseEntity.ok(receivedApplicationWebMapper.toReceivedApplicationResponses(applications));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<ReceivedApplicationResponse> updateJobApplication(UUID applicationId,
                                                                            UpdateJobApplicationRequest request) {
        var updated = updateJobApplicationUseCase.updateJobApplication(AuthenticatedUser.current().id(), applicationId,
                receivedApplicationWebMapper.toApplicationStatus(request), request.getNotes());
        return ResponseEntity.ok(receivedApplicationWebMapper.toReceivedApplicationResponse(updated));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Resource> getApplicantProfilePicture(UUID applicationId) {
        var picture = readApplicantFilesUseCase.getApplicantProfilePicture(AuthenticatedUser.current().id(),
                applicationId);
        return profileWebMapper.toFileResponse(picture, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Resource> getApplicantResume(UUID applicationId) {
        var resume = readApplicantFilesUseCase.getApplicantResume(AuthenticatedUser.current().id(), applicationId);
        return profileWebMapper.toFileResponse(resume, true);
    }
}
