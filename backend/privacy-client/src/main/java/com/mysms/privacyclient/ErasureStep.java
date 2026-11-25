package com.mysms.privacyclient;

import java.util.List;

/**
 * One table of a module's erasure (design §9.1), declared by the module's handler with SQL constants of its
 * repository layer. {@code select} lists the ids of the subject's live rows ({@code :subjectId}, plus the sweep's
 * {@code :cutoff} in a retention sweep); {@code erase} changes one row ({@code :id}, {@code :now}) and answers 1, or 0
 * when it changed meanwhile. Every statement filters {@code tenant_id = :tenantId}.
 *
 * @param table        the table, {@code <module>.<table>}: the audited entity and a record-set hold's entity type
 * @param categoryCode the data category of the rows (privacy's catalogue), which a category hold names
 * @param select       the ids of the subject's rows to erase
 * @param erase        the statement that erases one row
 * @param deletes      true: the row is cleared and soft-deleted (audited as a delete, {@code <table>.deleted}); false:
 *                     it is anonymised and kept (audited as an update of {@code fields}, {@code <table>.updated})
 * @param fields       the contract fields the statement overwrites, for the audit record
 */
public record ErasureStep(String table, String categoryCode, String select, String erase, boolean deletes,
                          List<String> fields) {

    public ErasureStep {
        fields = List.copyOf(fields);
    }
}
