package com.seby.jobleaf.application.service.user;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserRoleConflictException;
import com.seby.jobleaf.domain.model.company.Company;
import com.seby.jobleaf.domain.model.company.CompanyDetails;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.model.user.SystemActor;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignCompanyToEmployerServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final Company COMPANY = Company.create(COMPANY_ID, new CompanyDetails("DerpCorp", "/l.png",
            "Technology", "Large", 4.0, "Bucharest", 2010, "A company", 10, "https://d.example"), SystemActor.ID);

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private CompanyRepositoryPort companyRepositoryPort;

    @InjectMocks
    private AssignCompanyToEmployerService service;

    private static JobLeafUser user(RoleName role, UUID companyId, String companyName) {
        return new JobLeafUser(USER_ID, "Jane", "jane@example.com", null, role, companyId, companyName, null);
    }

    @Test
    @DisplayName("Should assign an existing company to an employer")
    void employer_assigned() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(user(RoleName.ROLE_EMPLOYER, null, null)));
        when(companyRepositoryPort.findByIdWithoutJobs(COMPANY_ID)).thenReturn(Optional.of(COMPANY));
        when(userRepositoryPort.assignCompany(USER_ID, COMPANY_ID))
                .thenReturn(user(RoleName.ROLE_EMPLOYER, COMPANY_ID, "DerpCorp"));

        var result = service.assignCompany(USER_ID, COMPANY_ID);

        assertThat(result.companyId()).isEqualTo(COMPANY_ID);
        assertThat(result.companyName()).isEqualTo("DerpCorp");
    }

    @Test
    @DisplayName("Should refuse a user who is not an employer, before looking at the company")
    void jobSeeker_conflict() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(user(RoleName.ROLE_JOB_SEEKER, null, null)));

        assertThatThrownBy(() -> service.assignCompany(USER_ID, COMPANY_ID))
                .isInstanceOf(UserRoleConflictException.class);
        verify(userRepositoryPort, never()).assignCompany(any(), any());
    }

    @Test
    @DisplayName("Should throw CompanyNotFoundException for an unknown company")
    void unknownCompany_notFound() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(user(RoleName.ROLE_EMPLOYER, null, null)));
        when(companyRepositoryPort.findByIdWithoutJobs(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignCompany(USER_ID, COMPANY_ID))
                .isInstanceOf(CompanyNotFoundException.class);
        verify(userRepositoryPort, never()).assignCompany(any(), any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException for an unknown user")
    void unknownUser_notFound() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignCompany(USER_ID, COMPANY_ID))
                .isInstanceOf(UserNotFoundException.class);
    }
}
