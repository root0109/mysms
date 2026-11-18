package com.mysms.core.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mysms.common.v1.ErrorField;
import com.mysms.common.v1.ErrorMessage;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.FieldError;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;

/**
 * A failed call to another module's gRPC service, with the callee's refusal from its {@code error-message-bin}
 * trailer. Callers map it onward by {@link #httpStatus()}; one nobody maps is a 503 that never shows the callee's
 * code, sentence or fields to this call's client.
 */
public final class IntegrationGrpcClientException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final String service;
    private final Status.Code statusCode;
    private final String errorCode;
    private final int httpStatus;
    private final transient List<FieldError> calleeErrors;

    private IntegrationGrpcClientException(String service, Status.Code statusCode, String errorCode, int httpStatus,
                                           List<FieldError> calleeErrors, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE", "The " + service + " service could not answer.");
        initCause(cause);
        this.service = service;
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.calleeErrors = List.copyOf(calleeErrors);
    }

    /**
     * One blocking stub call to {@code service}: its answer, or its failure as this exception.
     *
     * @param service the module called ({@code sis}), named in the 503 sentence and the log
     */
    public static <T> T call(String service, Supplier<T> stubCall) {
        try {
            return stubCall.get();
        } catch (StatusRuntimeException failure) {
            throw of(service, failure);
        }
    }

    /**
     * The failure of a call to {@code service}, read from the callee's {@code error-message-bin}; without one (the
     * callee unreachable, a deadline) the code is the gRPC code's name and the HTTP status its usual pairing.
     */
    public static IntegrationGrpcClientException of(String service, Throwable failure) {
        Status status = Status.fromThrowable(failure);
        Optional<ErrorMessage> error = errorMessage(Status.trailersFromThrowable(failure));
        if (error.isPresent()) {
            ErrorMessage message = error.get();
            List<FieldError> fields = message.getErrorsList().stream().map(IntegrationGrpcClientException::field)
                    .toList();
            return new IntegrationGrpcClientException(service, status.getCode(), message.getCode(), message.getStatus(),
                    fields, failure);
        }
        return new IntegrationGrpcClientException(service, status.getCode(), status.getCode().name(),
                httpStatus(status.getCode()), List.of(), failure);
    }

    /** The module that was called ({@code sis}). */
    public String service() {
        return service;
    }

    /** The gRPC status code of the call. */
    public Status.Code statusCode() {
        return statusCode;
    }

    /** The callee's problem code ({@code NOT_FOUND}, {@code VALIDATION_FAILED} …), or the gRPC code's name. */
    public String errorCode() {
        return errorCode;
    }

    /** The HTTP status REST answers the callee's refusal with (503 for a callee that could not be reached). */
    public int httpStatus() {
        return httpStatus;
    }

    /** The fields the callee refused (its names, for the caller to map to its own request's fields). */
    public List<FieldError> calleeErrors() {
        return calleeErrors;
    }

    private static Optional<ErrorMessage> errorMessage(Metadata trailers) {
        byte[] bytes = trailers == null ? null : trailers.get(GrpcEdge.ERROR_MESSAGE);
        if (bytes == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Any.parseFrom(bytes).unpack(ErrorMessage.class));
        } catch (InvalidProtocolBufferException notOurs) {
            return Optional.empty();
        }
    }

    private static FieldError field(ErrorField field) {
        return new FieldError(field.getField(), field.getMessage(), field.getCode().isEmpty() ? null : field.getCode());
    }

    /** The gRPC → HTTP status table, for a failure that carries no MySMS problem. */
    private static int httpStatus(Status.Code code) {
        return switch (code) {
            case INVALID_ARGUMENT, OUT_OF_RANGE -> HttpStatus.BAD_REQUEST.value();
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED.value();
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN.value();
            case NOT_FOUND -> HttpStatus.NOT_FOUND.value();
            case ALREADY_EXISTS, FAILED_PRECONDITION -> HttpStatus.CONFLICT.value();
            case ABORTED -> HttpStatus.PRECONDITION_FAILED.value();
            case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS.value();
            case UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED.value();
            // unreachable, no answer in time, or failed on its side: the dependency is unavailable
            default -> HttpStatus.SERVICE_UNAVAILABLE.value();
        };
    }
}
