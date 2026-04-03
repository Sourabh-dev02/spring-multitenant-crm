package com.crm.multitenant.dto;

import com.crm.multitenant.domain.SubscriptionPlan;
import lombok.Data;

@Data
public class TenantResponse {
    private Long id;
    private String name;
    private String schemaName;
    private String contactEmail;
    private SubscriptionPlan plan;
    private boolean active;
}
