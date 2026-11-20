package com.mysms.core.db;

import com.mysms.core.trace.RequestCost;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Binds the current call's tenant to every statement that names {@code :tenantId}, so tenant tables are filtered
 * explicitly and row-level security is only the backstop. Without a bound tenant the statement fails, never runs
 * unfiltered. Each statement started counts towards the request's {@link RequestCost}.
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
        RequestCost.statement();
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
