package com.seby.jobleaf.infrastructure.persistence.audit;

import org.hibernate.envers.RevisionListener;

/**
 * Stamps every new Envers revision with the acting user.
 */
public class ActorRevisionListener implements RevisionListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevisionEntity) revisionEntity).setUserId(CurrentActor.id());
    }
}
