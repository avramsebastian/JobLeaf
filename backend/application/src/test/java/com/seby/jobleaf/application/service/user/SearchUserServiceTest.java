package com.seby.jobleaf.application.service.user;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.model.user.JobLeafUser;
import com.seby.jobleaf.domain.model.user.RoleName;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchUserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private SearchUserService service;

    @Test
    @DisplayName("Should return the user with this email")
    void searchByEmail_found() {
        var user = new JobLeafUser(UUID.randomUUID(), "Jane", "jane@example.com", null, RoleName.ROLE_JOB_SEEKER,
                null, null, null);
        when(userRepositoryPort.findByEmail("Jane@Example.com")).thenReturn(Optional.of(user));

        assertThat(service.searchByEmail("Jane@Example.com")).isSameAs(user);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException for an unknown email")
    void searchByEmail_notFound() {
        when(userRepositoryPort.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.searchByEmail("nobody@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
