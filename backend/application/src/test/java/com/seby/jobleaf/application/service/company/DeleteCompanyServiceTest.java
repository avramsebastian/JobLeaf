package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.model.company.Company;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCompanyServiceTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private CompanyRepositoryPort companyRepositoryPort;

    @InjectMocks
    private DeleteCompanyService service;

    @Test
    @DisplayName("Should delete an existing company")
    void deleteCompany_deletes() {
        when(companyRepositoryPort.findByIdWithoutJobs(ID)).thenReturn(Optional.of(Company.create(ID, DETAILS, SystemActor.ID)));

        service.deleteCompany(ID);

        verify(companyRepositoryPort).deleteById(ID);
    }

    @Test
    @DisplayName("Should answer an unknown id with CompanyNotFoundException and delete nothing")
    void deleteCompany_unknownId_notFound() {
        when(companyRepositoryPort.findByIdWithoutJobs(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCompany(ID)).isInstanceOf(CompanyNotFoundException.class);
        verify(companyRepositoryPort, never()).deleteById(any());
    }
}
