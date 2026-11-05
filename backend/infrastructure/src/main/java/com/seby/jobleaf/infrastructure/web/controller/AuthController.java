package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.AuthenticationApi;
import com.seby.jobleaf.apis.generated.model.LoginRequest;
import com.seby.jobleaf.apis.generated.model.LoginResponse;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.AuthWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter implementing generated {@link AuthenticationApi}.
 * <p>
 * Wrong credentials surface as {@code InvalidCredentialsException}, which {@code GlobalExceptionHandler} turns into a
 * 401 problem response, so no try/catch is needed here.
 */
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {

    private final LoginUseCase loginUseCase;
    private final AuthWebMapper authWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoginResponse> loginUser(LoginRequest loginRequest) {
        var result = loginUseCase.login(authWebMapper.toLoginCommand(loginRequest));
        return ResponseEntity.ok(authWebMapper.toLoginResponse(result));
    }
}
