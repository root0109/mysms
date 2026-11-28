package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CertificateEvaluationUpdate;

/**
 * UpdateCertificateEvaluation over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CertificateEvaluationUpdate}
 * message as REST's {@link CertificateEvaluationUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CertificateEvaluationProtoBridge {

    private CertificateEvaluationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CertificateEvaluationUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.CertificateEvaluationUpdate request) {
        CertificateEvaluationUpdate changes = new CertificateEvaluationUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setCertificateRequestId(bridge.uuid(
                request.hasCertificateRequestId(), request.getCertificateRequestId(), "certificate_request_id"));
        changes.setEvaluatorUserId(bridge.uuid(
                request.hasEvaluatorUserId(), request.getEvaluatorUserId(), "evaluator_user_id"));
        changes.setEvaluationDate(bridge.date(
                request.hasEvaluationDate(), request.getEvaluationDate(), "evaluation_date"));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        changes.setRating(ProtoBridge.text(request.hasRating(), request.getRating()));
        changes.setSummary(ProtoBridge.text(request.hasSummary(), request.getSummary()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), CertificateEvaluationUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
