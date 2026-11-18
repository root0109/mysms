package com.mysms.authclient;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportIdentity;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Adapter of core's {@link ImportIdentity}: the importing user holding only the permission the platform checked, in
 * the institution it was checked for (tenant-wide only when it was held tenant-wide). It has no token, so it cannot
 * call other modules.
 */
public final class ImportCallerIdentity implements ImportIdentity {

    @Override
    public <T> T callAs(ImportBatch batch, String permission, Supplier<T> work) {
        CallContext event = CallContextHolder.require();
        UUID tenant = event.tenantId().orElseThrow(() -> new IllegalArgumentException("an import runs in a tenant"));
        if (batch.institutionId().isEmpty() && !batch.tenantWide()) {
            throw new IllegalArgumentException("tenant-level records are imported only under a tenant-wide grant");
        }
        boolean acrossTenant = batch.institutionId().isEmpty();
        AuthContext importer = new AuthContext(batch.userId(), Optional.of(tenant),
                batch.institutionId().map(Set::of).orElse(Set.of()), Optional.empty(), Set.of(permission),
                "import:" + batch.operationId(), batch.impersonatorId(), false, Optional.empty(), Optional.empty(),
                acrossTenant, acrossTenant ? Set.of(permission) : Set.of());
        if (AuthContextHolder.current().isPresent()) {
            throw new IllegalStateException("an import batch runs on an event thread, never inside a request");
        }
        CallContextHolder.set(event.withIdentity(tenant, batch.userId())
                .withImpersonator(batch.impersonatorId().orElse(null)));
        AuthContextHolder.set(importer, null);
        try {
            return work.get();
        } finally {
            AuthContextHolder.clear();
            CallContextHolder.set(event);
        }
    }
}
