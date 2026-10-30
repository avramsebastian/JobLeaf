package com.seby.jobleaf.domain.port.in.contact;

import com.seby.jobleaf.domain.model.Contact;

import java.util.Objects;
import java.util.UUID;

import static com.seby.jobleaf.domain.model.Contact.SYSTEM_ACTOR_UUID;
import static com.seby.jobleaf.domain.util.TextUtility.normalizeNullableText;

/**
 * Inbound port for creating contacts.
 */
public interface CreateContactUseCase {

    Contact execute(CreateContactCommand command);

    record CreateContactCommand(
            UUID userId,
            String name,
            String email,
            String userType,
            String subject,
            String message
    ){
        public CreateContactCommand{
            userId = Objects.requireNonNullElse(userId, SYSTEM_ACTOR_UUID);
            name = normalizeNullableText(name);
            email = normalizeNullableText(email);
            userType = normalizeNullableText(userType);
            subject = normalizeNullableText(subject);
            message = normalizeNullableText(message);

            Objects.requireNonNull(name, "Contact name cannot be null!");
            Objects.requireNonNull(email, "Contact email cannot be null!");
            Objects.requireNonNull(userType, "Contact userType cannot be null!");
            Objects.requireNonNull(subject, "Contact subject cannot be null!");
            Objects.requireNonNull(message, "Contact message cannot be null!");
        }
    }
}
