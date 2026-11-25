package com.mysms.privacyclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.GrpcAuthInterceptor;
import com.mysms.authclient.ServiceTokenProvider;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.UnavailableException;
import com.mysms.privacy.v1.BatchCheckConsentRequest;
import com.mysms.privacy.v1.BatchCheckConsentResponse;
import com.mysms.privacy.v1.ConsentDecisionServiceGrpc;
import com.sun.net.httpserver.HttpServer;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The gRPC client against an in-process stand-in of privacy's ConsentDecisionService and a stand-in token endpoint:
 * the request and the identity it carries, how answers are read, and every answer it refuses to read.
 */
class PrivacyDecisionGrpcClientTest {

    private static final ActivityCode MARKETING = new ActivityCode("comms.marketing_broadcast");
    private static final UUID TENANT = UUID.randomUUID();
    /** Generous for the ordinary calls, whose first one also fetches the service token. */
    private static final Duration DEADLINE = Duration.ofSeconds(5);
    /** What the unanswered call waits. */
    private static final Duration SHORT = Duration.ofMillis(300);

    private final AtomicReference<BatchCheckConsentRequest> asked = new AtomicReference<>();
    private final AtomicReference<Metadata> headers = new AtomicReference<>();
    private Function<BatchCheckConsentRequest, BatchCheckConsentResponse> answers;
    private HttpServer tokens;
    private Server server;
    private ManagedChannel channel;
    private PrivacyDecisionGrpcClient client;

    @BeforeEach
    void start() throws IOException {
        tokens = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tokens.createContext("/oauth2/token", exchange -> {
            byte[] body = "{\"access_token\":\"comms-token\",\"token_type\":\"Bearer\",\"expires_in\":300}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        tokens.start();
        ServerInterceptor capture = new ServerInterceptor() {
            @Override
            public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata metadata,
                    ServerCallHandler<Q, R> next) {
                headers.set(metadata);
                return next.startCall(call, metadata);
            }
        };
        ConsentDecisionServiceGrpc.ConsentDecisionServiceImplBase privacy =
                new ConsentDecisionServiceGrpc.ConsentDecisionServiceImplBase() {
                    @Override
                    public void batchCheckConsent(BatchCheckConsentRequest request,
                            StreamObserver<BatchCheckConsentResponse> out) {
                        asked.set(request);
                        out.onNext(answers.apply(request));
                        out.onCompleted();
                    }
                };
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(ServerInterceptors.intercept(privacy, capture)).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        String tokenUri = "http://127.0.0.1:" + tokens.getAddress().getPort() + "/oauth2/token";
        ServiceTokenProvider serviceTokens = new ServiceTokenProvider(tokenUri,
                module -> "comms".equals(module) ? "comms-secret-0123456789-abcdefghijklmnop" : null,
                JsonMapper.builder().build(), Clock.systemUTC());
        client = new PrivacyDecisionGrpcClient(channel, serviceTokens, DEADLINE);
    }

    @AfterEach
    void stop() {
        channel.shutdownNow();
        server.shutdownNow();
        tokens.stop(0);
        CallContextHolder.clear();
    }

    private static <T> T inTenant(Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous("trace").withIdentity(TENANT, null), work);
    }

    private static com.mysms.privacy.v1.ConsentDecision decision(String subject, boolean allowed,
            com.mysms.privacy.v1.ConsentDecision.Reason reason) {
        return com.mysms.privacy.v1.ConsentDecision.newBuilder().setSubjectPersonId(subject).setAllowed(allowed)
                .setReason(reason).setLegalBasis("consent").setStateVersion(7).build();
    }

    /** Answers every subject asked with the reason, in order. */
    private static BatchCheckConsentResponse all(BatchCheckConsentRequest request,
            com.mysms.privacy.v1.ConsentDecision.Reason reason) {
        boolean allowed = reason == com.mysms.privacy.v1.ConsentDecision.Reason.REASON_CONSENT
                || reason == com.mysms.privacy.v1.ConsentDecision.Reason.REASON_NON_CONSENT_BASIS;
        BatchCheckConsentResponse.Builder response = BatchCheckConsentResponse.newBuilder();
        request.getSubjectPersonIdsList().forEach(s -> response.addDecisions(decision(s, allowed, reason)));
        return response.build();
    }

    @Test
    void asksAsTheActivitysModuleInTheCallsTenantAndReadsTheAnswers() {
        UUID asha = UUID.randomUUID();
        UUID kabir = UUID.randomUUID();
        answers = request -> BatchCheckConsentResponse.newBuilder()
                .addDecisions(decision(asha.toString(), true, com.mysms.privacy.v1.ConsentDecision.Reason.REASON_CONSENT))
                .addDecisions(com.mysms.privacy.v1.ConsentDecision.newBuilder().setSubjectPersonId(kabir.toString())
                        .setReason(com.mysms.privacy.v1.ConsentDecision.Reason.REASON_RECONSENT_REQUIRED)
                        .setStateVersion(2))
                .build();
        Map<UUID, ConsentDecision> decided =
                inTenant(() -> client.decide(MARKETING, List.of(asha, kabir), ConsentScope.INSTITUTION));
        assertThat(decided.get(asha)).isEqualTo(new ConsentDecision(true, ConsentReason.CONSENT, "consent", 7));
        assertThat(decided.get(kabir))
                .isEqualTo(new ConsentDecision(false, ConsentReason.RECONSENT_REQUIRED, null, 2));
        assertThat(asked.get().getActivityCode()).isEqualTo("comms.marketing_broadcast");
        assertThat(asked.get().getScope()).isEqualTo(BatchCheckConsentRequest.Scope.SCOPE_INSTITUTION);
        assertThat(asked.get().getSubjectPersonIdsList()).containsExactly(asha.toString(), kabir.toString());
        assertThat(headers.get().get(GrpcAuthInterceptor.AUTHORIZATION)).isEqualTo("Bearer comms-token");
        assertThat(headers.get().get(GrpcAuthInterceptor.TENANT)).isEqualTo(TENANT.toString());
    }

    @Test
    void readsEveryReasonPrivacyGives() {
        for (com.mysms.privacy.v1.ConsentDecision.Reason reason : com.mysms.privacy.v1.ConsentDecision.Reason.values()) {
            if (reason == com.mysms.privacy.v1.ConsentDecision.Reason.REASON_UNSPECIFIED
                    || reason == com.mysms.privacy.v1.ConsentDecision.Reason.UNRECOGNIZED) {
                continue;
            }
            answers = request -> all(request, reason);
            UUID subject = UUID.randomUUID();
            ConsentDecision decided =
                    inTenant(() -> client.decide(MARKETING, List.of(subject), ConsentScope.TENANT)).get(subject);
            assertThat(decided.reason().name()).isEqualTo(reason.name().substring("REASON_".length()));
        }
    }

    @Test
    void refusesAnswersThatDoNotMatchTheQuestion() {
        UUID asha = UUID.randomUUID();
        UUID kabir = UUID.randomUUID();
        answers = request -> BatchCheckConsentResponse.getDefaultInstance();
        assertThatThrownBy(() -> inTenant(() -> client.decide(MARKETING, List.of(asha), ConsentScope.TENANT)))
                .isInstanceOf(UnavailableException.class);
        answers = request -> BatchCheckConsentResponse.newBuilder()
                .addDecisions(decision(kabir.toString(), true, com.mysms.privacy.v1.ConsentDecision.Reason.REASON_CONSENT))
                .addDecisions(decision(asha.toString(), true, com.mysms.privacy.v1.ConsentDecision.Reason.REASON_CONSENT))
                .build();
        assertThatThrownBy(() -> inTenant(() -> client.decide(MARKETING, List.of(asha, kabir), ConsentScope.TENANT)))
                .isInstanceOf(UnavailableException.class);
        answers = request -> all(request, com.mysms.privacy.v1.ConsentDecision.Reason.REASON_UNSPECIFIED);
        assertThatThrownBy(() -> inTenant(() -> client.decide(MARKETING, List.of(asha), ConsentScope.TENANT)))
                .isInstanceOf(UnavailableException.class);
        // an allow its reason does not support, and a refusal with an allowing reason
        answers = request -> BatchCheckConsentResponse.newBuilder()
                .addDecisions(decision(asha.toString(), true, com.mysms.privacy.v1.ConsentDecision.Reason.REASON_REFUSED))
                .build();
        assertThatThrownBy(() -> inTenant(() -> client.decide(MARKETING, List.of(asha), ConsentScope.TENANT)))
                .isInstanceOf(UnavailableException.class);
        answers = request -> BatchCheckConsentResponse.newBuilder()
                .addDecisions(decision(asha.toString(), false, com.mysms.privacy.v1.ConsentDecision.Reason.REASON_CONSENT))
                .build();
        assertThatThrownBy(() -> inTenant(() -> client.decide(MARKETING, List.of(asha), ConsentScope.TENANT)))
                .isInstanceOf(UnavailableException.class);
    }

    @Test
    void anUnansweredCallFailsWithinTheDeadline() {
        answers = request -> {
            try {
                Thread.sleep(SHORT.multipliedBy(3).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return all(request, com.mysms.privacy.v1.ConsentDecision.Reason.REASON_CONSENT);
        };
        server.shutdownNow();
        String name = InProcessServerBuilder.generateName();
        try {
            server = InProcessServerBuilder.forName(name)
                    .addService(new ConsentDecisionServiceGrpc.ConsentDecisionServiceImplBase() {
                        @Override
                        public void batchCheckConsent(BatchCheckConsentRequest request,
                                StreamObserver<BatchCheckConsentResponse> out) {
                            out.onNext(answers.apply(request));
                            out.onCompleted();
                        }
                    }).build().start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        channel.shutdownNow();
        channel = InProcessChannelBuilder.forName(name).build();
        ServiceTokenProvider warm = new ServiceTokenProvider(
                "http://127.0.0.1:" + tokens.getAddress().getPort() + "/oauth2/token",
                module -> "comms-secret-0123456789-abcdefghijklmnop", JsonMapper.builder().build(), Clock.systemUTC());
        // the token is fetched beforehand, so the short deadline is spent on the unanswered call alone
        warm.token("comms");
        PrivacyDecisionGrpcClient slow = new PrivacyDecisionGrpcClient(channel, warm, SHORT);
        assertThatThrownBy(() -> inTenant(() -> slow.decide(MARKETING, List.of(UUID.randomUUID()),
                ConsentScope.TENANT))).isInstanceOf(UnavailableException.class).hasMessageContaining("DEADLINE_EXCEEDED");
    }

    @Test
    void asksOneToOneThousandSubjectsWithinAPositiveDeadline() {
        assertThatThrownBy(() -> client.decide(MARKETING, List.of(), ConsentScope.TENANT))
                .isInstanceOf(IllegalArgumentException.class);
        List<UUID> tooMany = Stream.generate(UUID::randomUUID).limit(1001).toList();
        assertThatThrownBy(() -> client.decide(MARKETING, tooMany, ConsentScope.TENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PrivacyDecisionGrpcClient(channel, null, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
