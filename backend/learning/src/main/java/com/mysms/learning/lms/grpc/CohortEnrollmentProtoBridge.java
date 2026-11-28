package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortEnrollmentUpdate;

/**
 * UpdateCohortEnrollment over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortEnrollmentUpdate} message as
 * REST's {@link CohortEnrollmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class CohortEnrollmentProtoBridge {

    private CohortEnrollmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortEnrollmentUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortEnrollmentUpdate request) {
        CohortEnrollmentUpdate changes = new CohortEnrollmentUpdate();
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setHasAccommodation(ProtoBridge.flag(request.hasHasAccommodation(), request.getHasAccommodation()));
        changes.setAccommodationRef(ProtoBridge.text(request.hasAccommodationRef(), request.getAccommodationRef()));
        changes.setSurveyStatus(ProtoBridge.number(request.hasSurveyStatus(), request.getSurveyStatus()));
        return bridge.patch(changes);
    }
}
