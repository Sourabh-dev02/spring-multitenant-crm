package com.crm.multitenant.tenant;

/**
 * Holds per-request tenant info using ThreadLocal.
 *
 * Why ThreadLocal? Each HTTP request runs on its own thread, so this gives us
 * zero-overhead request-scoped storage without Spring's request scope overhead.
 *
 * Important: always call clear() at the end of each request! If threads are
 * reused (they always are in a servlet container), leftover tenant values
 * will bleed into the next request on that thread.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER   = new ThreadLocal<>();

    // utility class, no instances
    private TenantContext() {}

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentUser(String username) {
        CURRENT_USER.set(username);
    }

    public static String getCurrentUser() {
        return CURRENT_USER.get();
    }

    // Call this in the interceptor's afterCompletion - never skip it
    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
    }
}
