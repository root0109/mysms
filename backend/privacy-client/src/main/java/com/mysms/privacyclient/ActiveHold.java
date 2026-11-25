package com.mysms.privacyclient;

import java.util.Set;
import java.util.UUID;

/**
 * One active legal hold that may cover a subject's data, as privacy's ListActiveHolds answers it.
 *
 * @param id              the hold
 * @param scope           what it holds: {@link #SUBJECT}, {@link #CATEGORY} or {@link #RECORDS}
 * @param subjectPersonId the data principal held; null = everyone's (category and records holds)
 * @param categoryCode    the data category held (scope category); null otherwise
 * @param module          the module holding the records (scope records); null otherwise
 * @param entityType      the kind of record held (scope records), the table: {@code <module>.<table>}; null otherwise
 * @param entityIds       the records held (scope records); empty otherwise
 */
public record ActiveHold(UUID id, String scope, UUID subjectPersonId, String categoryCode, String module,
                         String entityType, Set<UUID> entityIds) {

    public static final String SUBJECT = "subject";
    public static final String CATEGORY = "category";
    public static final String RECORDS = "records";

    public ActiveHold {
        entityIds = Set.copyOf(entityIds);
    }
}
