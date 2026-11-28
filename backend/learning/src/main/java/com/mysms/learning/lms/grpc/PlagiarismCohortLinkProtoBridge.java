package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.PlagiarismCohortLinkUpdate;

/**
 * UpdatePlagiarismCohortLink over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.PlagiarismCohortLinkUpdate} message
 * as REST's {@link PlagiarismCohortLinkUpdate} body, one explicit line per field: unset keeps the value, null clears
 * it, a value sets it.
 */
final class PlagiarismCohortLinkProtoBridge {

    private PlagiarismCohortLinkProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PlagiarismCohortLinkUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.PlagiarismCohortLinkUpdate request) {
        PlagiarismCohortLinkUpdate changes = new PlagiarismCohortLinkUpdate();
        changes.setPlagiarismAccountId(bridge.uuid(
                request.hasPlagiarismAccountId(), request.getPlagiarismAccountId(), "plagiarism_account_id"));
        changes.setAccountRef(ProtoBridge.text(request.hasAccountRef(), request.getAccountRef()));
        changes.setProviderClassRef(ProtoBridge.text(request.hasProviderClassRef(), request.getProviderClassRef()));
        changes.setProviderClassTitle(ProtoBridge.text(
                request.hasProviderClassTitle(), request.getProviderClassTitle()));
        changes.setProviderClassEndAt(ProtoBridge.time(
                request.hasProviderClassEndAt(), request.getProviderClassEndAt()));
        changes.setProviderAssignmentRef(ProtoBridge.text(
                request.hasProviderAssignmentRef(), request.getProviderAssignmentRef()));
        changes.setProviderAssignmentTitle(ProtoBridge.text(
                request.hasProviderAssignmentTitle(), request.getProviderAssignmentTitle()));
        changes.setProviderAssignmentStartAt(ProtoBridge.time(
                request.hasProviderAssignmentStartAt(), request.getProviderAssignmentStartAt()));
        changes.setProviderAssignmentDueAt(ProtoBridge.time(
                request.hasProviderAssignmentDueAt(), request.getProviderAssignmentDueAt()));
        changes.setProviderAssignmentInstructions(ProtoBridge.text(
                request.hasProviderAssignmentInstructions(), request.getProviderAssignmentInstructions()));
        changes.setStudentReportAccess(ProtoBridge.flag(
                request.hasStudentReportAccess(), request.getStudentReportAccess()));
        changes.setTeacherEmail(ProtoBridge.text(request.hasTeacherEmail(), request.getTeacherEmail()));
        changes.setTeacherFirstName(ProtoBridge.text(request.hasTeacherFirstName(), request.getTeacherFirstName()));
        changes.setTeacherLastName(ProtoBridge.text(request.hasTeacherLastName(), request.getTeacherLastName()));
        changes.setClassCreated(ProtoBridge.flag(request.hasClassCreated(), request.getClassCreated()));
        changes.setAssignmentCreated(ProtoBridge.flag(request.hasAssignmentCreated(), request.getAssignmentCreated()));
        return bridge.patch(changes);
    }
}
