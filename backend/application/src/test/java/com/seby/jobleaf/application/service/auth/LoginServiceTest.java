package com.seby.jobleaf.application.service.auth;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginCommand;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import com.seby.jobleaf.domain.port.out.auth.CredentialsVerifierPort;
import com.seby.jobleaf.domain.port.out.auth.TokenIssuerPort;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private CredentialsVerifierPort credentialsVerifierPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private TokenIssuerPort tokenIssuerPort;

    @InjectMocks
    private LoginService loginService;

    private static final LoginCommand COMMAND = new LoginCommand("admin@example.com", "secret123");
    private static final JobLeafUser USER = new JobLeafUser(UUID.randomUUID(), "Admin", "admin@example.com", null,
            RoleName.ROLE_ADMIN, null, null, Instant.parse("2026-11-03T00:00:00Z"));

    @Test
    @DisplayName("Should verify the credentials, load the account and return it with a new token")
    void login_withValidCredentials_returnsUserAndToken() {
        // Given
        AuthToken token = new AuthToken("jwt-value", Instant.now().plusSeconds(3600));
        when(userRepositoryPort.findByEmail("admin@example.com")).thenReturn(Optional.of(USER));
        when(tokenIssuerPort.issue(USER)).thenReturn(token);

        // When
        LoginResult result = loginService.login(COMMAND);

        // Then
        verify(credentialsVerifierPort).verify("admin@example.com", "secret123");
        assertThat(result.user()).isSameAs(USER);
        assertThat(result.token()).isSameAs(token);
    }

    @Test
    @DisplayName("Should stop at wrong credentials without loading the account or issuing a token")
    void login_withWrongCredentials_throws() {
        // Given
        doThrow(new InvalidCredentialsException()).when(credentialsVerifierPort).verify(any(), any());

        // When / Then
        assertThatThrownBy(() -> loginService.login(COMMAND))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(userRepositoryPort, tokenIssuerPort);
    }

    @Test
    @DisplayName("Should answer like wrong credentials when the account is missing, and issue no token")
    void login_whenAccountIsMissing_throwsInvalidCredentials() {
        // Given
        when(userRepositoryPort.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> loginService.login(COMMAND)).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(tokenIssuerPort);
    }

    @Test
    @DisplayName("Should reject a null command")
    void login_whenCommandIsNull_throws() {
        assertThatThrownBy(() -> loginService.login(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(credentialsVerifierPort, userRepositoryPort, tokenIssuerPort);
    }
}
