package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.grpc.GrpcEdge;
import com.mysms.core.trace.TraceContext;
import com.mysms.core.trace.TraceHolder;
import io.grpc.CallCredentials;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class GrpcCallerInterceptorTest {

    private static final MethodDescriptor.Marshaller<String> TEXT = new MethodDescriptor.Marshaller<>() {
        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            return "";
        }
    };
    private static final MethodDescriptor<String, String> METHOD = MethodDescriptor.<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY).setFullMethodName("mysms.sis.v1.StudentService/GetStudent")
            .setRequestMarshaller(TEXT).setResponseMarshaller(TEXT).build();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID INSTITUTION = UUID.randomUUID();

    @AfterEach
    void clear() {
        AuthContextHolder.clear();
        CallContextHolder.clear();
        TraceHolder.clear();
    }

    /** The metadata a call through the interceptor starts with (and, in {@code options}, the options it went out with). */
    static Metadata sent(GrpcCallerInterceptor interceptor, CallOptions callOptions) {
        return sent(interceptor, callOptions, new AtomicReference<>());
    }

    private static Metadata sent(GrpcCallerInterceptor interceptor, CallOptions callOptions,
                                 AtomicReference<CallOptions> options) {
        AtomicReference<Metadata> headers = new AtomicReference<>();
        Channel channel = new Channel() {
            @Override
            public <Q, R> ClientCall<Q, R> newCall(MethodDescriptor<Q, R> method, CallOptions given) {
                options.set(given);
                return new ClientCall<>() {
                    @Override
                    public void start(Listener<R> listener, Metadata metadata) {
                        headers.set(metadata);
                    }

                    @Override
                    public void request(int numMessages) {
                    }

                    @Override
                    public void cancel(String message, Throwable cause) {
                    }

                    @Override
                    public void halfClose() {
                    }

                    @Override
                    public void sendMessage(Q message) {
                    }
                };
            }

            @Override
            public String authority() {
                return "sis";
            }
        };
        interceptor.interceptCall(METHOD, callOptions, channel).start(new ClientCall.Listener<>() { }, new Metadata());
        return headers.get();
    }

    private static void signedIn(String token) {
        AuthContext caller = new AuthContext(UUID.randomUUID(), Optional.of(TENANT), Set.of(INSTITUTION),
                Optional.of(INSTITUTION), Set.of("sis.student.read"), "jti", Optional.empty(), false, Optional.empty(),
                Optional.empty(), false);
        AuthContextHolder.set(caller, token);
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(TENANT, null));
    }

    @Test
    void sendsTheCallersIdentityTraceAndDeadline() {
        signedIn("callers-token");
        TraceHolder.set(TraceContext.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01").orElseThrow());
        AtomicReference<CallOptions> options = new AtomicReference<>();

        Metadata headers = sent(new GrpcCallerInterceptor(Duration.ofSeconds(7)), CallOptions.DEFAULT, options);

        assertThat(headers.get(GrpcAuthInterceptor.AUTHORIZATION)).isEqualTo("Bearer callers-token");
        assertThat(headers.get(GrpcAuthInterceptor.TENANT)).isEqualTo(TENANT.toString());
        assertThat(headers.get(GrpcAuthInterceptor.INSTITUTION)).isEqualTo(INSTITUTION.toString());
        assertThat(headers.get(GrpcEdge.TRACEPARENT)).startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-")
                .doesNotContain("00f067aa0ba902b7");
        assertThat(options.get().getDeadline().timeRemaining(TimeUnit.SECONDS)).isBetween(5L, 7L);
    }

    @Test
    void keepsTheStubsOwnDeadlineAndCredentials() {
        signedIn("callers-token");
        CallCredentials service = new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo request, Executor executor, MetadataApplier applier) {
            }
        };
        Deadline own = Deadline.after(1, TimeUnit.MINUTES);
        AtomicReference<CallOptions> options = new AtomicReference<>();

        Metadata headers = sent(new GrpcCallerInterceptor(Duration.ofSeconds(7)),
                CallOptions.DEFAULT.withDeadline(own).withCallCredentials(service), options);

        assertThat(headers.get(GrpcAuthInterceptor.AUTHORIZATION)).isNull();
        assertThat(headers.get(GrpcAuthInterceptor.TENANT)).isNull();
        assertThat(headers.get(GrpcEdge.TRACEPARENT)).as("a new trace without a current one").isNotNull();
        assertThat(options.get().getDeadline()).isEqualTo(own);
    }

    @Test
    void sendsNoIdentityWithoutACaller() {
        Metadata headers = sent(new GrpcCallerInterceptor(Duration.ofSeconds(7)), CallOptions.DEFAULT);

        assertThat(headers.get(GrpcAuthInterceptor.AUTHORIZATION)).isNull();
        assertThat(headers.get(GrpcAuthInterceptor.INSTITUTION)).isNull();
    }

    @Test
    void refusesACallInsideADatabaseTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatThrownBy(() -> sent(new GrpcCallerInterceptor(Duration.ofSeconds(7)), CallOptions.DEFAULT))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("inside a database transaction");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
