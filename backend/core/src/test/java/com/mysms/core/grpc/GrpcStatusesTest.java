package com.mysms.core.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.PreconditionFailure;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.PreconditionRequiredException;
import com.mysms.core.error.Problem;
import com.mysms.core.error.ValidationFailedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

/** The HTTP ↔ gRPC status table and the google.rpc details, problem by problem. */
class GrpcStatusesTest {

    private final GrpcStatuses statuses = new GrpcStatuses(new ConstraintMapper(MessageUtil.ofCatalogue()));

    @AfterEach
    void clear() {
        CallContextHolder.clear();
    }

    @Test
    void mapsEveryStatusOfTheTable() {
        assertThat(GrpcStatuses.code(HttpStatus.BAD_REQUEST, "INVALID_FILTER")).isEqualTo(Code.INVALID_ARGUMENT);
        assertThat(GrpcStatuses.code(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED")).isEqualTo(Code.INVALID_ARGUMENT);
        assertThat(GrpcStatuses.code(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED")).isEqualTo(Code.UNAUTHENTICATED);
        assertThat(GrpcStatuses.code(HttpStatus.FORBIDDEN, "PERMISSION_DENIED")).isEqualTo(Code.PERMISSION_DENIED);
        assertThat(GrpcStatuses.code(HttpStatus.NOT_FOUND, "NOT_FOUND")).isEqualTo(Code.NOT_FOUND);
        assertThat(GrpcStatuses.code(HttpStatus.CONFLICT, "DUPLICATE")).isEqualTo(Code.ALREADY_EXISTS);
        assertThat(GrpcStatuses.code(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION")).isEqualTo(Code.FAILED_PRECONDITION);
        assertThat(GrpcStatuses.code(HttpStatus.PRECONDITION_FAILED, "ETAG_MISMATCH")).isEqualTo(Code.ABORTED);
        assertThat(GrpcStatuses.code(HttpStatus.PRECONDITION_FAILED, "CONSENT_REQUIRED")).isEqualTo(Code.FAILED_PRECONDITION);
        assertThat(GrpcStatuses.code(HttpStatus.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED"))
                .isEqualTo(Code.FAILED_PRECONDITION);
        assertThat(GrpcStatuses.code(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED")).isEqualTo(Code.RESOURCE_EXHAUSTED);
        assertThat(GrpcStatuses.code(HttpStatus.PAYLOAD_TOO_LARGE, "INVALID_ARGUMENT")).isEqualTo(Code.RESOURCE_EXHAUSTED);
        assertThat(GrpcStatuses.code(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED")).isEqualTo(Code.UNIMPLEMENTED);
        assertThat(GrpcStatuses.code(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE")).isEqualTo(Code.UNAVAILABLE);
        assertThat(GrpcStatuses.code(HttpStatus.GATEWAY_TIMEOUT, "X")).isEqualTo(Code.DEADLINE_EXCEEDED);
        assertThat(GrpcStatuses.code(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL")).isEqualTo(Code.INTERNAL);
        assertThat(GrpcStatuses.code(HttpStatus.METHOD_NOT_ALLOWED, "X")).isEqualTo(Code.INVALID_ARGUMENT);
        assertThat(GrpcStatuses.retriable(Status.Code.UNAVAILABLE)).isTrue();
        assertThat(GrpcStatuses.retriable(Status.Code.ALREADY_EXISTS)).isFalse();
    }

    @Test
    void carriesTheCodeTheFieldErrorsAndThePrecondition() throws Exception {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        StatusRuntimeException invalid = statuses.toException(statuses.problemOf(new ValidationFailedException(
                List.of(new FieldError("name", "must not be null", "NotNull")))));
        assertThat(invalid.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(invalid.getStatus().getDescription()).isEqualTo("The request has invalid fields.");
        com.google.rpc.Status rpc = StatusProto.fromThrowable(invalid);
        ErrorInfo info = rpc.getDetails(0).unpack(ErrorInfo.class);
        assertThat(info.getReason()).isEqualTo("VALIDATION_FAILED");
        assertThat(info.getDomain()).isEqualTo(GrpcStatuses.DOMAIN);
        assertThat(info.getMetadataMap()).containsEntry("status", "422")
                .containsEntry("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        BadRequest bad = rpc.getDetails(1).unpack(BadRequest.class);
        assertThat(bad.getFieldViolations(0).getField()).isEqualTo("name");
        assertThat(bad.getFieldViolations(0).getReason()).isEqualTo("NotNull");

        StatusRuntimeException stale = statuses.toException(statuses.problemOf(new PreconditionFailedException("Stale.")));
        assertThat(stale.getStatus().getCode()).isEqualTo(Status.Code.ABORTED);
        PreconditionFailure failure = detail(stale, PreconditionFailure.class);
        assertThat(failure.getViolations(0).getType()).isEqualTo("ETAG_MISMATCH");
        StatusRuntimeException missing = statuses.toException(statuses.problemOf(
                new PreconditionRequiredException("Send the ETag.")));
        assertThat(missing.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(detail(missing, PreconditionFailure.class).getViolations(0).getType())
                .isEqualTo("PRECONDITION_REQUIRED");
        assertThat(statuses.toException(statuses.problemOf(new ConflictException("SETTING_LOCKED", "Locked.")))
                .getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(statuses.toException(statuses.problemOf(new DuplicateResourceException("Taken."))).getStatus()
                .getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
    }

    @Test
    void mapsIntegrityViolationsAndHidesEverythingElse() {
        assertThat(statuses.problemOf(new DuplicateKeyException("uq")).code()).isEqualTo("DUPLICATE");
        assertThat(statuses.problemOf(new DataIntegrityViolationException("row values")).status())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        Problem internal = statuses.problemOf(new IllegalStateException("password=hunter2"));
        assertThat(internal.code()).isEqualTo("INTERNAL");
        StatusRuntimeException answer = statuses.toException(internal);
        assertThat(answer.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(String.valueOf(answer.getStatus().getDescription())).doesNotContain("hunter2");

        Problem unwrapped = statuses.problemOf(Status.NOT_FOUND.withDescription("student 42 of tenant 7")
                .asRuntimeException());
        assertThat(unwrapped.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(unwrapped.code()).isEqualTo("UNAVAILABLE");
        assertThat(unwrapped.detail()).isEqualTo(GrpcStatuses.UNANSWERED).doesNotContain("student");
        assertThat(statuses.toException(unwrapped).getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    private static <T extends com.google.protobuf.Message> T detail(StatusRuntimeException e, Class<T> type)
            throws Exception {
        for (Any any : StatusProto.fromThrowable(e).getDetailsList()) {
            if (any.is(type)) {
                return any.unpack(type);
            }
        }
        throw new AssertionError("no " + type.getSimpleName());
    }
}
