package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.LoginRequest;
import com.seby.jobleaf.apis.generated.model.LoginResponse;
import com.seby.jobleaf.apis.generated.model.UserResponse;
import com.seby.jobleaf.domain.model.UserAccount;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginCommand;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.util.Optional.ofNullable;

/**
 * Maps between the OpenAPI login DTOs and the login use case.
 */
@Component
public class AuthWebMapper {

    static final String LOGIN_SUCCESS_MESSAGE = "Login successful";

    public LoginCommand toLoginCommand(LoginRequest request) {
        return new LoginCommand(request.getEmail(), request.getPassword());
    }

    public LoginResponse toLoginResponse(LoginResult result) {
        var response = new LoginResponse();
        response.setMessage(LOGIN_SUCCESS_MESSAGE);
        response.setUser(toUserResponse(result.user()));
        response.setJwtToken(result.token().value());
        response.setExpiresAt(convertInstant(result.token().expiresAt()));
        return response;
    }

    public UserResponse toUserResponse(UserAccount user) {
        var response = new UserResponse();
        response.setUserId(user.id());
        response.setName(user.name());
        response.setEmail(user.email());
        response.setMobileNumber(user.mobileNumber());
        response.setRole(UserResponse.RoleEnum.fromValue(user.role()));
        response.setCompanyId(user.companyId());
        response.setCompanyName(user.companyName());
        response.setCreatedAt(convertInstant(user.createdAt()));
        return response;
    }

    private OffsetDateTime convertInstant(Instant instant) {
        return ofNullable(instant).map(date -> date.atOffset(ZoneOffset.UTC))
                .orElse(null);
    }
}
