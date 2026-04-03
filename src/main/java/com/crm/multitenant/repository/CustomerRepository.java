package com.crm.multitenant.repository;

import com.crm.multitenant.domain.Customer;
import com.crm.multitenant.domain.CustomerStatus;
import com.crm.multitenant.repository.projection.CustomerSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    // basic finders - Spring Data generates these from method names
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);

    // paginated list with a status filter - common for the customer list page
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    /**
     * Projection query - only selects id/firstName/lastName/email/status.
     * Much faster than loading the full entity when we just need a summary list.
     * Spring Data maps the result directly into the CustomerSummary interface.
     */
    @Query("SELECT c.id as id, c.firstName as firstName, c.lastName as lastName, " +
           "c.email as email, c.status as status, c.company as company " +
           "FROM Customer c WHERE c.deleted = false")
    Page<CustomerSummary> findAllSummaries(Pageable pageable);

    /**
     * JPQL with a LIKE search across name and email.
     * Not ideal for large datasets (full scan) - in prod you'd add PG full-text search.
     * Good enough for a POC and shows how to write custom JPQL.
     */
    @Query("SELECT c FROM Customer c WHERE c.deleted = false AND " +
           "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           " LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           " LOWER(c.email)     LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<Customer> search(@Param("term") String term, Pageable pageable);

    /**
     * Fetch join to load customers with their orders in a single query.
     * Without this, iterating orders on each customer = classic N+1.
     * Returns a List because Pageable + fetch join = HibernateException in JPA.
     * Paginate at the service layer if needed (or use a count query separately).
     */
    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders o WHERE c.deleted = false")
    List<Customer> findAllWithOrders();

    // native SQL example - sometimes JPQL just doesn't cut it
    @Query(value = "SELECT COUNT(*) FROM customers WHERE status = :status AND is_deleted = false",
           nativeQuery = true)
    long countByStatusNative(@Param("status") String status);

    // bulk soft-delete - one UPDATE instead of N individual saves
    @Modifying
    @Query("UPDATE Customer c SET c.deleted = true, c.updatedBy = :by WHERE c.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Long> ids, @Param("by") String by);
}
