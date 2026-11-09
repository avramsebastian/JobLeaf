package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.AuthenticationApi;
import com.seby.jobleaf.apis.generated.model.LoginRequest;
import com.seby.jobleaf.apis.generated.model.LoginResponse;
import com.seby.jobleaf.apis.generated.model.LogoutResponse;
import com.seby.jobleaf.apis.generated.model.RegisterRequest;
import com.seby.jobleaf.apis.generated.model.RegisterResponse;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.AuthWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter implementing generated {@link AuthenticationApi}.
 * <p>
 * The JWT goes back in an HttpOnly cookie ({@link AccessTokenCookies}), never in the body, so JavaScript cannot read
 * it. Failures surface as domain exceptions ({@code InvalidCredentialsException}, {@code UserAlreadyExistsException},
 * {@code DomainValidationException}), which {@code GlobalExceptionHandler} turns into problem responses.
 */
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final AuthWebMapper authWebMapper;
    private final AccessTokenCookies accessTokenCookies;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<LoginResponse> loginUser(LoginRequest loginRequest) {
        var result = loginUseCase.login(authWebMapper.toLoginCommand(loginRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookies.create(result.token()).toString())
                .body(authWebMapper.toLoginResponse(result));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<RegisterResponse> registerUser(RegisterRequest registerRequest) {
        var result = registerUseCase.register(authWebMapper.toRegisterCommand(registerRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookies.create(result.token()).toString())
                .body(authWebMapper.toRegisterResponse(result));
    }

    /**
     * Logging out is only "forget the token" for the browser: the cookie is HttpOnly, so the server has to delete it.
     * The JWT itself stays valid until it expires (there is no server-side session to end).
     */
    @Override
    public ResponseEntity<LogoutResponse> logoutUser() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookies.clear().toString())
                .body(authWebMapper.toLogoutResponse());
    }
}
