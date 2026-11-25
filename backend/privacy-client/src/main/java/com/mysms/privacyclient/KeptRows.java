package com.mysms.privacyclient;

/**
 * One table whose rows of a subject are kept under a legal-retention policy (design §9.1, §10), declared by the
 * module's handler with an SQL constant of its repository layer: an erasure leaves them untouched and answers their
 * count as held, with the policy's code; an export counts them. {@code count} is a {@code SELECT count(*) …} of the
 * subject's rows kept ({@code :subjectId}) and filters {@code tenant_id = :tenantId}.
 *
 * @param table      the table, {@code <module>.<table>}
 * @param count      the statement counting the subject's rows kept
 * @param policyCode the privacy retention policy that keeps them, e.g. {@code payroll.after_leaving}
 */
public record KeptRows(String table, String count, String policyCode) {
}
