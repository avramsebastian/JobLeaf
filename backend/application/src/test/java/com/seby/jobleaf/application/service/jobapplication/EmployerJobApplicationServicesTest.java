package com.seby.jobleaf.application.service.jobapplication;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.exceptions.job.JobNotFoundException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException;
import com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.EmployerAccessException;
import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.jobapplication.Applicant;
import com.seby.jobleaf.domain.model.jobapplication.ApplicationStatus;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;
import com.seby.jobleaf.domain.exceptions.profile.ProfileNotFoundException;
import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.job.JobRepositoryPort;
import com.seby.jobleaf.domain.port.out.jobapplication.JobApplicationRepositoryPort;
import com.seby.jobleaf.domain.port.out.profile.ProfileRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.seby.jobleaf.application.service.jobapplication.JobApplicationServicesTest.DETAILS;
import static com.seby.jobleaf.application.service.jobapplication.JobApplicationServicesTest.SEEKER;
import static com.seby.jobleaf.application.service.jobapplication.JobApplicationServicesTest.USER_ID;
import static com.seby.jobleaf.application.service.jobapplication.JobApplicationServicesTest.application;
import static com.seby.jobleaf.application.service.jobapplication.JobApplicationServicesTest.openJob;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmployerJobApplicationServicesTest {

    static final UUID EMPLOYER_ID = UUID.randomUUID();
    static final Applicant APPLICANT = new Applicant(USER_ID, "Jane", "jane@example.com", null, DETAILS, true, false);

    static JobLeafUser employerOf(Job job) {
        return new JobLeafUser(EMPLOYER_ID, "Boss", "boss@example.com", null, RoleName.ROLE_EMPLOYER,
                job.companyId(), "Acme", null);
    }

    static ReceivedApplication received(Job job, ApplicationStatus status) {
        return new ReceivedApplication(application(job, status), APPLICANT);
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ListReceived {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobRepositoryPort jobRepositoryPort;
        @Mock
        JobApplicationRepositoryPort jobApplicationRepositoryPort;
        @InjectMocks
        ListReceivedApplicationsService service;

        @Test
        @DisplayName("Should list the applications of a job of the employer's company")
        void lists() {
            var job = openJob();
            var received = received(job, ApplicationStatus.PENDING);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerOf(job)));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));
            when(jobApplicationRepositoryPort.findReceivedByJob(job.id())).thenReturn(List.of(received));

            assertThat(service.listReceivedApplications(EMPLOYER_ID, job.id())).containsExactly(received);
        }

        @Test
        @DisplayName("Should answer 404 for a job of another company, without reading its applications")
        void otherCompany() {
            var job = openJob();
            var employerElsewhere = new JobLeafUser(EMPLOYER_ID, "Boss", "boss@example.com", null,
                    RoleName.ROLE_EMPLOYER, UUID.randomUUID(), "Other", null);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerElsewhere));
            when(jobRepositoryPort.findById(job.id())).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> service.listReceivedApplications(EMPLOYER_ID, job.id()))
                    .isInstanceOf(JobNotFoundException.class);
            verifyNoInteractions(jobApplicationRepositoryPort);
        }

        @Test
        @DisplayName("Should refuse a user who is not an employer")
        void notEmployer() {
            when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(SEEKER));

            assertThatThrownBy(() -> service.listReceivedApplications(USER_ID, UUID.randomUUID()))
                    .isInstanceOf(EmployerAccessException.class);
            verifyNoInteractions(jobRepositoryPort, jobApplicationRepositoryPort);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ReadFiles {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobApplicationRepositoryPort jobApplicationRepositoryPort;
        @Mock
        ProfileRepositoryPort profileRepositoryPort;
        @InjectMocks
        ReadApplicantFilesService service;

        @Test
        @DisplayName("Should return the applicant's resume with its content")
        void readsResume() {
            var job = openJob();
            var received = received(job, ApplicationStatus.REVIEWED);
            UUID id = received.application().id();
            var resume = new ProfileFile("cv.pdf", ProfileFileType.PDF, "%PDF-1.7".getBytes());
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerOf(job)));
            when(jobApplicationRepositoryPort.findReceivedById(id)).thenReturn(Optional.of(received));
            when(profileRepositoryPort.findByUserId(USER_ID, true))
                    .thenReturn(Optional.of(Profile.create(UUID.randomUUID(), USER_ID, DETAILS).withResume(resume)));

            assertThat(service.getApplicantResume(EMPLOYER_ID, id)).isEqualTo(resume);
        }

        @Test
        @DisplayName("Should answer 404 when there is no picture")
        void noPicture() {
            var job = openJob();
            var received = received(job, ApplicationStatus.PENDING);
            UUID id = received.application().id();
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerOf(job)));
            when(jobApplicationRepositoryPort.findReceivedById(id)).thenReturn(Optional.of(received));
            when(profileRepositoryPort.findByUserId(USER_ID, true))
                    .thenReturn(Optional.of(Profile.create(UUID.randomUUID(), USER_ID, DETAILS)));

            assertThatThrownBy(() -> service.getApplicantProfilePicture(EMPLOYER_ID, id))
                    .isInstanceOf(ProfileNotFoundException.class);
        }

        @Test
        @DisplayName("Should answer 404 for a withdrawn application or another company's, without reading the profile")
        void notAllowed() {
            var job = openJob();
            var withdrawn = received(job, ApplicationStatus.WITHDRAWN);
            var elsewhere = received(openJob(), ApplicationStatus.PENDING);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerOf(job)));
            when(jobApplicationRepositoryPort.findReceivedById(withdrawn.application().id()))
                    .thenReturn(Optional.of(withdrawn));
            when(jobApplicationRepositoryPort.findReceivedById(elsewhere.application().id()))
                    .thenReturn(Optional.of(elsewhere));

            assertThatThrownBy(() -> service.getApplicantResume(EMPLOYER_ID, withdrawn.application().id()))
                    .isInstanceOf(JobApplicationNotFoundException.class);
            assertThatThrownBy(() -> service.getApplicantResume(EMPLOYER_ID, elsewhere.application().id()))
                    .isInstanceOf(JobApplicationNotFoundException.class);
            verifyNoInteractions(profileRepositoryPort);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Update {

        @Mock
        UserRepositoryPort userRepositoryPort;
        @Mock
        JobApplicationRepositoryPort jobApplicationRepositoryPort;
        @InjectMocks
        UpdateJobApplicationService service;

        @Test
        @DisplayName("Should save the new status and notes and return them with the applicant")
        void updates() {
            var job = openJob();
            var received = received(job, ApplicationStatus.PENDING);
            UUID id = received.application().id();
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerOf(job)));
            when(jobApplicationRepositoryPort.findReceivedById(id)).thenReturn(Optional.of(received));
            when(jobApplicationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var updated = service.updateJobApplication(EMPLOYER_ID, id, ApplicationStatus.SHORTLISTED, " Strong CV ");

            var saved = ArgumentCaptor.forClass(JobApplication.class);
            verify(jobApplicationRepositoryPort).save(saved.capture());
            assertThat(saved.getValue().status()).isEqualTo(ApplicationStatus.SHORTLISTED);
            assertThat(saved.getValue().notes()).isEqualTo("Strong CV");
            assertThat(updated.applicant()).isEqualTo(APPLICANT);
        }

        @Test
        @DisplayName("Should answer 404 for an application to another company's job, without saving")
        void otherCompany() {
            var job = openJob();
            var received = received(job, ApplicationStatus.PENDING);
            UUID id = received.application().id();
            var employerElsewhere = new JobLeafUser(EMPLOYER_ID, "Boss", "boss@example.com", null,
                    RoleName.ROLE_EMPLOYER, UUID.randomUUID(), "Other", null);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerElsewhere));
            when(jobApplicationRepositoryPort.findReceivedById(id)).thenReturn(Optional.of(received));

            assertThatThrownBy(() -> service.updateJobApplication(EMPLOYER_ID, id, ApplicationStatus.REJECTED, null))
                    .isInstanceOf(JobApplicationNotFoundException.class);
            verify(jobApplicationRepositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should refuse a withdrawn application and the job seeker's statuses")
        void refuses() {
            var job = openJob();
            var withdrawn = received(job, ApplicationStatus.WITHDRAWN);
            var pending = received(job, ApplicationStatus.PENDING);
            when(userRepositoryPort.findById(EMPLOYER_ID)).thenReturn(Optional.of(employerOf(job)));
            when(jobApplicationRepositoryPort.findReceivedById(withdrawn.application().id()))
                    .thenReturn(Optional.of(withdrawn));
            when(jobApplicationRepositoryPort.findReceivedById(pending.application().id()))
                    .thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.updateJobApplication(EMPLOYER_ID, withdrawn.application().id(),
                    ApplicationStatus.REVIEWED, null)).isInstanceOf(JobApplicationConflictException.class);
            assertThatThrownBy(() -> service.updateJobApplication(EMPLOYER_ID, pending.application().id(),
                    ApplicationStatus.WITHDRAWN, null)).isInstanceOf(DomainValidationException.class);
            verify(jobApplicationRepositoryPort, never()).save(any());
        }
    }
}
