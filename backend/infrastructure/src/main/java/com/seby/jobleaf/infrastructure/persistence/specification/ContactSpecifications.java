package com.seby.jobleaf.infrastructure.persistence.specification;

import com.seby.jobleaf.domain.model.ContactStatus;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable WHERE conditions for {@link ContactEntity} queries.
 * <p>
 * A {@link Specification} is one condition built with the JPA Criteria API ({@code root} = the entity,
 * {@code criteriaBuilder} = the factory for predicates like {@code equal}, {@code like}, {@code and}). Spring Data
 * turns it into the WHERE clause, and adds ORDER BY ({@code Sort}) and LIMIT/OFFSET ({@code Pageable}) on top.
 * Conditions combine with {@code and()} / {@code or()}, so new filters (e.g. by email) can be added later without
 * writing a repository method for every combination.
 */
public final class ContactSpecifications {

    static final String STATUS = "status";

    private ContactSpecifications() {
    }

    /**
     * @return {@code WHERE status = :status}
     */
    public static Specification<ContactEntity> hasStatus(ContactStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(STATUS), status);
    }
}
