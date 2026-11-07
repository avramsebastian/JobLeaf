package com.seby.jobleaf.application.service.auth;

import com.seby.jobleaf.domain.exceptions.auth.UserAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase.RegisterCommand;
import com.seby.jobleaf.domain.port.out.auth.PasswordBreachCheckerPort;
import com.seby.jobleaf.domain.port.out.auth.PasswordHasherPort;
import com.seby.jobleaf.domain.port.out.auth.TokenIssuerPort;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort.NewUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordBreachCheckerPort passwordBreachCheckerPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private TokenIssuerPort tokenIssuerPort;

    @InjectMocks
    private RegisterService registerService;

    private static final RegisterCommand COMMAND =
            new RegisterCommand("Jane Doe", "jane@example.com", "+40712345678", "a-Long-unique-pass-42");

    @Test
    @DisplayName("Should store the user as a job seeker with a hashed password and log them in")
    void register_withNewUser_createsJobSeekerAndIssuesToken() {
        // Given
        JobLeafUser created = new JobLeafUser(UUID.randomUUID(), "Jane Doe", "jane@example.com", "+40712345678",
                RoleName.ROLE_JOB_SEEKER, null, null, Instant.now());
        AuthToken token = new AuthToken("jwt-value", Instant.now().plusSeconds(3600));
        when(passwordHasherPort.hash("a-Long-unique-pass-42")).thenReturn("$2a$10$hash");
        when(userRepositoryPort.create(new NewUser("Jane Doe", "jane@example.com", "+40712345678",
                RoleName.ROLE_JOB_SEEKER), "$2a$10$hash")).thenReturn(created);
        when(tokenIssuerPort.issue(created)).thenReturn(token);

        // When
        LoginResult result = registerService.register(COMMAND);

        // Then
        assertThat(result.user()).isSameAs(created);
        assertThat(result.token()).isSameAs(token);
    }

    @Test
    @DisplayName("Should reject an already registered email without storing anything")
    void register_withExistingEmail_throws409() {
        when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registerService.register(COMMAND))
                .isInstanceOf(UserAlreadyExistsException.class)
                .extracting("field").isEqualTo("email");
        verify(userRepositoryPort, never()).create(any(), anyString());
        verifyNoInteractions(passwordHasherPort, tokenIssuerPort);
    }

    @Test
    @DisplayName("Should reject an already registered mobile number without storing anything")
    void register_withExistingMobileNumber_throws409() {
        when(userRepositoryPort.existsByMobileNumber("+40712345678")).thenReturn(true);

        assertThatThrownBy(() -> registerService.register(COMMAND))
                .isInstanceOf(UserAlreadyExistsException.class)
                .extracting("field").isEqualTo("mobileNumber");
        verify(userRepositoryPort, never()).create(any(), anyString());
    }

    @Test
    @DisplayName("Should reject a password found in data breaches before hashing or storing it")
    void register_withCompromisedPassword_throws400() {
        when(passwordBreachCheckerPort.isCompromised("a-Long-unique-pass-42")).thenReturn(true);

        assertThatThrownBy(() -> registerService.register(COMMAND))
                .isInstanceOf(DomainValidationException.class)
                .extracting("field").isEqualTo("password");
        verifyNoInteractions(passwordHasherPort, tokenIssuerPort);
        verify(userRepositoryPort, never()).create(any(), anyString());
    }
}
