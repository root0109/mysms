package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.EvaluatorAvailabilityUpdate;

/**
 * UpdateEvaluatorAvailability over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.EvaluatorAvailabilityUpdate}
 * message as REST's {@link EvaluatorAvailabilityUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EvaluatorAvailabilityProtoBridge {

    private EvaluatorAvailabilityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EvaluatorAvailabilityUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.EvaluatorAvailabilityUpdate request) {
        EvaluatorAvailabilityUpdate changes = new EvaluatorAvailabilityUpdate();
        changes.setWeekday(bridge.choice(
                request.hasWeekday(), request.getWeekday(), EvaluatorAvailabilityUpdate.WeekdayEnum::fromValue));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        return bridge.patch(changes);
    }
}
