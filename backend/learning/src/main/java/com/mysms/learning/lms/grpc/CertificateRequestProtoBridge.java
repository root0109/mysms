package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CertificateRequestUpdate;

/**
 * UpdateCertificateRequest over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CertificateRequestUpdate} message as
 * REST's {@link CertificateRequestUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class CertificateRequestProtoBridge {

    private CertificateRequestProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CertificateRequestUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CertificateRequestUpdate request) {
        CertificateRequestUpdate changes = new CertificateRequestUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setEvaluatorUserId(bridge.uuid(
                request.hasEvaluatorUserId(), request.getEvaluatorUserId(), "evaluator_user_id"));
        changes.setRequestDate(bridge.date(request.hasRequestDate(), request.getRequestDate(), "request_date"));
        changes.setWeekday(bridge.choice(
                request.hasWeekday(), request.getWeekday(), CertificateRequestUpdate.WeekdayEnum::fromValue));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        changes.setTimezone(ProtoBridge.text(request.hasTimezone(), request.getTimezone()));
        changes.setMeetingLink(ProtoBridge.text(request.hasMeetingLink(), request.getMeetingLink()));
        return bridge.patch(changes);
    }
}
