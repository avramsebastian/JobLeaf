package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.domain.model.ContactStatus;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

/**
 * {@link JpaSpecificationExecutor} adds {@code findAll(Specification, Sort)} and
 * {@code findAll(Specification, Pageable)}, used by the sorted and the paged admin queries.
 */
public interface ContactJpaRepository extends JpaRepository<ContactEntity, UUID>,
        JpaSpecificationExecutor<ContactEntity> {

    /**
     * Derived query: Spring Data builds {@code WHERE status = ? ORDER BY created_at ASC} from the method name.
     */
    List<ContactEntity> findByStatusOrderByCreatedAtAsc(ContactStatus status);
}
