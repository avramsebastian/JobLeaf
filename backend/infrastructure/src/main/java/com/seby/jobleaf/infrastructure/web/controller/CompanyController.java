package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.CompanyApi;
import com.seby.jobleaf.apis.generated.model.CompanyAdminResponse;
import com.seby.jobleaf.apis.generated.model.CompanyRequest;
import com.seby.jobleaf.apis.generated.model.CompanyResponse;
import com.seby.jobleaf.apis.generated.model.DeleteCompanyResponse;
import com.seby.jobleaf.domain.port.in.company.CreateCompanyUseCase;
import com.seby.jobleaf.domain.port.in.company.DeleteCompanyUseCase;
import com.seby.jobleaf.domain.port.in.company.ReadCompanyUseCase;
import com.seby.jobleaf.domain.port.in.company.UpdateCompanyUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.CompanyWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter implementing generated {@link CompanyApi}.
 * <p>
 * The {@code /admin} endpoints need {@code ROLE_ADMIN}: that is enforced by the security filter chain
 * ({@code adminPaths} in {@code PathsConfig}) before a request reaches this class.
 */
@RestController
@RequiredArgsConstructor
public class CompanyController implements CompanyApi {

    private final ReadCompanyUseCase readCompanyUseCase;
    private final CreateCompanyUseCase createCompanyUseCase;
    private final UpdateCompanyUseCase updateCompanyUseCase;
    private final DeleteCompanyUseCase deleteCompanyUseCase;
    private final CompanyWebMapper companyWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        var listCompanies = readCompanyUseCase.listAllCompanies();
        return ResponseEntity.ok(companyWebMapper.toListCompaniesResponse(listCompanies));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CompanyAdminResponse>> listCompaniesForAdmin() {
        var companies = readCompanyUseCase.listCompaniesForAdmin();
        return ResponseEntity.ok(companyWebMapper.toCompanyAdminResponses(companies));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CompanyAdminResponse> createCompany(CompanyRequest companyRequest) {
        var company = createCompanyUseCase.createCompany(companyWebMapper.toCompanyDetails(companyRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(companyWebMapper.toCompanyAdminResponse(company));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CompanyAdminResponse> updateCompany(UUID id, CompanyRequest companyRequest) {
        var company = updateCompanyUseCase.updateCompany(id, companyWebMapper.toCompanyDetails(companyRequest));
        return ResponseEntity.ok(companyWebMapper.toCompanyAdminResponse(company));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<DeleteCompanyResponse> deleteCompany(UUID id) {
        deleteCompanyUseCase.deleteCompany(id);
        return ResponseEntity.ok(companyWebMapper.toDeleteCompanyResponse());
    }
}
