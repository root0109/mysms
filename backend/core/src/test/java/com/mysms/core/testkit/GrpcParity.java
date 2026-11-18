package com.mysms.core.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.ErrorInfo;
import com.mysms.core.grpc.GrpcStatuses;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * REST ↔ gRPC parity checks for component tests: the same operation over both transports must give the same resource
 * and the same refusal, each answer read as an {@link Answer} and compared field by field.
 */
public final class GrpcParity {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer();
    private static final Pattern CURSOR = Pattern.compile("[?&]cursor=([^&]+)");
    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcParity() {
    }

    /**
     * One answer of either transport.
     *
     * @param status the HTTP status (gRPC: 200, or the {@code status} of the refusal's ErrorInfo)
     * @param code   the problem code (gRPC: the ErrorInfo reason); null on success
     * @param body   the response body (gRPC: the response message as JSON); null on a refusal
     */
    public record Answer(int status, String code, JsonNode body) {
    }

    /**
     * A channel to the service's in-process server: a component test runs the starter's server in process only
     * ({@code grpc.server.port=-1}, {@code grpc.server.in-process-name=<name>} — {@link #IN_PROCESS_SERVER}), so its
     * calls go through every edge interceptor without a socket.
     */
    public static ManagedChannel channel(String inProcessName) {
        return InProcessChannelBuilder.forName(inProcessName).build();
    }

    /**
     * A plaintext channel to the service's gRPC port, for a test that needs a real socket (the peer's address, which
     * an in-process call has not): the test gives the server a free port ({@code grpc.server.port}).
     */
    public static ManagedChannel channel(int port) {
        return ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
    }

    /**
     * The property that starts a component test's in-process gRPC server under the test class's name (a name per test
     * class: the contexts Spring keeps for reuse must not share one): {@code properties = GrpcParity.IN_PROCESS_SERVER
     * + "=MasterGrpcParityComponentTest"}.
     */
    public static final String IN_PROCESS_SERVER = "grpc.server.in-process-name";

    /** A channel that sends the bearer token (none when null) on every call. */
    public static Channel as(Channel channel, String token) {
        Metadata headers = new Metadata();
        if (token != null) {
            headers.put(AUTHORIZATION, "Bearer " + token);
        }
        return ClientInterceptors.intercept(channel, MetadataUtils.newAttachHeadersInterceptor(headers));
    }

    /** A unary method of the contract by its descriptor, with dynamic messages (wire-identical to the stubs'). */
    public static MethodDescriptor<DynamicMessage, DynamicMessage> method(Descriptors.MethodDescriptor rpc) {
        return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(rpc.getService().getFullName(), rpc.getName()))
                .setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(rpc.getInputType())))
                .setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(rpc.getOutputType())))
                .build();
    }

    /** Calls a method and reads the answer, whichever it is. */
    public static Answer call(Channel channel, Descriptors.MethodDescriptor rpc, DynamicMessage request, String token) {
        try {
            return new Answer(200, null, json(ClientCalls.blockingUnaryCall(as(channel, token), method(rpc),
                    CallOptions.DEFAULT, request)));
        } catch (StatusRuntimeException e) {
            return refusal(e);
        }
    }

    /** A gRPC refusal read as REST's status and code (from the ErrorInfo it must carry). */
    public static Answer refusal(StatusRuntimeException failure) {
        com.google.rpc.Status status = StatusProto.fromThrowable(failure);
        assertThat(status).as("a refusal carries google.rpc.Status details: " + failure.getStatus()).isNotNull();
        for (Any detail : status.getDetailsList()) {
            if (detail.is(ErrorInfo.class)) {
                try {
                    ErrorInfo info = detail.unpack(ErrorInfo.class);
                    int http = Integer.parseInt(info.getMetadataOrThrow("status"));
                    assertThat(failure.getStatus().getCode().value()).as("the gRPC code of api/README's table")
                            .isEqualTo(GrpcStatuses.code(HttpStatus.valueOf(http), info.getReason()).getNumber());
                    return new Answer(http, info.getReason(), null);
                } catch (InvalidProtocolBufferException e) {
                    throw new AssertionError(e);
                }
            }
        }
        throw new AssertionError("no ErrorInfo in " + failure.getStatus());
    }

    /** The message as protobuf's JSON (lowerCamelCase names, RFC 3339 timestamps, enum value names). */
    public static JsonNode json(MessageOrBuilder message) {
        try {
            return JSON.readTree(PRINTER.print(message));
        } catch (InvalidProtocolBufferException e) {
            throw new AssertionError(e);
        }
    }

    /** Both transports refused alike: the same HTTP status and the same problem code. */
    public static void assertSameRefusal(Answer rest, Answer grpc) {
        assertThat(grpc.status()).as("status (REST %s %s)", rest.status(), rest.code()).isEqualTo(rest.status());
        assertThat(grpc.code()).as("code").isEqualTo(rest.code());
    }

    /**
     * Both transports point at the same next page: REST's {@code next} link carries the cursor gRPC returned as
     * {@code next_cursor}, compared by what it encodes (page, sort, view, last row's keys and id) and not by the
     * second it was issued in, which two calls need not share.
     */
    public static void assertSameCursor(String restNextLink, String grpcCursor) {
        Matcher cursor = CURSOR.matcher(restNextLink);
        assertThat(cursor.find()).as("REST's next link %s carries a cursor", restNextLink).isTrue();
        assertThat(position(grpcCursor)).as("the next page's position")
                .isEqualTo(position(URLDecoder.decode(cursor.group(1), StandardCharsets.UTF_8)));
    }

    /** A cursor's content (core's PageCursor: base64url JSON) without its issue time {@code t}. */
    private static JsonNode position(String cursor) {
        ObjectNode decoded = (ObjectNode) JSON.readTree(Base64.getUrlDecoder().decode(cursor));
        decoded.remove("t");
        return decoded;
    }

    /**
     * The same resource over both transports: every field REST returns has the gRPC message's value — proto3 leaves a
     * default value ({@code false}, {@code 0}, {@code ""}, an int64 {@code "0"}) out, an enum is its value name
     * ({@code STATUS_ACTIVE} for {@code "active"}), an instant is compared as an instant, a number by its value (a
     * {@code google.protobuf.Value} number is a double, an int64 a string) — and the message has no field REST lacks.
     */
    public static void assertSameResource(JsonNode rest, JsonNode grpc) {
        Iterator<Map.Entry<String, JsonNode>> fields = ((ObjectNode) rest).properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode expected = field.getValue();
            JsonNode actual = grpc.get(field.getKey());
            if (actual == null) {
                assertThat(isDefault(expected)).as("%s = %s is missing over gRPC", field.getKey(), expected).isTrue();
            } else if (expected.isObject() && actual.isObject()) {
                assertSameResource(expected, actual);
            } else {
                assertThat(same(expected, actual)).as("%s: REST %s, gRPC %s", field.getKey(), expected, actual).isTrue();
            }
        }
        grpc.propertyNames().forEach(name -> assertThat(rest.has(name)).as("%s only over gRPC", name).isTrue());
    }

    /** A REST number and the gRPC value of the same amount, rendered as protobuf does (an int64 as a string, a double). */
    private static boolean sameNumber(JsonNode rest, JsonNode grpc) {
        if (!rest.isNumber()) {
            return false;
        }
        try {
            return new BigDecimal(rest.asString()).compareTo(new BigDecimal(grpc.asString())) == 0;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    private static boolean isDefault(JsonNode value) {
        return value.isNull() || value.isBoolean() && !value.asBoolean() || value.isNumber() && value.asDouble() == 0
                || value.isString() && (value.asString().isEmpty() || "0".equals(value.asString()))
                || value.isArray() && value.isEmpty() || value.isObject() && value.isEmpty();
    }

    private static boolean same(JsonNode rest, JsonNode grpc) {
        if (rest.equals(grpc) || sameNumber(rest, grpc)) {
            return true;
        }
        if (rest.isObject() && grpc.isObject()) {
            try {
                assertSameResource(rest, grpc);
                return true;
            } catch (AssertionError different) {
                return false;
            }
        }
        if (rest.isString() && grpc.isString()) {
            String value = rest.asString();
            String name = grpc.asString();
            if (name.endsWith("_" + value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_"))) {
                return true; // an enum value
            }
            try {
                return Instant.parse(value).equals(Instant.parse(name));
            } catch (DateTimeParseException notAnInstant) {
                return false;
            }
        }
        if (rest.isArray() && grpc.isArray() && rest.size() == grpc.size()) {
            for (int i = 0; i < rest.size(); i++) {
                if (!same(rest.get(i), grpc.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
