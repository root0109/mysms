package com.mysms.comms.grpc;

import com.mysms.comms.api.model.TaskUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateTask over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.TaskUpdate} message
 * as REST's {@link TaskUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class TaskProtoBridge {

    private TaskProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TaskUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.TaskUpdate request) {
        TaskUpdate changes = new TaskUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setOwnerUserId(bridge.uuid(request.hasOwnerUserId(), request.getOwnerUserId(), "owner_user_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setTaskCategory(bridge.choice(
                request.hasTaskCategory(), request.getTaskCategory(), TaskUpdate.TaskCategoryEnum::fromValue));
        changes.setDueAt(ProtoBridge.time(request.hasDueAt(), request.getDueAt()));
        changes.setReminderOption(bridge.choice(
                request.hasReminderOption(), request.getReminderOption(), TaskUpdate.ReminderOptionEnum::fromValue));
        changes.setReminderAt(ProtoBridge.time(request.hasReminderAt(), request.getReminderAt()));
        changes.setChecklist(ProtoBridge.text(request.hasChecklist(), request.getChecklist()));
        changes.setPriority(bridge.choice(
                request.hasPriority(), request.getPriority(), TaskUpdate.PriorityEnum::fromValue));
        changes.setAssignmentMode(bridge.choice(
                request.hasAssignmentMode(), request.getAssignmentMode(), TaskUpdate.AssignmentModeEnum::fromValue));
        return bridge.patch(changes);
    }
}
