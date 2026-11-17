package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException;
import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.model.CompanyDetails;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCompanyServiceTest {

    static final CompanyDetails DETAILS = new CompanyDetails("DerpCorp", "/logos/d.png", "Technology",
            "Large", 4.5, "Bucharest", 2010, "A company", 100, "https://d.example");

    @Mock
    private CompanyRepositoryPort companyRepositoryPort;

    @InjectMocks
    private CreateCompanyService service;

    @Test
    @DisplayName("Should create a company with a new id and no jobs")
    void createCompany_storesNewCompany() {
        when(companyRepositoryPort.existsByName("DerpCorp")).thenReturn(false);
        when(companyRepositoryPort.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Company created = service.createCompany(DETAILS);

        var stored = ArgumentCaptor.forClass(Company.class);
        verify(companyRepositoryPort).create(stored.capture());
        assertThat(stored.getValue().id()).isNotNull();
        assertThat(stored.getValue().jobs()).isEmpty();
        assertThat(created.name()).isEqualTo("DerpCorp");
    }

    @Test
    @DisplayName("Should refuse to create a company whose name is taken")
    void createCompany_duplicateName_conflict() {
        when(companyRepositoryPort.existsByName("DerpCorp")).thenReturn(true);

        assertThatThrownBy(() -> service.createCompany(DETAILS)).isInstanceOf(CompanyAlreadyExistsException.class);
        verify(companyRepositoryPort, never()).create(any());
    }
}
