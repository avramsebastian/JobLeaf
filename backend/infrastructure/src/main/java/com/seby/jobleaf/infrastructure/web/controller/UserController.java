package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.UserApi;
import com.seby.jobleaf.apis.generated.model.UserResponse;
import com.seby.jobleaf.domain.port.in.user.AssignCompanyToEmployerUseCase;
import com.seby.jobleaf.domain.port.in.user.ElevateToEmployerUseCase;
import com.seby.jobleaf.domain.port.in.user.SearchUserUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.AuthWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST adapter implementing generated {@link UserApi}: the admin's user management.
 * <p>
 * All endpoints need {@code ROLE_ADMIN}, enforced by the security filter chain ({@code adminPaths}). The user is
 * returned as the same {@code UserResponse} that login and register use, so {@link AuthWebMapper} maps it.
 */
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final SearchUserUseCase searchUserUseCase;
    private final ElevateToEmployerUseCase elevateToEmployerUseCase;
    private final AssignCompanyToEmployerUseCase assignCompanyToEmployerUseCase;
    private final AuthWebMapper authWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<UserResponse> searchUserByEmail(String email) {
        return ResponseEntity.ok(authWebMapper.toUserResponse(searchUserUseCase.searchByEmail(email)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<UserResponse> elevateToEmployer(UUID userId) {
        return ResponseEntity.ok(authWebMapper.toUserResponse(elevateToEmployerUseCase.elevateToEmployer(userId)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<UserResponse> assignCompanyToEmployer(UUID userId, UUID companyId) {
        var user = assignCompanyToEmployerUseCase.assignCompany(userId, companyId);
        return ResponseEntity.ok(authWebMapper.toUserResponse(user));
    }
}
