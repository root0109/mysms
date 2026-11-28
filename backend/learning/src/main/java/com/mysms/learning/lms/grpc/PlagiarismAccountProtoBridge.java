package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.PlagiarismAccountUpdate;

/**
 * UpdatePlagiarismAccount over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.PlagiarismAccountUpdate} message as
 * REST's {@link PlagiarismAccountUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class PlagiarismAccountProtoBridge {

    private PlagiarismAccountProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PlagiarismAccountUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.PlagiarismAccountUpdate request) {
        PlagiarismAccountUpdate changes = new PlagiarismAccountUpdate();
        changes.setProvider(ProtoBridge.text(request.hasProvider(), request.getProvider()));
        changes.setAccountRef(ProtoBridge.text(request.hasAccountRef(), request.getAccountRef()));
        changes.setSubAccountRef(ProtoBridge.text(request.hasSubAccountRef(), request.getSubAccountRef()));
        changes.setApiUrl(ProtoBridge.text(request.hasApiUrl(), request.getApiUrl()));
        changes.setSharedSecret(ProtoBridge.text(request.hasSharedSecret(), request.getSharedSecret()));
        changes.setIsDefault(ProtoBridge.flag(request.hasIsDefault(), request.getIsDefault()));
        changes.setDefaultTeacherUserId(bridge.uuid(
                request.hasDefaultTeacherUserId(), request.getDefaultTeacherUserId(), "default_teacher_user_id"));
        changes.setDefaultTeacherEmail(ProtoBridge.text(
                request.hasDefaultTeacherEmail(), request.getDefaultTeacherEmail()));
        changes.setDefaultTeacherFirstName(ProtoBridge.text(
                request.hasDefaultTeacherFirstName(), request.getDefaultTeacherFirstName()));
        changes.setDefaultTeacherLastName(ProtoBridge.text(
                request.hasDefaultTeacherLastName(), request.getDefaultTeacherLastName()));
        return bridge.patch(changes);
    }
}
