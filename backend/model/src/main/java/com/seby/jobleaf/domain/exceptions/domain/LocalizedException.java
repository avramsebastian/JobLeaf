package com.seby.jobleaf.domain.exceptions.domain;

/**
 * Base class for all domain exceptions that carry an i18n message key.
 */
public abstract class LocalizedException extends RuntimeException {

    protected LocalizedException(String fallbackMessage) {
        super(fallbackMessage);
    }

    public abstract String getMessageKey();

    public Object[] getArgs() {
        return new Object[0];
    }
}
