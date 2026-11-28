package com.seby.jobleaf.domain.model.contact;

/**
 * Lifecycle of a contact message: every message starts as {@link #NEW} and an admin closes it once handled.
 */
public enum ContactStatus {
    NEW,
    CLOSED
}
