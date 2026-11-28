package com.mysms.campus.facility.grpc;

import com.mysms.campus.facility.api.model.RoomUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateRoom over gRPC (JAVA-CODING-RULES §7.2): the {@code facility.v1.RoomUpdate} message as REST's {@link
 * RoomUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RoomProtoBridge {

    private RoomProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RoomUpdate changes(ProtoBridge bridge, com.mysms.facility.v1.RoomUpdate request) {
        RoomUpdate changes = new RoomUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setRoomType(bridge.choice(
                request.hasRoomType(), request.getRoomType(), RoomUpdate.RoomTypeEnum::fromValue));
        changes.setCapacity(ProtoBridge.number(request.hasCapacity(), request.getCapacity()));
        changes.setDefaultCourseId(bridge.uuid(
                request.hasDefaultCourseId(), request.getDefaultCourseId(), "default_course_id"));
        changes.setDefaultBatchId(bridge.uuid(
                request.hasDefaultBatchId(), request.getDefaultBatchId(), "default_batch_id"));
        changes.setDefaultClassSectionId(bridge.uuid(
                request.hasDefaultClassSectionId(), request.getDefaultClassSectionId(), "default_class_section_id"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
