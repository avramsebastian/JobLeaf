package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import org.springframework.stereotype.Component;

@Component
public class ContactPersistenceMapper {

    public Contact toDomain(ContactEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Contact(
                entity.id,
                entity.getName(),
                entity.getEmail(),
                entity.getUserType(),
                entity.getSubject(),
                entity.getMessage(),
                entity.createdAt,
                entity.createdBy,
                entity.updatedAt,
                entity.updatedBy
        );
    }

    public ContactEntity toEntity(Contact contact) {
        if (contact == null) {
            return null;
        }

        ContactEntity entity = new ContactEntity();
        entity.id = contact.id();
        entity.setName(contact.name());
        entity.setEmail(contact.email());
        entity.setUserType(contact.userType());
        entity.setSubject(contact.subject());
        entity.setMessage(contact.message());
        entity.createdAt = contact.createdAt();
        entity.createdBy = contact.createdBy();
        entity.updatedAt = contact.updatedAt();
        entity.updatedBy = contact.updatedBy();

        return entity;
    }
}
