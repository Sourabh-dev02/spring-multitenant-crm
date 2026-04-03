package com.crm.multitenant.dto;

import com.crm.multitenant.domain.CustomerStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class CustomerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String company;
    private CustomerStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
