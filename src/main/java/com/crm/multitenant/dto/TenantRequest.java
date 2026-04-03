package com.crm.multitenant.dto;

import com.crm.multitenant.domain.SubscriptionPlan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    // schema name must be safe to use in SET search_path - enforced here and in the service
    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{2,62}$",
             message = "Schema name must be 3-63 lowercase alphanumeric chars or underscores, starting with a letter")
    private String schemaName;

    @NotBlank
    @Email
    private String contactEmail;

    private SubscriptionPlan plan = SubscriptionPlan.FREE;
}
