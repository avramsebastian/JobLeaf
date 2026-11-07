package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.LoginRequest;
import com.seby.jobleaf.apis.generated.model.LoginResponse;
import com.seby.jobleaf.apis.generated.model.UserResponse;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginCommand;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthWebMapperTest {

    private final AuthWebMapper mapper = new AuthWebMapper();

    @Test
    @DisplayName("Should map the login request into a normalized login command")
    void toLoginCommand_mapsFields() {
        LoginRequest request = new LoginRequest();
        request.setEmail("Admin@Example.com");
        request.setPassword("secret123");

        LoginCommand command = mapper.toLoginCommand(request);

        assertThat(command.email()).isEqualTo("admin@example.com");
        assertThat(command.password()).isEqualTo("secret123");
    }

    @Test
    @DisplayName("Should map the login result into the login response with the user and the token")
    void toLoginResponse_mapsAllFields() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-03T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-11-03T13:00:00Z");
        JobLeafUser user = new JobLeafUser(userId, "John Smith", "john@company.com", "+12025550143",
                RoleName.ROLE_EMPLOYER, companyId, "Acme Corp", createdAt);

        // When
        LoginResponse response = mapper.toLoginResponse(new LoginResult(user, new AuthToken("jwt-value", expiresAt)));

        // Then
        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getExpiresAt()).isEqualTo(expiresAt.atOffset(ZoneOffset.UTC));
        UserResponse userResponse = response.getUser();
        assertThat(userResponse.getUserId()).isEqualTo(userId);
        assertThat(userResponse.getName()).isEqualTo("John Smith");
        assertThat(userResponse.getEmail()).isEqualTo("john@company.com");
        assertThat(userResponse.getMobileNumber()).isEqualTo("+12025550143");
        assertThat(userResponse.getRole()).isEqualTo(UserResponse.RoleEnum.ROLE_EMPLOYER);
        assertThat(userResponse.getCompanyId()).isEqualTo(companyId);
        assertThat(userResponse.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(userResponse.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
    }
}
