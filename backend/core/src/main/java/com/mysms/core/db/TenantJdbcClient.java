package com.mysms.core.db;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The module's {@link JdbcClient}: every statement whose SQL names {@code :tenantId} gets the tenant of the current
 * call bound to it (JAVA-CODING-RULES §1.6, §6 R1). Each read and write of a tenant table states
 * {@code tenant_id = :tenantId} in its own SQL, so the tenant is filtered explicitly and row-level security is only
 * the backstop. Without a bound tenant the parameter stays unbound and the statement fails, never runs unfiltered.
 */
public final class TenantJdbcClient implements JdbcClient {

    /** The named parameter tenant-scoped SQL uses. */
    public static final String TENANT_PARAM = "tenantId";
    private static final String MARKER = ":" + TENANT_PARAM;

    private final JdbcClient delegate;

    private TenantJdbcClient(JdbcClient delegate) {
        this.delegate = delegate;
    }

    public static JdbcClient wrap(JdbcClient delegate) {
        return new TenantJdbcClient(delegate);
    }

    /** The tenant of the current call, when one is bound. */
    public static Optional<UUID> currentTenant() {
        return CallContextHolder.current().flatMap(CallContext::tenantId);
    }

    @Override
    public StatementSpec sql(String sql) {
        StatementSpec spec = delegate.sql(sql);
        if (sql.contains(MARKER)) {
            Optional<UUID> tenant = currentTenant();
            if (tenant.isPresent()) {
                spec = spec.param(TENANT_PARAM, tenant.get());
            }
        }
        return spec;
    }
}
