package com.seby.jobleaf.domain.model.page;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;

/**
 * Which page the caller wants. Framework free on purpose: the persistence adapter turns it into a Spring Data
 * {@code Pageable}, so the domain and application layers never depend on Spring Data.
 *
 * @param pageNumber zero-based page index
 * @param pageSize   items per page, 1 to {@value #MAX_PAGE_SIZE}
 */
public record PageQuery(int pageNumber, int pageSize) {

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public PageQuery {
        if (pageNumber < 0) {
            throw DomainValidationException.invalidField("pageNumber", "pageNumber must be 0 or greater");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw DomainValidationException.invalidField("pageSize",
                    "pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * @param pageNumber requested page, {@code null} means {@value #DEFAULT_PAGE_NUMBER}
     * @param pageSize   requested size, {@code null} means {@value #DEFAULT_PAGE_SIZE}
     */
    public static PageQuery of(Integer pageNumber, Integer pageSize) {
        return new PageQuery(pageNumber == null ? DEFAULT_PAGE_NUMBER : pageNumber,
                pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
    }
}
