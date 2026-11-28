package com.mysms.platform;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.util.UUID;

/**
 * Registers a component test's tenant in {@code platform.tenant}, as iam's installation bootstrap does in a running
 * system: the platform's rows of a tenant refer to it, so without it the audit trail of every write the test makes in
 * that tenant (each module's {@code audit.recorded}, kept in {@code platform.audit_log}) is refused by the inbox.
 */
public final class PlatformTenants {

    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    private PlatformTenants() {
    }

    /** Adds the tenant (a code derived from its id) unless it is there already. */
    public static void register(ModuleDatabase platform, UUID tenant) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(platform).write(() -> platform.jdbc()
                        .sql("INSERT INTO platform.tenant (id, code, name) VALUES (:tenant, "
                                + "'t' || substr(md5(CAST(:tenant AS text)), 1, 12), 'Test tenant') "
                                + "ON CONFLICT (id) DO NOTHING")
                        .param("tenant", tenant).update()));
    }
}
