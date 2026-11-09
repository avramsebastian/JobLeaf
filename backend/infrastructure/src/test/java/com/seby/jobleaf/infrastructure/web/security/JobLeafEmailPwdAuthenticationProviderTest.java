package com.seby.jobleaf.infrastructure.web.security;

import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.RoleEntity;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Uses a real BCrypt encoder, like the application, and a mocked repository.
 */
class JobLeafEmailPwdAuthenticationProviderTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private JobLeafUserJpaRepository repository;
    private JobLeafEmailPwdAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        repository = mock(JobLeafUserJpaRepository.class);
        provider = new JobLeafEmailPwdAuthenticationProvider(repository, passwordEncoder);

        var role = new RoleEntity(RoleName.ROLE_ADMIN);
        var user = new JobLeafUserEntity();
        user.setEmail("admin@example.com");
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        user.setRole(role);
        when(repository.findWithRoleByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.findWithRoleByEmail("admin@example.com")).thenReturn(Optional.of(user));
    }

    private static Authentication login(String email, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(email, password);
    }

    @Test
    @DisplayName("Should authenticate the correct email and password with the user's role, without keeping the password")
    void authenticate_withCorrectPassword_returnsAuthenticatedUser() {
        Authentication result = provider.authenticate(login(" Admin@Example.com ", "correct-password"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("admin@example.com");
        assertThat(result.getCredentials()).isNull();
        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should reject a wrong password and an unknown email with the same message")
    void authenticate_withWrongCredentials_throwsSameError() {
        assertThatThrownBy(() -> provider.authenticate(login("admin@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class).hasMessage("Invalid email or password");
        assertThatThrownBy(() -> provider.authenticate(login("nobody@example.com", "correct-password")))
                .isInstanceOf(BadCredentialsException.class).hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should handle email and password login tokens only")
    void supports_usernamePasswordTokens() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(Authentication.class)).isFalse();
    }
}
