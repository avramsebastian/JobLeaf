package com.seby.jobleaf.domain.model.jobapplication;

/**
 * An application together with its applicant: what an employer sees for the jobs of their company.
 */
public record ReceivedApplication(JobApplication application, Applicant applicant) {

    public ReceivedApplication withApplication(JobApplication changed) {
        return new ReceivedApplication(changed, applicant);
    }
}
