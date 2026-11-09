package com.mysms.core.tx;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Programmatic transactions directly on {@link PlatformTransactionManager} (JAVA-CODING-RULES §4): explicit
 * begin → `set_config('app.tenant_id', …, true)` (the transaction-local equivalent of SET LOCAL, which row-level
 * security reads) → work → commit, with rollback on any failure. Annotation-driven and template-based
 * transactions are banned; every write in MySMS goes through here.
 *
 * <p>One executor per module, bound to that module's own transaction manager and connection pool. The tenant comes
 * from the call context, which the auth filter fills from the token — never from the request.
 */
public final class TenantTransactionExecutor {

    private static final String BIND_TENANT = "SELECT set_config('app.tenant_id', :tenant, true)";

    private final PlatformTransactionManager transactionManager;
    private final JdbcClient jdbc;
    private final boolean tenantScoped;
    private final int timeoutSeconds;

    /**
     * @param tenantScoped false only for the global `master` module, whose tables carry no tenant
     */
    public TenantTransactionExecutor(PlatformTransactionManager transactionManager, JdbcClient jdbc,
                                     boolean tenantScoped, Duration timeout) {
        this.transactionManager = transactionManager;
        this.jdbc = jdbc;
        this.tenantScoped = tenantScoped;
        this.timeoutSeconds = Math.toIntExact(timeout.toSeconds());
    }

    /** Runs a read-write transaction. */
    public <T> T write(TxWork<T> work) {
        return execute(false, work);
    }

    /** Runs a read-only transaction (the database rejects writes inside it). */
    public <T> T read(TxWork<T> work) {
        return execute(true, work);
    }

    /**
     * Runs a read-only transaction that binds no tenant, for the module's global tables only (catalogues such as
     * iam.permission and iam.setting_definition, and iam.tenant_directory, which sign-in reads before a tenant is
     * known). Row-level security still applies and fails closed: a tenant-owned table read here returns no rows or,
     * on a pooled connection whose earlier transaction bound a tenant, an error — never any tenant's rows.
     */
    public <T> T readWithoutTenant(TxWork<T> work) {
        return execute(true, false, work);
    }

    /**
     * Runs a read-write transaction that binds no tenant, for a module's global tables only (no tenant column and no
     * row-level security, such as platform catalogues). A tenant-owned table written here fails closed. The work also
     * runs without the caller's tenant in the call context (user and trace kept), so the events and audit records it
     * writes are global ones even when a signed-in tenant user triggered it (a public form, say).
     */
    public <T> T writeWithoutTenant(TxWork<T> work) {
        Optional<CallContext> caller = CallContextHolder.current();
        if (caller.isEmpty() || caller.get().tenantId().isEmpty()) {
            return execute(false, false, work);
        }
        CallContext global = new CallContext(Optional.empty(), caller.get().userId(), caller.get().traceId());
        return CallContextHolder.callAs(global, () -> execute(false, false, work));
    }

    private <T> T execute(boolean readOnly, TxWork<T> work) {
        return execute(readOnly, tenantScoped, work);
    }

    private <T> T execute(boolean readOnly, boolean bindTenant, TxWork<T> work) {
        DefaultTransactionDefinition definition =
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setReadOnly(readOnly);
        definition.setTimeout(timeoutSeconds);
        TransactionStatus status = transactionManager.getTransaction(definition);
        T result;
        try {
            if (bindTenant && status.isNewTransaction()) {
                bindTenant();
            }
            result = work.run();
        } catch (RuntimeException | Error failure) {
            rollback(status, failure);
            throw failure;
        }
        transactionManager.commit(status);
        return result;
    }

    private void bindTenant() {
        UUID tenant = CallContextHolder.require().tenantId()
                .orElseThrow(() -> new IllegalStateException("tenant-scoped transaction without a tenant in the call context"));
        jdbc.sql(BIND_TENANT).param("tenant", tenant.toString()).query().singleValue();
    }

    private void rollback(TransactionStatus status, Throwable failure) {
        if (status.isCompleted()) {
            return;
        }
        try {
            transactionManager.rollback(status);
        } catch (RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
