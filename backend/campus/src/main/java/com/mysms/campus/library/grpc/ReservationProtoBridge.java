package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.ReservationUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateReservation over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.ReservationUpdate} message as REST's
 * {@link ReservationUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ReservationProtoBridge {

    private ReservationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ReservationUpdate changes(ProtoBridge bridge, com.mysms.library.v1.ReservationUpdate request) {
        ReservationUpdate changes = new ReservationUpdate();
        changes.setMediaId(bridge.uuid(request.hasMediaId(), request.getMediaId(), "media_id"));
        changes.setReference(ProtoBridge.text(request.hasReference(), request.getReference()));
        changes.setRequestedByPersonId(bridge.uuid(
                request.hasRequestedByPersonId(), request.getRequestedByPersonId(), "requested_by_person_id"));
        changes.setDateFrom(bridge.date(request.hasDateFrom(), request.getDateFrom(), "date_from"));
        changes.setDateTo(bridge.date(request.hasDateTo(), request.getDateTo(), "date_to"));
        return bridge.patch(changes);
    }
}
