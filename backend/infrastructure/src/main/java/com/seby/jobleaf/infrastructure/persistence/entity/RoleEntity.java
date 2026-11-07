package com.seby.jobleaf.infrastructure.persistence.entity;

import com.seby.jobleaf.domain.model.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * JPA entity for the {@code roles} table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
@Audited
public class RoleEntity extends BaseEntity {

    /**
     * Stored as the enum constant's name (e.g. {@code ROLE_ADMIN}), never as its position, so reordering the enum
     * cannot change what is in the database.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 50)
    protected RoleName name;
}
