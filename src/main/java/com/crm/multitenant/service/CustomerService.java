package com.crm.multitenant.service;

import com.crm.multitenant.domain.Customer;
import com.crm.multitenant.dto.CustomerRequest;
import com.crm.multitenant.dto.CustomerResponse;
import com.crm.multitenant.exception.ConflictException;
import com.crm.multitenant.exception.ResourceNotFoundException;
import com.crm.multitenant.mapper.CustomerMapper;
import com.crm.multitenant.repository.CustomerRepository;
import com.crm.multitenant.repository.projection.CustomerSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // default all methods to read-only; override where needed
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper     customerMapper;

    /**
     * Paged summary list - uses the projection query so we only pull 6 columns
     * instead of the whole entity. Makes a noticeable difference at 100k+ rows.
     */
    public Page<CustomerSummary> listSummaries(Pageable pageable) {
        return customerRepository.findAllSummaries(pageable);
    }

    public Page<CustomerResponse> search(String term, Pageable pageable) {
        return customerRepository.search(term, pageable)
                .map(customerMapper::toResponse);
    }

    // @Cacheable caches by ID - avoids a DB hit on repeated fetches of the same customer
    @Cacheable(value = "customers", key = "#id")
    public CustomerResponse findById(Long id) {
        return customerRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }
        Customer customer = customerMapper.toEntity(request);
        customer = customerRepository.save(customer);
        log.debug("Created customer id={} email={}", customer.getId(), customer.getEmail());
        return customerMapper.toResponse(customer);
    }

    @CacheEvict(value = "customers", key = "#id")
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));

        // check email uniqueness only if it changed
        if (!customer.getEmail().equalsIgnoreCase(request.getEmail())
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        customerMapper.updateEntity(request, customer);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    // soft delete a single customer
    @CacheEvict(value = "customers", key = "#id")
    @Transactional
    public void softDelete(Long id, String deletedBy) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        customer.setDeleted(true);
        customer.setUpdatedBy(deletedBy);
        customerRepository.save(customer);
        log.debug("Soft-deleted customer id={}", id);
    }

    // bulk soft-delete - one UPDATE statement, much faster than N individual saves
    @CacheEvict(value = "customers", allEntries = true)
    @Transactional
    public int bulkSoftDelete(List<Long> ids, String deletedBy) {
        int count = customerRepository.softDeleteByIds(ids, deletedBy);
        log.info("Bulk soft-deleted {} customers", count);
        return count;
    }

    /**
     * Returns full entities with orders already loaded via FETCH JOIN.
     * Use this when you need to process customer + their orders without hitting N+1.
     */
    public List<CustomerResponse> findAllWithOrders() {
        return customerRepository.findAllWithOrders()
                .stream()
                .map(customerMapper::toResponse)
                .toList();
    }
}
