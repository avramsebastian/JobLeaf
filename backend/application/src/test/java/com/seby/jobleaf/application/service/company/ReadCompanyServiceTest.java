package com.seby.jobleaf.application.service.company;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.port.out.company.CompanyRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadCompanyServiceTest {

    @Mock
    private CompanyRepositoryPort companyRepositoryPort;

    @InjectMocks
    private ReadCompanyService readCompanyService;

    private Company createSampleCompany(UUID id, String name) {
        return new Company(
                id,
                name,
                "/logos/test.png",
                "Technology",
                "Large",
                4.8,
                "San Francisco,New York",
                2015,
                "Innovative software company",
                500,
                "https://test.example",
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID()
        );
    }

    @Test
    @DisplayName("Should return company when found by id")
    void getCompanyById_whenCompanyExists_returnsCompany() {
        // Given
        UUID companyId = UUID.randomUUID();
        Company expectedCompany = createSampleCompany(companyId, "Acme Corp");
        when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(expectedCompany));

        // When
        Company actualCompany = readCompanyService.getCompanyById(companyId);

        // Then
        assertThat(actualCompany).isNotNull();
        assertThat(actualCompany.id()).isEqualTo(companyId);
        assertThat(actualCompany.name()).isEqualTo("Acme Corp");
        verify(companyRepositoryPort).findById(companyId);
    }

    @Test
    @DisplayName("Should throw CompanyNotFoundException when company does not exist")
    void getCompanyById_whenCompanyNotFound_throwsCompanyNotFoundException() {
        // Given
        UUID companyId = UUID.randomUUID();
        when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> readCompanyService.getCompanyById(companyId))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessage("Company with id " + companyId + " not found");

        verify(companyRepositoryPort).findById(companyId);
    }

    @Test
    @DisplayName("Should return list of all companies")
    void listAllCompanies_returnsListOfCompanies() {
        // Given
        Company company1 = createSampleCompany(UUID.randomUUID(), "Company Alpha");
        Company company2 = createSampleCompany(UUID.randomUUID(), "Company Beta");
        List<Company> expectedCompanies = List.of(company1, company2);
        when(companyRepositoryPort.findAll()).thenReturn(expectedCompanies);

        // When
        List<Company> actualCompanies = readCompanyService.listAllCompanies();

        // Then
        assertThat(actualCompanies).hasSize(2);
        assertThat(actualCompanies).containsExactly(company1, company2);
        verify(companyRepositoryPort).findAll();
    }
}
