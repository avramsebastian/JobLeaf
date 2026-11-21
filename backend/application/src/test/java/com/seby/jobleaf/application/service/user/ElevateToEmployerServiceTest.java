package com.seby.jobleaf.application.service.user;

import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserRoleConflictException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevateToEmployerServiceTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private ElevateToEmployerService service;

    private static JobLeafUser user(RoleName role) {
        return new JobLeafUser(ID, "Jane", "jane@example.com", null, role, null, null, null);
    }

    @Test
    @DisplayName("Should make a job seeker an employer")
    void jobSeeker_elevated() {
        when(userRepositoryPort.findById(ID)).thenReturn(Optional.of(user(RoleName.ROLE_JOB_SEEKER)));
        when(userRepositoryPort.updateRole(ID, RoleName.ROLE_EMPLOYER)).thenReturn(user(RoleName.ROLE_EMPLOYER));

        assertThat(service.elevateToEmployer(ID).role()).isEqualTo(RoleName.ROLE_EMPLOYER);
    }

    @Test
    @DisplayName("Should leave an employer unchanged and save nothing")
    void employer_unchanged() {
        when(userRepositoryPort.findById(ID)).thenReturn(Optional.of(user(RoleName.ROLE_EMPLOYER)));

        assertThat(service.elevateToEmployer(ID).role()).isEqualTo(RoleName.ROLE_EMPLOYER);
        verify(userRepositoryPort, never()).updateRole(any(), any());
    }

    @Test
    @DisplayName("Should refuse to change an admin")
    void admin_conflict() {
        when(userRepositoryPort.findById(ID)).thenReturn(Optional.of(user(RoleName.ROLE_ADMIN)));

        assertThatThrownBy(() -> service.elevateToEmployer(ID)).isInstanceOf(UserRoleConflictException.class);
        verify(userRepositoryPort, never()).updateRole(any(), any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException for an unknown id")
    void unknownUser_notFound() {
        when(userRepositoryPort.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.elevateToEmployer(ID)).isInstanceOf(UserNotFoundException.class);
    }
}
