package com.mysms.core.imports;

import java.util.function.Supplier;

/**
 * Port that runs an {@link ImportBatch}'s rows as the user who started the import, holding only the permission the
 * platform checked, in the institution it was checked for, so an import never does more than its user could.
 */
public interface ImportIdentity {

    /** Runs {@code work} with the batch's user bound (call context and auth context), then restores what was bound. */
    <T> T callAs(ImportBatch batch, String permission, Supplier<T> work);
}
