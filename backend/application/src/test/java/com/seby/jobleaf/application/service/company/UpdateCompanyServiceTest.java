package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.model.company.Company;
import com.seby.jobleaf.domain.model.company.CompanyDetails;
import com.seby.jobleaf.domain.model.user.SystemActor;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.seby.jobleaf.application.service.company.CreateCompanyServiceTest.DETAILS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCompanyServiceTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Company EXISTING = Company.create(ID, new CompanyDetails("Old Name", "/logos/o.png",
            "Retail", "Small", 3.0, "Cluj", 2000, "Old", 5, "https://o.example"), SystemActor.ID);

    @Mock
    private CompanyRepositoryPort companyRepositoryPort;

    @InjectMocks
    private UpdateCompanyService service;

    @Test
    @DisplayName("Should update the details of an existing company, keeping its id")
    void updateCompany_updatesDetails() {
        when(companyRepositoryPort.findByIdWithoutJobs(ID)).thenReturn(Optional.of(EXISTING));
        when(companyRepositoryPort.existsByNameExcluding("DerpCorp", ID)).thenReturn(false);
        when(companyRepositoryPort.updateDetails(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Company updated = service.updateCompany(ID, DETAILS);

        assertThat(updated.id()).isEqualTo(ID);
        assertThat(updated.name()).isEqualTo("DerpCorp");
        assertThat(updated.industry()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("Should refuse renaming to the name of another company")
    void updateCompany_nameOfOtherCompany_conflict() {
        when(companyRepositoryPort.findByIdWithoutJobs(ID)).thenReturn(Optional.of(EXISTING));
        when(companyRepositoryPort.existsByNameExcluding("DerpCorp", ID)).thenReturn(true);

        assertThatThrownBy(() -> service.updateCompany(ID, DETAILS)).isInstanceOf(CompanyAlreadyExistsException.class);
        verify(companyRepositoryPort, never()).updateDetails(any());
    }

    @Test
    @DisplayName("Should answer an unknown id with CompanyNotFoundException")
    void updateCompany_unknownId_notFound() {
        when(companyRepositoryPort.findByIdWithoutJobs(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCompany(ID, DETAILS)).isInstanceOf(CompanyNotFoundException.class);
        verify(companyRepositoryPort, never()).updateDetails(any());
    }
}
