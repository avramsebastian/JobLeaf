package com.seby.jobleaf.domain.model.user;

import com.seby.jobleaf.domain.exceptions.user.UserRoleConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobLeafUserRulesTest {

    private static JobLeafUser user(RoleName role) {
        return new JobLeafUser(UUID.randomUUID(), "Jane", "jane@example.com", null, role, null, null, null);
    }

    @Test
    @DisplayName("Should let job seekers and employers become employers, but not admins")
    void checkCanBecomeEmployer() {
        assertThatCode(() -> user(RoleName.ROLE_JOB_SEEKER).checkCanBecomeEmployer()).doesNotThrowAnyException();
        assertThatCode(() -> user(RoleName.ROLE_EMPLOYER).checkCanBecomeEmployer()).doesNotThrowAnyException();
        assertThatThrownBy(() -> user(RoleName.ROLE_ADMIN).checkCanBecomeEmployer())
                .isInstanceOf(UserRoleConflictException.class)
                .hasMessageContaining("is an admin");
    }

    @Test
    @DisplayName("Should only let employers be assigned to a company")
    void checkCanBeAssignedToCompany() {
        assertThatCode(() -> user(RoleName.ROLE_EMPLOYER).checkCanBeAssignedToCompany()).doesNotThrowAnyException();
        assertThatThrownBy(() -> user(RoleName.ROLE_JOB_SEEKER).checkCanBeAssignedToCompany())
                .isInstanceOf(UserRoleConflictException.class)
                .hasMessageContaining("Only employers");
        assertThatThrownBy(() -> user(RoleName.ROLE_ADMIN).checkCanBeAssignedToCompany())
                .isInstanceOf(UserRoleConflictException.class);
    }
}
