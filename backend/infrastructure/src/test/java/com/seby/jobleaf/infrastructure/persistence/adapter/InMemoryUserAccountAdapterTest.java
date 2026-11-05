package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryUserAccountAdapterTest {

    private final InMemoryUserAccountAdapter adapter = new InMemoryUserAccountAdapter();

    @Test
    @DisplayName("Should find an account by email, ignoring case and surrounding spaces")
    void findByEmail_isCaseInsensitive() {
        assertThat(adapter.findByEmail(" Admin@Example.com ")).get()
                .extracting(UserAccount::role).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should return empty for unknown or missing emails")
    void findByEmail_whenUnknown_returnsEmpty() {
        assertThat(adapter.findByEmail("nobody@example.com")).isEmpty();
        assertThat(adapter.findByEmail(null)).isEmpty();
    }

    @Test
    @DisplayName("Should give Spring Security the email, the BCrypt hash and the role")
    void loadUserByUsername_returnsSecurityUser() {
        UserDetails user = adapter.loadUserByUsername("madan@example.com");

        assertThat(user.getUsername()).isEqualTo("madan@example.com");
        assertThat(user.getPassword()).startsWith("$2a$");
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_JOB_SEEKER");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException for unknown emails")
    void loadUserByUsername_whenUnknown_throws() {
        assertThatThrownBy(() -> adapter.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
