package com.mysms.students.admission.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.admission.api.model.AdmissionCycleFeeUpdate;

/**
 * UpdateAdmissionCycleFee over gRPC (JAVA-CODING-RULES §7.2): the {@code admission.v1.AdmissionCycleFeeUpdate} message
 * as REST's {@link AdmissionCycleFeeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class AdmissionCycleFeeProtoBridge {

    private AdmissionCycleFeeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AdmissionCycleFeeUpdate changes(ProtoBridge bridge, com.mysms.admission.v1.AdmissionCycleFeeUpdate request) {
        AdmissionCycleFeeUpdate changes = new AdmissionCycleFeeUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setFeeHeadId(bridge.uuid(request.hasFeeHeadId(), request.getFeeHeadId(), "fee_head_id"));
        return bridge.patch(changes);
    }
}
