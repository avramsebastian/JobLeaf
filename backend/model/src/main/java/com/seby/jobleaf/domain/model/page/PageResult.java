package com.seby.jobleaf.domain.model.page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of results: the domain's own version of Spring Data's {@code Page}.
 *
 * @param content       the items on this page
 * @param pageNumber    zero-based index of this page
 * @param pageSize      requested items per page
 * @param totalElements matching items over all pages
 * @param totalPages    number of pages
 */
public record PageResult<T>(List<T> content, int pageNumber, int pageSize, long totalElements, int totalPages) {

    public PageResult {
        content = List.copyOf(content);
    }

    public boolean first() {
        return pageNumber == 0;
    }

    public boolean last() {
        return pageNumber >= totalPages - 1;
    }

    /**
     * @return the same page with every item converted, e.g. domain objects to response DTOs
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), pageNumber, pageSize, totalElements,
                totalPages);
    }
}
