package com.seby.jobleaf.domain.port.in.contact;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.model.SystemActor;

import java.util.Objects;
import java.util.UUID;

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
        private static final String RESOURCE = "Contact";

        public CreateContactCommand{
            userId = Objects.requireNonNullElse(userId, SystemActor.ID);
            name = requireText(name, "name");
            email = requireText(email, "email");
            userType = requireText(userType, "userType");
            subject = requireText(subject, "subject");
            message = requireText(message, "message");
        }

        private static String requireText(String value, String field) {
            String normalized = normalizeNullableText(value);
            if (normalized == null) {
                throw DomainValidationException.requiredField(field, RESOURCE);
            }
            return normalized;
        }
    }
}
