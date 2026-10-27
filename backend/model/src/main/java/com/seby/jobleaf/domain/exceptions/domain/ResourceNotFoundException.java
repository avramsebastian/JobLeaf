package com.seby.jobleaf.domain.exceptions.domain;

/**
 * Generic "resource not found" domain exception — maps to HTTP 404.
 */
public class ResourceNotFoundException extends LocalizedException {

    private final String messageKey;
    private final Object[] args;

    public ResourceNotFoundException(String messageKey, String fallbackMessage, Object... args) {
        super(fallbackMessage);
        this.messageKey = messageKey;
        this.args = args == null ? new Object[0] : args.clone();
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public Object[] getArgs() {
        return args.clone();
    }
}
