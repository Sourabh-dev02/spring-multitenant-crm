package com.crm.multitenant.repository.projection;

import com.crm.multitenant.domain.CustomerStatus;

/**
 * Interface-based DTO projection for customer list views.
 *
 * Spring Data JPA generates a proxy at runtime that maps query result columns
 * to these getter methods. No class, no constructor, no MapStruct needed.
 * Only the selected columns are fetched from the DB - great for wide tables.
 */
public interface CustomerSummary {
    Long getId();
    String getFirstName();
    String getLastName();
    String getEmail();
    String getCompany();
    CustomerStatus getStatus();

    // convenient default so callers don't have to concatenate everywhere
    default String getFullName() {
        return getFirstName() + " " + getLastName();
    }
}
