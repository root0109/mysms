package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.PurchaseRequestCourseUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePurchaseRequestCourse over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.PurchaseRequestCourseUpdate}
 * message as REST's {@link PurchaseRequestCourseUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PurchaseRequestCourseProtoBridge {

    private PurchaseRequestCourseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PurchaseRequestCourseUpdate changes(
            ProtoBridge bridge, com.mysms.library.v1.PurchaseRequestCourseUpdate request) {
        PurchaseRequestCourseUpdate changes = new PurchaseRequestCourseUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        return bridge.patch(changes);
    }
}
