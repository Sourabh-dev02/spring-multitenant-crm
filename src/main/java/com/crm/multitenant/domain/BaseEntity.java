package com.crm.multitenant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Everything in the domain inherits from this. Keeps audit columns, soft-delete
 * and optimistic locking in one place rather than copy-pasting across entities.
 *
 * Using Instant (UTC) instead of LocalDateTime to avoid timezone headaches
 * when tenants are in different regions.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_seq")
    private Long id;

    // optimistic locking - Hibernate increments this on every update
    // if two transactions both read version=3 and try to save, the second one throws OptimisticLockException
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    // soft delete - we never hard-delete customer data, just hide it
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
