package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.CompanyApi;
import com.seby.jobleaf.apis.generated.model.CompanyResponse;
import com.seby.jobleaf.domain.port.in.company.ReadCompanyUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.CompanyWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST adapter implementing generated {@link CompanyApi}.
 */
@RestController
@RequiredArgsConstructor
public class CompanyController implements CompanyApi {

    private final ReadCompanyUseCase readCompanyUseCase;
    private final CompanyWebMapper companyWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<CompanyResponse>> listCompanies() {
        var listCompanies = readCompanyUseCase.listAllCompanies();
        return ResponseEntity.ok(companyWebMapper.toListCompaniesResponse(listCompanies));
    }
}
