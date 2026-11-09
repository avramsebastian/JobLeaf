package com.seby.jobleaf.infrastructure.web.security.adapter;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses a real {@link DaoAuthenticationProvider} with a real BCrypt hash, like the application's AuthenticationManager.
 */
class SpringSecurityCredentialsAdapterTest {

    private SpringSecurityCredentialsAdapter adapter;

    @BeforeEach
    void setUp() {
        var encoder = new BCryptPasswordEncoder();
        var users = new InMemoryUserDetailsManager(User.withUsername("admin@example.com")
                .password(encoder.encode("correct-password")).authorities("ROLE_ADMIN").build());
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        adapter = new SpringSecurityCredentialsAdapter(new ProviderManager(provider));
    }

    @Test
    @DisplayName("Should accept the correct email and password")
    void verify_withCorrectPassword_passes() {
        assertThatCode(() -> adapter.verify("admin@example.com", "correct-password")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject a wrong password with the generic invalid credentials error")
    void verify_withWrongPassword_throws() {
        assertThatThrownBy(() -> adapter.verify("admin@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should reject an unknown email with the same generic error")
    void verify_withUnknownEmail_throws() {
        assertThatThrownBy(() -> adapter.verify("nobody@example.com", "correct-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
