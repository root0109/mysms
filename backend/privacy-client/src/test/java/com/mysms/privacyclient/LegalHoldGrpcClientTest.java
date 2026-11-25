package com.mysms.privacyclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.ServiceTokenProvider;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.UnavailableException;
import com.mysms.privacy.v1.ActiveLegalHold;
import com.mysms.privacy.v1.LegalHoldServiceGrpc;
import com.mysms.privacy.v1.ListActiveHoldsRequest;
import com.mysms.privacy.v1.ListActiveHoldsResponse;
import com.sun.net.httpserver.HttpServer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The legal-holds client against an in-process stand-in of privacy's LegalHoldService and a stand-in token endpoint:
 * the request it sends, how holds are read, and every answer it refuses (fail closed).
 */
class LegalHoldGrpcClientTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final Duration DEADLINE = Duration.ofSeconds(5);

    private final AtomicReference<ListActiveHoldsRequest> asked = new AtomicReference<>();
    private Function<ListActiveHoldsRequest, ListActiveHoldsResponse> answers;
    private HttpServer tokens;
    private Server server;
    private ManagedChannel channel;
    private LegalHoldGrpcClient client;

    @BeforeEach
    void start() throws IOException {
        tokens = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tokens.createContext("/oauth2/token", exchange -> {
            byte[] body = "{\"access_token\":\"people-token\",\"token_type\":\"Bearer\",\"expires_in\":300}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        tokens.start();
        LegalHoldServiceGrpc.LegalHoldServiceImplBase privacy = new LegalHoldServiceGrpc.LegalHoldServiceImplBase() {
            @Override
            public void listActiveHolds(ListActiveHoldsRequest request,
                    StreamObserver<ListActiveHoldsResponse> out) {
                asked.set(request);
                try {
                    out.onNext(answers.apply(request));
                    out.onCompleted();
                } catch (IllegalStateException e) {
                    out.onError(Status.UNAVAILABLE.asRuntimeException());
                }
            }
        };
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(privacy).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        String tokenUri = "http://127.0.0.1:" + tokens.getAddress().getPort() + "/oauth2/token";
        ServiceTokenProvider serviceTokens = new ServiceTokenProvider(tokenUri,
                module -> "people".equals(module) ? "people-secret-0123456789-abcdefghijklmnop" : null,
                JsonMapper.builder().build(), Clock.systemUTC());
        client = new LegalHoldGrpcClient(channel, serviceTokens, DEADLINE);
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

    @Test
    void asksForTheSubjectAndTheModuleAndReadsEveryKindOfHold() {
        UUID subject = UUID.randomUUID();
        UUID row = UUID.randomUUID();
        answers = request -> ListActiveHoldsResponse.newBuilder()
                .addHolds(ActiveLegalHold.newBuilder().setId(UUID.randomUUID().toString())
                        .setScope(ActiveLegalHold.Scope.SCOPE_SUBJECT).setSubjectPersonId(subject.toString()))
                .addHolds(ActiveLegalHold.newBuilder().setId(UUID.randomUUID().toString())
                        .setScope(ActiveLegalHold.Scope.SCOPE_CATEGORY).setCategoryCode("health"))
                .addHolds(ActiveLegalHold.newBuilder().setId(UUID.randomUUID().toString())
                        .setScope(ActiveLegalHold.Scope.SCOPE_RECORDS).setModule("people")
                        .setEntityType("people.person_document").addEntityIds(row.toString()))
                .build();

        ActiveHolds holds = inTenant(() -> client.activeHolds("people", subject));

        assertThat(asked.get().getSubjectPersonId()).isEqualTo(subject.toString());
        assertThat(asked.get().getModule()).isEqualTo("people");
        assertThat(holds.holds()).extracting(ActiveHold::scope)
                .containsExactly(ActiveHold.SUBJECT, ActiveHold.CATEGORY, ActiveHold.RECORDS);
        assertThat(holds.holds().get(0).subjectPersonId()).isEqualTo(subject);
        assertThat(holds.holds().get(2).entityIds()).containsExactly(row);
        assertThat(holds.keeps("people.person", "identity", UUID.randomUUID())).as("the subject is held").isTrue();
    }

    @Test
    void noHoldIsAnEmptyAnswer() {
        answers = request -> ListActiveHoldsResponse.getDefaultInstance();
        ActiveHolds holds = inTenant(() -> client.activeHolds("people", UUID.randomUUID()));
        assertThat(holds.holds()).isEmpty();
        assertThat(holds.keeps("people.person", "identity", UUID.randomUUID())).isFalse();
    }

    @Test
    void aFailedCallOrAnUnreadableHoldIsNoAnswer() {
        answers = request -> {
            throw new IllegalStateException("privacy is down");
        };
        assertThatThrownBy(() -> inTenant(() -> client.activeHolds("people", UUID.randomUUID())))
                .isInstanceOf(UnavailableException.class);

        answers = request -> ListActiveHoldsResponse.newBuilder()
                .addHolds(ActiveLegalHold.newBuilder().setId(UUID.randomUUID().toString())).build();
        assertThatThrownBy(() -> inTenant(() -> client.activeHolds("people", UUID.randomUUID())))
                .as("a hold without a scope").isInstanceOf(UnavailableException.class);

        answers = request -> ListActiveHoldsResponse.newBuilder()
                .addHolds(ActiveLegalHold.newBuilder().setId("not-a-uuid")
                        .setScope(ActiveLegalHold.Scope.SCOPE_SUBJECT)).build();
        assertThatThrownBy(() -> inTenant(() -> client.activeHolds("people", UUID.randomUUID())))
                .as("a malformed id").isInstanceOf(UnavailableException.class);
    }

    @Test
    void refusesADeadlineThatIsNotPositive() {
        assertThatThrownBy(() -> new LegalHoldGrpcClient(channel, null, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
