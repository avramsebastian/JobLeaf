package com.seby.jobleaf.domain.exceptions.domain;

/**
 * Thrown when input breaks a domain rule (e.g. a required field is blank) — maps to HTTP 400.
 */
public class DomainValidationException extends LocalizedException {

    public static final String REQUIRED_FIELD_MESSAGE_KEY = "error.validation.field.required";

    private final String messageKey;
    private final String field;

    public DomainValidationException(String messageKey, String field, String fallbackMessage) {
        super(fallbackMessage);
        this.messageKey = messageKey;
        this.field = field;
    }

    /**
     * @param field         name of the missing field, as exposed in the API
     * @param resourceLabel human-readable resource name used in the message (e.g. "Contact")
     */
    public static DomainValidationException requiredField(String field, String resourceLabel) {
        return new DomainValidationException(REQUIRED_FIELD_MESSAGE_KEY, field,
                resourceLabel + " " + field + " is required");
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public Object[] getArgs() {
        return new Object[]{field};
    }

    public String getField() {
        return field;
    }
}
