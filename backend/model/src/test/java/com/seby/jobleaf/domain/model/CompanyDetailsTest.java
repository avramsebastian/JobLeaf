package com.seby.jobleaf.domain.model;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyDetailsTest {

    private static CompanyDetails details(String name, Double rating, Integer founded, Integer employees) {
        return new CompanyDetails(name, "/logos/a.png", "Technology", "Large", rating, "Bucharest", founded,
                "A company", employees, "https://a.example");
    }

    @Test
    @DisplayName("Should trim the texts and give a company without rating 0")
    void validDetails_normalized() {
        var details = details("  DerpCorp  ", null, 2010, null);

        assertThat(details.name()).isEqualTo("DerpCorp");
        assertThat(details.rating()).isEqualTo(0.0);
        assertThat(details.employees()).isNull();
    }

    @Test
    @DisplayName("Should reject blank texts with the field's own message")
    void blankText_rejected() {
        assertThatThrownBy(() -> details("   ", 4.0, 2010, 10))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("Name can not be empty");
    }

    @Test
    @DisplayName("Should enforce the rating, founded and employees ranges")
    void ranges_enforced() {
        assertThatThrownBy(() -> details("A", 5.1, 2010, 10)).hasMessage("Rating must be at most 5");
        assertThatThrownBy(() -> details("A", -0.1, 2010, 10)).hasMessage("Rating must be at least 0");
        assertThatThrownBy(() -> details("A", 4.0, 1899, 10)).hasMessage("Founded must be 1900 or later");
        assertThatThrownBy(() -> details("A", 4.0, null, 10)).hasMessage("Founded can not be empty");
        assertThatThrownBy(() -> details("A", 4.0, Year.now().getValue() + 1, 10))
                .hasMessage("Founded can not be in the future");
        assertThatThrownBy(() -> details("A", 4.0, 2010, 0))
                .hasMessage("Employees must be greater than or equal to 1");
    }

    @Test
    @DisplayName("Should create a company without jobs and replace only the details on update")
    void company_createAndWithDetails() {
        var company = Company.create(UUID.randomUUID(), details("A", 4.0, 2010, 10), SystemActor.ID);
        assertThat(company.jobs()).isEmpty();
        assertThat(company.createdAt()).isNull();

        var renamed = company.withDetails(details("B", 3.0, 2011, 20));
        assertThat(renamed.id()).isEqualTo(company.id());
        assertThat(renamed.name()).isEqualTo("B");
        assertThat(renamed.createdBy()).isEqualTo(company.createdBy());
    }
}
