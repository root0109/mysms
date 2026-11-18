package com.mysms.core.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.common.v1.ErrorMessage;
import com.google.protobuf.Any;
import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.error.Problem;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * The client side of a refused call: what the server's error trailers carry ({@link GrpcStatuses}) is what a
 * {@code <Callee>GrpcClient} unwraps ({@link IntegrationGrpcClientException}), and a call that never reached a MySMS
 * server is still a typed failure.
 */
class IntegrationGrpcClientExceptionTest {

    private static final GrpcStatuses STATUSES = new GrpcStatuses(new ConstraintMapper(MessageUtil.ofCatalogue()));

    @Test
    void aRefusalCarriesTheReferenceTrailersNextToTheGoogleRpcDetails() throws Exception {
        StatusRuntimeException refused = STATUSES.toException(new Problem(HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_FAILED", "Fix the fields — ✓ 名前.", List.of(new FieldError("name", "too long", "Size"))));

        Metadata trailers = refused.getTrailers();
        ErrorMessage message = Any.parseFrom(trailers.get(GrpcEdge.ERROR_MESSAGE)).unpack(ErrorMessage.class);
        assertThat(message.getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(message.getStatus()).isEqualTo(422);
        assertThat(message.getErrors(0).getField()).isEqualTo("name");
        assertThat(message.getErrors(0).getCode()).isEqualTo("Size");
        assertThat(trailers.get(GrpcEdge.ERROR_DETAILS)).as("ASCII only")
                .isEqualTo("VALIDATION_FAILED 422: Fix the fields ? ? ??.");
        assertThat(refused.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void theClientUnwrapsTheCalleesRefusal() {
        IntegrationGrpcClientException e = IntegrationGrpcClientException.of("sis", STATUSES.toException(
                new Problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such student.", List.of())));

        assertThat(e.service()).isEqualTo("sis");
        assertThat(e.statusCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(e.errorCode()).isEqualTo("NOT_FOUND");
        assertThat(e.httpStatus()).isEqualTo(404);
        assertThat(e.calleeErrors()).isEmpty();
        assertThat(e.status()).as("unmapped, it is this call's 503").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(e.getMessage()).isEqualTo("The sis service could not answer.").doesNotContain("No such");

        IntegrationGrpcClientException fields = IntegrationGrpcClientException.of("people", STATUSES.toException(
                new Problem(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", null,
                        List.of(new FieldError("dateOfBirth", "in the future", null)))));
        assertThat(fields.calleeErrors()).containsExactly(new FieldError("dateOfBirth", "in the future", null));
    }

    @Test
    void aFailureWithoutAMySmsRefusalIsReadFromItsStatus() {
        assertThat(IntegrationGrpcClientException.of("org", Status.UNAVAILABLE.asRuntimeException()).httpStatus())
                .isEqualTo(503);
        assertThat(IntegrationGrpcClientException.of("org", Status.DEADLINE_EXCEEDED.asRuntimeException()))
                .satisfies(e -> {
                    assertThat(e.httpStatus()).isEqualTo(503);
                    assertThat(e.errorCode()).isEqualTo("DEADLINE_EXCEEDED");
                });
        assertThat(IntegrationGrpcClientException.of("org", Status.PERMISSION_DENIED.asRuntimeException())
                .httpStatus()).isEqualTo(403);
        assertThat(IntegrationGrpcClientException.of("org", Status.NOT_FOUND.asRuntimeException()).httpStatus())
                .isEqualTo(404);
        assertThat(IntegrationGrpcClientException.of("org", Status.INVALID_ARGUMENT.asRuntimeException())
                .httpStatus()).isEqualTo(400);
        assertThat(IntegrationGrpcClientException.of("org", Status.UNAUTHENTICATED.asRuntimeException())
                .httpStatus()).isEqualTo(401);
        assertThat(IntegrationGrpcClientException.of("org", Status.FAILED_PRECONDITION.asRuntimeException())
                .httpStatus()).isEqualTo(409);
        assertThat(IntegrationGrpcClientException.of("org", Status.ABORTED.asRuntimeException()).httpStatus())
                .isEqualTo(412);
        assertThat(IntegrationGrpcClientException.of("org", Status.RESOURCE_EXHAUSTED.asRuntimeException())
                .httpStatus()).isEqualTo(429);
        assertThat(IntegrationGrpcClientException.of("org", Status.UNIMPLEMENTED.asRuntimeException()).httpStatus())
                .isEqualTo(501);

        Metadata foreign = new Metadata();
        foreign.put(GrpcEdge.ERROR_MESSAGE, new byte[] {1, 2, 3});
        assertThat(IntegrationGrpcClientException.of("org", Status.INTERNAL.asRuntimeException(foreign)).errorCode())
                .as("a trailer that is not ours").isEqualTo("INTERNAL");
    }

    @Test
    void aStubCallAnswersOrThrowsTheIntegrationException() {
        assertThat(IntegrationGrpcClientException.call("org", () -> "answer")).isEqualTo("answer");
        assertThatThrownBy(() -> IntegrationGrpcClientException.call("org", () -> {
            throw Status.UNAVAILABLE.asRuntimeException();
        })).isInstanceOfSatisfying(IntegrationGrpcClientException.class,
                e -> assertThat(e.statusCode()).isEqualTo(Status.Code.UNAVAILABLE));
    }
}
