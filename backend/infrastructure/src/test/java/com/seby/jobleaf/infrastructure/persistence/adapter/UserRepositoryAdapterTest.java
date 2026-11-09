package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.out.auth.UserRepositoryPort.NewUser;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.RoleEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.JobLeafUserPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import com.seby.jobleaf.infrastructure.persistence.repository.RoleJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private JobLeafUserJpaRepository userJpaRepository;

    @Mock
    private RoleJpaRepository roleJpaRepository;

    private UserRepositoryAdapter adapter() {
        return new UserRepositoryAdapter(userJpaRepository, roleJpaRepository, new JobLeafUserPersistenceMapper());
    }

    @Test
    @DisplayName("Should store a new user with id, lower-case email, hash and the role entity")
    void create_storesUserWithRole() {
        // Given
        var role = new RoleEntity(RoleName.ROLE_JOB_SEEKER);
        when(roleJpaRepository.findByName(RoleName.ROLE_JOB_SEEKER)).thenReturn(Optional.of(role));
        when(userJpaRepository.save(any(JobLeafUserEntity.class))).then(returnsFirstArg());

        // When
        JobLeafUser user = adapter().create(
                new NewUser("Jane Doe", "Jane@Example.com", "+40712345678", RoleName.ROLE_JOB_SEEKER), "$2a$10$hash");

        // Then
        ArgumentCaptor<JobLeafUserEntity> saved = ArgumentCaptor.forClass(JobLeafUserEntity.class);
        verify(userJpaRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNotNull();
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(saved.getValue().getRole()).isSameAs(role);
        assertThat(user.role()).isEqualTo(RoleName.ROLE_JOB_SEEKER);
        assertThat(user.email()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Should fail clearly when the role is missing from the roles table")
    void create_whenRoleMissing_throws() {
        when(roleJpaRepository.findByName(RoleName.ROLE_JOB_SEEKER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter().create(
                new NewUser("Jane", "jane@example.com", "+40712345678", RoleName.ROLE_JOB_SEEKER), "hash"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ROLE_JOB_SEEKER");
    }

    @Test
    @DisplayName("Should look up emails case-insensitively")
    void existsByEmail_normalizesEmail() {
        when(userJpaRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThat(adapter().existsByEmail(" Jane@Example.com ")).isTrue();
    }
}
