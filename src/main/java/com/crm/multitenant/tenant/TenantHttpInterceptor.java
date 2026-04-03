package com.crm.multitenant.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Plucks the tenant ID out of the request header and stashes it in TenantContext
 * so the rest of the stack (particularly Hibernate) can pick it up.
 *
 * In production you'd get the tenant ID from a validated JWT claim instead of
 * a plain header. The header approach is intentional for this POC - simple to test.
 */
@Slf4j
@Component
public class TenantHttpInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String USER_HEADER   = "X-User-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);
        String userId   = request.getHeader(USER_HEADER);

        if (tenantId != null && !tenantId.isBlank()) {
            // normalize to lowercase - schema names in postgres are case-insensitive but
            // let's keep it consistent everywhere
            TenantContext.setCurrentTenant(tenantId.toLowerCase().trim());
            log.debug("Request routed to tenant: {}", tenantId);
        }

        if (userId != null && !userId.isBlank()) {
            TenantContext.setCurrentUser(userId.trim());
        }

        return true; // proceed with the request
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // This is the most important part - ThreadLocals leak if not cleaned up.
        // afterCompletion runs even when the handler throws, so we're covered.
        TenantContext.clear();
    }
}
