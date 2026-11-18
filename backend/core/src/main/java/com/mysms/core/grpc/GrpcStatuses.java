package com.mysms.core.grpc;

import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.PreconditionFailure;
import com.mysms.common.v1.ErrorField;
import com.mysms.common.v1.ErrorMessage;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.IntegrityViolations;
import com.mysms.core.error.Problem;
import com.mysms.core.error.UnavailableException;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

/**
 * Turns a {@link Problem} — what REST writes as problem+json — into a {@code google.rpc.Status} with the matching gRPC
 * code, {@code ErrorInfo}, {@code BadRequest} and {@code PreconditionFailure} details, and the
 * {@code error-message-bin} / {@code error-details} trailers MySMS clients unwrap. The message is always a sentence
 * the code wrote, never an exception's message.
 */
public final class GrpcStatuses {

    public static final String DOMAIN = "mysms";
    private static final Logger LOG = LoggerFactory.getLogger(GrpcStatuses.class);

    private final ConstraintMapper constraints;

    public GrpcStatuses(ConstraintMapper constraints) {
        this.constraints = constraints;
    }

    /** The detail of a 503 for another service's status that escaped a stub call. */
    public static final String UNANSWERED = "A service this operation needs could not answer.";

    /**
     * What a failure means to the client: a typed exception keeps its status and code (field errors in the caller's
     * language, as on REST), an integrity violation maps as on REST, a gRPC status that escaped a stub call made
     * without {@code IntegrationGrpcClientException.call} is 503 UNAVAILABLE without anything of the callee's answer,
     * and anything else is logged and answered INTERNAL.
     */
    public Problem problemOf(Throwable failure) {
        if (failure instanceof StatusRuntimeException unwrapped) {
            LOG.warn("a call to another service failed with {} and was not unwrapped",
                    unwrapped.getStatus().getCode());
            return Problem.of(new UnavailableException(UNANSWERED));
        }
        if (failure instanceof ApiException api) {
            Problem problem = Problem.of(api);
            return new Problem(problem.status(), problem.code(), problem.detail(),
                    problem.errors().stream().map(constraints::localized).toList());
        }
        if (failure instanceof DataIntegrityViolationException integrity) {
            return IntegrityViolations.problem(integrity);
        }
        LOG.error("unhandled exception", failure);
        return Problem.internal();
    }

    /** The status (with its details in the trailers) that answers the problem. */
    public StatusRuntimeException toException(Problem problem) {
        HttpStatus status = problem.status();
        String traceId = CallContextHolder.current().map(CallContext::traceId).orElse(null);
        com.google.rpc.Status.Builder rpc = com.google.rpc.Status.newBuilder()
                .setCode(code(status, problem.code()).getNumber());
        if (problem.detail() != null && !problem.detail().isBlank()) {
            rpc.setMessage(problem.detail());
        }
        ErrorInfo.Builder info = ErrorInfo.newBuilder().setReason(problem.code()).setDomain(DOMAIN)
                .putMetadata("status", Integer.toString(status.value()));
        if (traceId != null) {
            info.putMetadata("traceId", traceId);
        }
        rpc.addDetails(Any.pack(info.build()));
        if (!problem.errors().isEmpty()) {
            BadRequest.Builder bad = BadRequest.newBuilder();
            for (FieldError error : problem.errors()) {
                BadRequest.FieldViolation.Builder violation = BadRequest.FieldViolation.newBuilder()
                        .setField(error.field()).setDescription(error.message() == null ? "" : error.message());
                if (error.code() != null) {
                    violation.setReason(error.code());
                }
                bad.addFieldViolations(violation);
            }
            rpc.addDetails(Any.pack(bad.build()));
        }
        if (status == HttpStatus.PRECONDITION_FAILED || status == HttpStatus.PRECONDITION_REQUIRED) {
            rpc.addDetails(Any.pack(PreconditionFailure.newBuilder().addViolations(PreconditionFailure.Violation
                    .newBuilder().setType(problem.code()).setSubject("etag")
                    .setDescription(problem.detail() == null ? "" : problem.detail())).build()));
        }
        return StatusProto.toStatusRuntimeException(rpc.build(), errorTrailers(problem, traceId));
    }

    /** The error trailers of a refusal: the problem packed, and as ASCII text. */
    static Metadata errorTrailers(Problem problem, String traceId) {
        ErrorMessage.Builder message = ErrorMessage.newBuilder().setCode(problem.code())
                .setStatus(problem.status().value());
        if (problem.detail() != null) {
            message.setDetail(problem.detail());
        }
        if (traceId != null) {
            message.setTraceId(traceId);
        }
        for (FieldError error : problem.errors()) {
            ErrorField.Builder field = ErrorField.newBuilder().setField(error.field());
            if (error.message() != null) {
                field.setMessage(error.message());
            }
            if (error.code() != null) {
                field.setCode(error.code());
            }
            message.addErrors(field);
        }
        Metadata trailers = new Metadata();
        trailers.put(GrpcEdge.ERROR_MESSAGE, Any.pack(message.build()).toByteArray());
        trailers.put(GrpcEdge.ERROR_DETAILS, ascii(problem.code() + " " + problem.status().value()
                + (problem.detail() == null ? "" : ": " + problem.detail())));
        return trailers;
    }

    /** ASCII metadata carries printable ASCII only: the detail's other characters (any language) become '?'. */
    private static String ascii(String text) {
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().forEach(c -> out.append(c >= 0x20 && c < 0x7f ? (char) c : '?'));
        return out.toString();
    }

    /** The HTTP status's gRPC code. */
    public static Code code(HttpStatus status, String code) {
        return switch (status.value()) {
            case 400, 422 -> Code.INVALID_ARGUMENT;
            case 401 -> Code.UNAUTHENTICATED;
            case 403 -> Code.PERMISSION_DENIED;
            case 404 -> Code.NOT_FOUND;
            case 409 -> "DUPLICATE".equals(code) ? Code.ALREADY_EXISTS : Code.FAILED_PRECONDITION;
            case 412 -> "ETAG_MISMATCH".equals(code) ? Code.ABORTED : Code.FAILED_PRECONDITION;
            case 428 -> Code.FAILED_PRECONDITION;
            case 413, 429 -> Code.RESOURCE_EXHAUSTED;
            case 501 -> Code.UNIMPLEMENTED;
            case 503 -> Code.UNAVAILABLE;
            case 504 -> Code.DEADLINE_EXCEEDED;
            default -> status.is5xxServerError() ? Code.INTERNAL : Code.INVALID_ARGUMENT;
        };
    }

    /**
     * Whether a call that ended with this code failed on the server or asked to be retried (500, 503, 504, 429 on
     * REST), so its idempotency key is freed rather than its answer kept.
     */
    static boolean retriable(Status.Code code) {
        return switch (code) {
            case UNKNOWN, INTERNAL, UNAVAILABLE, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED, DATA_LOSS, CANCELLED -> true;
            default -> false;
        };
    }
}
