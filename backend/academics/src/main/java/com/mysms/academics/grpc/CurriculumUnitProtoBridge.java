package com.mysms.academics.grpc;

import com.mysms.academics.api.model.CurriculumUnitUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateCurriculumUnit over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.CurriculumUnitUpdate} message as
 * REST's {@link CurriculumUnitUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class CurriculumUnitProtoBridge {

    private CurriculumUnitProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CurriculumUnitUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.CurriculumUnitUpdate request) {
        CurriculumUnitUpdate changes = new CurriculumUnitUpdate();
        changes.setClassSubjectId(bridge.uuid(
                request.hasClassSubjectId(), request.getClassSubjectId(), "class_subject_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setSequence(ProtoBridge.number(request.hasSequence(), request.getSequence()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), CurriculumUnitUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
