package com.mysms.privacy.model;

import java.util.List;
import java.util.UUID;

/**
 * What a rights request asks, as privacy.rights_request.details keeps it (jsonb, written and read by the request's
 * repository): the requester's own words and, typed per request type, what a correction names and how a grievance is
 * classed. A request of another type carries neither.
 *
 * @param description       the requester's description of what they ask, at most 4000 characters
 * @param correction        for a correction: the record and the fields to correct; null for any other type
 * @param grievanceCategory for a grievance: what it is about (consent, access, correction, data_security,
 *                          communication, other); null for any other type
 */
public record RightsRequestDetails(String description, Correction correction, String grievanceCategory) {

    /**
     * The record a correction is about, in the module that holds it: privacy never writes another module's data, so
     * the staff correct it there through that module's own operation.
     *
     * @param module     the module that holds the record (people, sis, hr …)
     * @param recordType the kind of record (person, student, staff …)
     * @param recordId   the record's id when the requester knows it; null otherwise
     * @param fields     the fields to correct, each with the value asked for
     */
    public record Correction(String module, String recordType, UUID recordId, List<Field> fields) {

        public Correction {
            fields = List.copyOf(fields);
        }
    }

    /**
     * One field a correction asks to change.
     *
     * @param field          the field as the requester names it
     * @param requestedValue the value asked for
     */
    public record Field(String field, String requestedValue) {
    }
}
