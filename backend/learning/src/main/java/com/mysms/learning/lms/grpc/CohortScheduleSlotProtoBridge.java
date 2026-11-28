package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortScheduleSlotUpdate;

/**
 * UpdateCohortScheduleSlot over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortScheduleSlotUpdate} message as
 * REST's {@link CohortScheduleSlotUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class CohortScheduleSlotProtoBridge {

    private CohortScheduleSlotProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortScheduleSlotUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortScheduleSlotUpdate request) {
        CohortScheduleSlotUpdate changes = new CohortScheduleSlotUpdate();
        changes.setSlotType(bridge.choice(
                request.hasSlotType(), request.getSlotType(), CohortScheduleSlotUpdate.SlotTypeEnum::fromValue));
        changes.setCohortTeacherId(bridge.uuid(
                request.hasCohortTeacherId(), request.getCohortTeacherId(), "cohort_teacher_id"));
        changes.setWeekday(bridge.choice(
                request.hasWeekday(), request.getWeekday(), CohortScheduleSlotUpdate.WeekdayEnum::fromValue));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        changes.setVenue(ProtoBridge.text(request.hasVenue(), request.getVenue()));
        return bridge.patch(changes);
    }
}
