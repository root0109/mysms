package com.mysms.core.grpc;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.IdempotencyLedger;
import com.mysms.core.db.IdempotencyStore;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.Problem;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;

/**
 * Applies REST's Idempotency-Key rules to a {@code request_id} through the same {@link IdempotencyLedger}: a retry is
 * replayed, a reused key is {@code IDEMPOTENCY_KEY_REUSED}, a retry while the first runs is
 * {@code REQUEST_IN_PROGRESS}, and a server failure is not kept. Unary calls only; runs inside the auth interceptor.
 */
@GrpcGlobalServerInterceptor
@Order(GrpcEdge.IDEMPOTENCY_ORDER)
public final class GrpcIdempotencyInterceptor implements ServerInterceptor {

    public static final String REQUEST_ID = "request_id";
    public static final Metadata.Key<String> REPLAYED =
            Metadata.Key.of("idempotent-replayed", Metadata.ASCII_STRING_MARSHALLER);
    private static final String REQUIRED = "required";

    private final IdempotencyLedger ledger;
    private final GrpcStatuses statuses;

    public GrpcIdempotencyInterceptor(IdempotencyLedger ledger, GrpcStatuses statuses) {
        this.ledger = ledger;
        this.statuses = statuses;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String declared = GrpcEdge.IDEMPOTENCY.get();
        String operation = GrpcEdge.OPERATION_ID.get();
        if (declared == null || operation == null || call.getMethodDescriptor().getType()
                != MethodDescriptor.MethodType.UNARY) {
            return next.startCall(call, headers);
        }
        return new KeyedCall<>(call, headers, next, operation, REQUIRED.equals(declared));
    }

    /**
     * Holds the unary request until it is complete, claims its key, then runs, replays or refuses it (the request
     * message arrives before the decision can be made, so this listener starts the resolver itself).
     */
    private final class KeyedCall<ReqT, RespT> extends ServerCall.Listener<ReqT> {

        private final ServerCall<ReqT, RespT> call;
        private final Metadata headers;
        private final ServerCallHandler<ReqT, RespT> next;
        private final String operation;
        private final boolean required;
        private ReqT request;
        private ServerCall.Listener<ReqT> running;

        KeyedCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next, String operation,
                  boolean required) {
            this.call = call;
            this.headers = headers;
            this.next = next;
            this.operation = operation;
            this.required = required;
            call.request(1);
        }

        @Override
        public void onMessage(ReqT message) {
            request = message;
        }

        @Override
        public void onHalfClose() {
            Optional<Target> target = target();
            if (target.isEmpty()) {
                start(call).onMessage(request);
                running.onHalfClose();
                return;
            }
            Target t = target.get();
            IdempotencyLedger.Ticket ticket = ledger.claim(t.store(), operation, t.key(), t.tenant(), t.user(),
                    IdempotencyLedger.hash("GRPC\n" + call.getMethodDescriptor().getFullMethodName() + "\n" + t.user()
                            + "\n", withoutKey((Message) request)));
            switch (ticket.outcome()) {
                case MISMATCH -> refuse(new Problem(HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED",
                        "This request_id was used for a different request.", List.of()));
                case IN_PROGRESS -> refuse(new Problem(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                        "A request with this request_id is still running; retry shortly.", List.of()));
                case REPLAY -> replay(ticket);
                case RUN -> run(ticket);
                default -> throw new IllegalStateException("unknown outcome");
            }
        }

        @Override
        public void onCancel() {
            if (running != null) {
                running.onCancel();
            }
        }

        @Override
        public void onComplete() {
            if (running != null) {
                running.onComplete();
            }
        }

        @Override
        public void onReady() {
            if (running != null) {
                running.onReady();
            }
        }

        /** The key and the caller, or empty when the request runs without one (no key; no tenant). */
        private Optional<Target> target() {
            Descriptors.FieldDescriptor field = ((Message) request).getDescriptorForType().findFieldByName(REQUEST_ID);
            String value = field == null ? "" : String.valueOf(((Message) request).getField(field)).trim();
            if (value.isEmpty()) {
                if (required) {
                    throw new InvalidArgumentException("This operation needs a request_id.");
                }
                return Optional.empty();
            }
            UUID key;
            try {
                key = UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                throw new InvalidArgumentException("request_id must be a UUID.");
            }
            Optional<CallContext> context = CallContextHolder.current();
            if (context.isEmpty() || context.get().tenantId().isEmpty()) {
                return Optional.empty(); // public and platform-staff calls run as usual, as on REST
            }
            return ledger.storeFor(operation).map(store -> new Target(store, key, context.get().tenantId().get(),
                    context.get().userId().orElse(null)));
        }

        private ServerCall.Listener<ReqT> start(ServerCall<ReqT, RespT> target) {
            running = next.startCall(target, headers);
            return running;
        }

        private void run(IdempotencyLedger.Ticket ticket) {
            KeepingCall<ReqT, RespT> keeping = new KeepingCall<>(call, ticket);
            try {
                start(keeping).onMessage(request);
                running.onHalfClose();
            } catch (RuntimeException e) {
                // answered here, through the keeping call, so a refusal (409, 422...) is kept like on REST
                StatusRuntimeException answer = statuses.toException(statuses.problemOf(e));
                keeping.close(answer.getStatus(), answer.getTrailers());
            }
        }

        private void refuse(Problem problem) {
            StatusRuntimeException answer = statuses.toException(problem);
            call.close(answer.getStatus(), answer.getTrailers());
        }

        private void replay(IdempotencyLedger.Ticket ticket) {
            Map<String, Object> kept = ledger.kept(ticket);
            Metadata replayed = new Metadata();
            replayed.put(REPLAYED, "true");
            Object message = kept.get("message");
            if (message != null) {
                call.sendHeaders(replayed);
                call.sendMessage(call.getMethodDescriptor().parseResponse(
                        new ByteArrayInputStream(Base64.getDecoder().decode(String.valueOf(message)))));
                call.close(Status.OK, new Metadata());
                return;
            }
            Metadata trailers = new Metadata();
            trailers.merge(replayed);
            Object details = kept.get("details");
            if (details != null) {
                trailers.put(DETAILS, Base64.getDecoder().decode(String.valueOf(details)));
            }
            Object error = kept.get("error");
            if (error != null) {
                trailers.put(GrpcEdge.ERROR_MESSAGE, Base64.getDecoder().decode(String.valueOf(error)));
            }
            Object errorDetails = kept.get("errorDetails");
            if (errorDetails != null) {
                trailers.put(GrpcEdge.ERROR_DETAILS, String.valueOf(errorDetails));
            }
            Object description = kept.get("description");
            call.close(Status.fromCodeValue(((Number) kept.get("code")).intValue())
                    .withDescription(description == null ? null : String.valueOf(description)), trailers);
        }
    }

    /** The google.rpc.Status trailer gRPC carries error details in. */
    private static final Metadata.Key<byte[]> DETAILS =
            Metadata.Key.of("grpc-status-details-bin", Metadata.BINARY_BYTE_MARSHALLER);

    /** Keeps the answer of a call that ran under a claimed key, or frees the key when the server failed. */
    private final class KeepingCall<ReqT, RespT> extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

        private final IdempotencyLedger.Ticket ticket;
        private RespT response;
        private boolean closed;

        KeepingCall(ServerCall<ReqT, RespT> delegate, IdempotencyLedger.Ticket ticket) {
            super(delegate);
            this.ticket = ticket;
        }

        @Override
        public void sendMessage(RespT message) {
            response = message;
            super.sendMessage(message);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            if (closed) {
                return;
            }
            closed = true;
            if (status.getCode() == Status.Code.UNKNOWN && status.getCause() != null) {
                // a resolver's onError(e): answer it as the error interceptor would, and keep that answer
                StatusRuntimeException answer = statuses.toException(statuses.problemOf(status.getCause()));
                status = answer.getStatus();
                trailers = answer.getTrailers();
            }
            try {
                if (GrpcStatuses.retriable(status.getCode())) {
                    ledger.release(ticket);
                } else {
                    ledger.complete(ticket, httpStatus(status.getCode()), kept(status, trailers));
                }
            } finally {
                super.close(status, trailers); // the client gets its answer whatever happens to the kept copy
            }
        }

        private Map<String, Object> kept(Status status, Metadata trailers) {
            Map<String, Object> kept = new LinkedHashMap<>();
            if (status.isOk() && response != null) {
                kept.put("message", Base64.getEncoder().encodeToString(bytes(getMethodDescriptor().streamResponse(
                        response))));
                return kept;
            }
            kept.put("code", status.getCode().value());
            if (status.getDescription() != null) {
                kept.put("description", status.getDescription());
            }
            byte[] details = trailers == null ? null : trailers.get(DETAILS);
            if (details != null) {
                kept.put("details", Base64.getEncoder().encodeToString(details));
            }
            byte[] error = trailers == null ? null : trailers.get(GrpcEdge.ERROR_MESSAGE);
            if (error != null) {
                kept.put("error", Base64.getEncoder().encodeToString(error));
            }
            String errorDetails = trailers == null ? null : trailers.get(GrpcEdge.ERROR_DETAILS);
            if (errorDetails != null) {
                kept.put("errorDetails", errorDetails);
            }
            return kept;
        }
    }

    /** The request's bytes without its key, deterministically serialized: the key does not make two requests differ. */
    private static byte[] withoutKey(Message request) {
        Message.Builder builder = request.toBuilder();
        Descriptors.FieldDescriptor field = request.getDescriptorForType().findFieldByName(REQUEST_ID);
        if (field != null) {
            builder.clearField(field);
        }
        Message keyless = builder.build();
        byte[] bytes = new byte[keyless.getSerializedSize()];
        CodedOutputStream out = CodedOutputStream.newInstance(bytes);
        out.useDeterministicSerialization();
        try {
            keyless.writeTo(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes;
    }

    private static byte[] bytes(InputStream stream) {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The HTTP status kept in the store's {@code response_status} for a gRPC answer (informational). */
    static int httpStatus(Status.Code code) {
        return switch (code) {
            case OK -> HttpStatus.OK.value();
            case INVALID_ARGUMENT, OUT_OF_RANGE -> HttpStatus.BAD_REQUEST.value();
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED.value();
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN.value();
            case NOT_FOUND -> HttpStatus.NOT_FOUND.value();
            case ALREADY_EXISTS, FAILED_PRECONDITION -> HttpStatus.CONFLICT.value();
            case ABORTED -> HttpStatus.PRECONDITION_FAILED.value();
            case UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED.value();
            default -> HttpStatus.INTERNAL_SERVER_ERROR.value();
        };
    }

    private record Target(IdempotencyStore store, UUID key, UUID tenant, UUID user) {
    }
}
