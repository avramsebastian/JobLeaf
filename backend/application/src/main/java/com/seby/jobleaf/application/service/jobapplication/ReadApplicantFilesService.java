package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException;
import com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.port.in.jobapplication.ReadApplicantFilesUseCase;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Application service implementing {@link ReadApplicantFilesUseCase}.
 * <p>
 * An employer only sees the files of someone who applied to their company, and only while the application is not
 * withdrawn: applying is what shares the profile with the company.
 */
@Service
@RequiredArgsConstructor
public class ReadApplicantFilesService implements ReadApplicantFilesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReadApplicantFilesService.class);

    private final UserRepositoryPort userRepositoryPort;
    private final JobApplicationRepositoryPort jobApplicationRepositoryPort;
    private final ProfileRepositoryPort profileRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileFile getApplicantProfilePicture(UUID employerId, UUID applicationId) {
        return loadFile(employerId, applicationId, Profile::profilePicture, "profile picture");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileFile getApplicantResume(UUID employerId, UUID applicationId) {
        return loadFile(employerId, applicationId, Profile::resume, "resume");
    }

    private ProfileFile loadFile(UUID employerId, UUID applicationId, Function<Profile, ProfileFile> file,
                                 String description) {
        Objects.requireNonNull(employerId, "employerId");
        Objects.requireNonNull(applicationId, "applicationId");
        UUID companyId = userRepositoryPort.findById(employerId)
                .orElseThrow(() -> UserNotFoundException.byId(employerId))
                .requireEmployerCompany();

        // Another company's application and a withdrawn one both answer 404
        ReceivedApplication received = jobApplicationRepositoryPort.findReceivedById(applicationId)
                .filter(found -> found.application().job().belongsTo(companyId))
                .filter(found -> !found.application().isWithdrawn())
                .orElseThrow(() -> {
                    log.warn("Employer {} tried to read the {} of application {}, which is not open for company {}",
                            employerId, description, applicationId, companyId);
                    return JobApplicationNotFoundException.byId(applicationId);
                });

        UUID applicantId = received.applicant().userId();
        var found = profileRepositoryPort.findByUserId(applicantId, true)
                .map(file)
                .filter(ProfileFile::hasContent)
                .orElseThrow(() -> ProfileNotFoundException.noFile(description));
        log.info("Employer {} read the {} of user {} (application {}, {} bytes)", employerId, description,
                applicantId, applicationId, found.size());
        return found;
    }
}
