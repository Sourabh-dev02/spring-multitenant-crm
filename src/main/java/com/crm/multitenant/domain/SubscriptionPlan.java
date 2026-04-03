package com.crm.multitenant.domain;

/**
 * Subscription tiers a tenant can be on. Stored as a string in the DB
 * (EnumType.STRING) so adding new tiers doesn't require a schema migration.
 */
public enum SubscriptionPlan {
    FREE,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE
}
