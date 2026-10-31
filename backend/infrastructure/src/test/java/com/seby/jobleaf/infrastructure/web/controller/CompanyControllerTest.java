package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.model.CompanyResponse;
import com.seby.jobleaf.domain.model.Company;
import com.seby.jobleaf.domain.port.in.company.ReadCompanyUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.CompanyWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private ReadCompanyUseCase readCompanyUseCase;

    @Mock
    private CompanyWebMapper companyWebMapper;

    @InjectMocks
    private CompanyController companyController;

    private Company createSampleCompany(UUID id, String name) {
        return new Company(
                id,
                name,
                "/logos/test.png",
                "Technology",
                "Medium",
                4.5,
                "San Francisco",
                2018,
                "Innovative tech startup",
                200,
                "https://test.example",
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                List.of()
        );
    }

    @Test
    @DisplayName("Should return 200 OK with list of companies mapped to CompanyResponse")
    void listCompanies_returnsOkWithList() {
        // Given
        Company company = createSampleCompany(UUID.randomUUID(), "TechCorp");
        List<Company> domainCompanies = List.of(company);

        CompanyResponse responseDto = new CompanyResponse();
        responseDto.setId(company.id());
        responseDto.setName("TechCorp");
        List<CompanyResponse> expectedDtos = List.of(responseDto);

        when(readCompanyUseCase.listAllCompanies()).thenReturn(domainCompanies);
        when(companyWebMapper.toListCompaniesResponse(domainCompanies)).thenReturn(expectedDtos);

        // When
        ResponseEntity<List<CompanyResponse>> responseEntity = companyController.listCompanies();

        // Then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).hasSize(1);
        assertThat(responseEntity.getBody().get(0).getName()).isEqualTo("TechCorp");

        verify(readCompanyUseCase).listAllCompanies();
        verify(companyWebMapper).toListCompaniesResponse(domainCompanies);
    }
}
