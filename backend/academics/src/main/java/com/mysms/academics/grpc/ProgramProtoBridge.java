package com.mysms.academics.grpc;

import com.mysms.academics.api.model.ProgramUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateProgram over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.ProgramUpdate} message as REST's {@link
 * ProgramUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ProgramProtoBridge {

    private ProgramProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ProgramUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.ProgramUpdate request) {
        ProgramUpdate changes = new ProgramUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setDepartmentId(bridge.uuid(request.hasDepartmentId(), request.getDepartmentId(), "department_id"));
        changes.setProgramLevelId(bridge.uuid(
                request.hasProgramLevelId(), request.getProgramLevelId(), "program_level_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setMinUnitLoad(ProtoBridge.text(request.hasMinUnitLoad(), request.getMinUnitLoad()));
        changes.setMaxUnitLoad(ProtoBridge.text(request.hasMaxUnitLoad(), request.getMaxUnitLoad()));
        changes.setImageFileId(bridge.uuid(request.hasImageFileId(), request.getImageFileId(), "image_file_id"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
