package com.seby.jobleaf.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * JPA entity for the {@code contacts} table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contacts")
@Audited
public class ContactEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "user_type", nullable = false, length = 100)
    private String userType;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "message", nullable = false, length = 255)
    private String message;
}
