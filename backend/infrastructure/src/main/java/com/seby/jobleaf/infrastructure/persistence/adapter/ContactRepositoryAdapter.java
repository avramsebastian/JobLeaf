package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.contact.Contact;
import com.seby.jobleaf.domain.model.contact.ContactSort;
import com.seby.jobleaf.domain.model.contact.ContactStatus;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;
import com.seby.jobleaf.domain.model.page.SortDirection;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.ContactPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.ContactJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.seby.jobleaf.infrastructure.persistence.specification.ContactSpecifications.hasStatus;

/**
 * Secondary adapter that implements {@link ContactRepositoryPort} using Spring Data JPA.
 * <p>
 * This is the only place that knows Spring Data's {@link Sort}, {@link Pageable} and {@link Page}: the domain's
 * {@link ContactSort} / {@link PageQuery} come in, a domain {@link PageResult} goes out.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class ContactRepositoryAdapter implements ContactRepositoryPort {

    /** Tie-breaker so rows with equal sort values (e.g. same status) keep the same order on every page. */
    static final String ID = "id";

    private final ContactJpaRepository contactJpaRepository;
    private final ContactPersistenceMapper mapper;

    @Override
    public Contact save(Contact contact) {
        ContactEntity saved = contactJpaRepository.save(mapper.toEntity(contact));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contact> findById(UUID id) {
        return contactJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contact> findAllByStatus(ContactStatus status) {
        return contactJpaRepository.findByStatusOrderByCreatedAtAsc(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contact> findAllByStatus(ContactStatus status, ContactSort sort) {
        return contactJpaRepository.findAll(hasStatus(status), toSort(sort)).stream().map(mapper::toDomain).toList();
    }

    /**
     * Runs two queries: one for the rows of the page ({@code LIMIT/OFFSET}) and a {@code count(*)} for the totals.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Contact> findAllByStatus(ContactStatus status, ContactSort sort, PageQuery page) {
        Pageable pageable = PageRequest.of(page.pageNumber(), page.pageSize(), toSort(sort));
        Page<ContactEntity> result = contactJpaRepository.findAll(hasStatus(status), pageable);
        return new PageResult<>(result.map(mapper::toDomain).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    static Sort toSort(ContactSort sort) {
        Sort.Direction direction = sort.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, sort.field().fieldName()).and(Sort.by(Sort.Direction.ASC, ID));
    }
}
