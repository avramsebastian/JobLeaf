package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.LoginRequest;
import com.seby.jobleaf.apis.generated.model.LoginResponse;
import com.seby.jobleaf.apis.generated.model.LogoutResponse;
import com.seby.jobleaf.apis.generated.model.RegisterRequest;
import com.seby.jobleaf.apis.generated.model.RegisterResponse;
import com.seby.jobleaf.apis.generated.model.UserResponse;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginCommand;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase.RegisterCommand;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.util.Optional.ofNullable;

/**
 * Maps between the OpenAPI authentication DTOs (login, register) and the use cases.
 */
@Component
public class AuthWebMapper {

    static final String LOGIN_SUCCESS_MESSAGE = "Login successful";
    static final String REGISTER_SUCCESS_MESSAGE = "Registration successful";
    static final String LOGOUT_SUCCESS_MESSAGE = "Logout successful";

    public LoginCommand toLoginCommand(LoginRequest request) {
        return new LoginCommand(request.getEmail(), request.getPassword());
    }

    public LoginResponse toLoginResponse(LoginResult result) {
        var response = new LoginResponse();
        response.setMessage(LOGIN_SUCCESS_MESSAGE);
        response.setUser(toUserResponse(result.user()));
        response.setExpiresAt(convertInstant(result.token().expiresAt()));
        return response;
    }

    public RegisterCommand toRegisterCommand(RegisterRequest request) {
        return new RegisterCommand(request.getName(), request.getEmail(), request.getMobileNumber(),
                request.getPassword());
    }

    /**
     * Registration logs the user in, so the response carries the same user and expiry as a login.
     */
    public RegisterResponse toRegisterResponse(LoginResult result) {
        var response = new RegisterResponse();
        response.setMessage(REGISTER_SUCCESS_MESSAGE);
        response.setUser(toUserResponse(result.user()));
        response.setExpiresAt(convertInstant(result.token().expiresAt()));
        return response;
    }

    public LogoutResponse toLogoutResponse() {
        var response = new LogoutResponse();
        response.setMessage(LOGOUT_SUCCESS_MESSAGE);
        return response;
    }

    public UserResponse toUserResponse(JobLeafUser user) {
        var response = new UserResponse();
        response.setUserId(user.id());
        response.setName(user.name());
        response.setEmail(user.email());
        response.setMobileNumber(user.mobileNumber());
        response.setRole(UserResponse.RoleEnum.fromValue(user.role().name()));
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
