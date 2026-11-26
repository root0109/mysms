package com.mysms.learning.lms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.TestTransactions;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the lms workflow component tests share: signed tokens, calls over HTTP to the running service, and the rows a
 * test starts from, written with plain SQL as the application role inside the test's own tenant.
 */
final class LmsFixtures {

    static final JsonMapper JSON = JsonMapper.builder().build();
    /** A 32-byte field-encryption master key for the tests ({@code mysms.secrets.keys}). */
    static final String SECRETS = "test:" + java.util.Base64.getEncoder().encodeToString(new byte[32]);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    private final TestTokens tokens;
    private final ModuleDatabase database;
    private final int port;

    LmsFixtures(TestTokens tokens, ModuleDatabase database, int port) {
        this.tokens = tokens;
        this.database = database;
        this.port = port;
    }

    record Reply(int status, JsonNode body, String text) {

        /** The answer as the gRPC parity checks compare it. */
        GrpcParity.Answer asAnswer() {
            return new GrpcParity.Answer(status, status >= 400 ? body.path("code").asString() : null, body);
        }
    }

    /** A tenant user's token: the institutions it reaches ("*" for all), its tenant-wide permissions and its own. */
    String token(UUID user, UUID tenant, List<String> institutions, List<String> tenantWide, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", tenantWide);
        claims.put("ins", institutions);
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    /** Every permission in every institution of the tenant. */
    String admin(UUID tenant) {
        return token(UUID.randomUUID(), tenant, List.of("*"), List.of("*"), "*");
    }

    /** The given permissions in one institution only. */
    String member(UUID tenant, UUID institution, String... permissions) {
        return token(UUID.randomUUID(), tenant, List.of(institution.toString()), List.of(), permissions);
    }

    /** The given permissions in one institution, as a named user. */
    String user(UUID user, UUID tenant, UUID institution, String... permissions) {
        return token(user, tenant, List.of(institution.toString()), List.of(), permissions);
    }

    Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        boolean typed = false;
        for (int i = 0; i < headers.length; i += 2) {
            typed |= "Content-Type".equalsIgnoreCase(headers[i]);
        }
        if (body != null && !typed) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    /** One RPC of lms's services by name, its request given as the proto's JSON, over the in-process channel. */
    static GrpcParity.Answer grpc(io.grpc.Channel channel, String rpcName, String token, String json) {
        Descriptors.MethodDescriptor rpc = null;
        for (Descriptors.ServiceDescriptor service : com.mysms.lms.v1.LmsProto.getDescriptor().getServices()) {
            rpc = rpc != null ? rpc : service.findMethodByName(rpcName);
        }
        assertThat(rpc).as(rpcName).isNotNull();
        DynamicMessage.Builder request = DynamicMessage.newBuilder(rpc.getInputType());
        try {
            JsonFormat.parser().merge(json, request);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException(rpcName + " request", e);
        }
        return GrpcParity.call(channel, rpc, request.build(), token);
    }

    static Reply expect(Reply reply, int status) {
        assertThat(reply.status()).as(reply.text()).isEqualTo(status);
        return reply;
    }

    // ---- seeded rows

    /** Runs SQL work in the tenant, as the application role, in one committed transaction. */
    <T> T inTenant(UUID tenant, Supplier<T> work) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        return CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(work::get));
    }

    /** One SQL statement run in the tenant, with named parameters in pairs. */
    void sql(UUID tenant, String statement, Object... namedParams) {
        inTenant(tenant, () -> {
            var sql = database.jdbc().sql(statement);
            for (int i = 0; i < namedParams.length; i += 2) {
                sql = sql.param((String) namedParams[i], namedParams[i + 1]);
            }
            return sql.update();
        });
    }

    /** One value read with SQL in the tenant. */
    <T> T value(UUID tenant, String sql, Class<T> type, Object... namedParams) {
        return inTenant(tenant, () -> {
            var statement = database.jdbc().sql(sql);
            for (int i = 0; i < namedParams.length; i += 2) {
                statement = statement.param((String) namedParams[i], namedParams[i + 1]);
            }
            return statement.query(type).optional().orElse(null);
        });
    }

    /** A published cohort of the institution. */
    UUID cohort(UUID tenant, UUID institution) {
        UUID cohort = UUID.randomUUID();
        sql(tenant, "INSERT INTO lms.cohort (id, tenant_id, institution_id, title, start_date, end_date, timezone, "
                        + "lifecycle_status, is_published) VALUES (:id, :tenantId, :institution, 'Cohort', :start, "
                        + ":end, 'UTC', 'active', true)",
                "id", cohort, "institution", institution,
                "start", LocalDate.parse("2026-01-05"), "end", LocalDate.parse("2026-06-30"));
        return cohort;
    }

    /** A graded content item of the cohort (points possible as given; kind and its target from the caller). */
    UUID contentItem(UUID tenant, UUID cohort, String kind, String targetColumn, Object target, String points) {
        UUID item = UUID.randomUUID();
        sql(tenant, "INSERT INTO lms.content_item (id, tenant_id, cohort_id, item_kind, title, is_graded, points, "
                        + targetColumn + ") VALUES (:id, :tenantId, :cohort, :kind, 'Item', true, :points, :target)",
                "id", item, "cohort", cohort, "kind", kind, "points",
                points == null ? null : new java.math.BigDecimal(points), "target", target);
        return item;
    }
}
