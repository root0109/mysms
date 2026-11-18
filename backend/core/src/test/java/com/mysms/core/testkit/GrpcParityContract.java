package com.mysms.core.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.FieldMask;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.testkit.GrpcParity.Answer;
import io.grpc.Channel;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The REST ↔ gRPC parity contract of one resource's standard operations against a live service (Template Method: the
 * module test supplies the REST caller, channel and tokens). Both transports must answer the same resource and refuse
 * alike: no permission, absent row, another tenant's row, malformed id, duplicate, invalid body or filter, stale or
 * missing etag.
 */
public final class GrpcParityContract {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<String> STANDARD = Set.of("Create", "Get", "List", "Update", "Delete");
    private static final Pattern PARENT = Pattern.compile("([a-z0-9-]+)/([0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})");
    private static final int PARTITION_DAYS = 30;

    /** REST of the running service. */
    @FunctionalInterface
    public interface Rest {

        Answer call(String method, String path, String token, String body, String... headers) throws Exception;
    }

    /**
     * The callers: one holding every permission of the module, one of the same tenant holding none, and one of another
     * tenant holding every permission (null for global data, which every tenant reads alike).
     */
    public record Callers(String admin, String nobody, String stranger) {
    }

    /**
     * One resource as crud-spec.json describes it.
     *
     * @param collection         its REST collection path (nested: with the parent id in it)
     * @param create             a valid create body (empty for a resource the API does not create)
     * @param update             the changes of an update; null when the resource is not updated
     * @param duplicateConflicts whether creating the same body twice is a 409
     * @param described          the whole crud-spec.json entry ({@code ops}, {@code fixtures}, {@code seed},
     *                           {@code seedId}, {@code partition}, {@code scope}, …)
     */
    public record Resource(String name, String collection, ObjectNode create, ObjectNode update,
                           boolean duplicateConflicts, JsonNode described) {

        /** A resource serving all five standard operations, described by its fields alone. */
        public Resource(String name, String collection, ObjectNode create, ObjectNode update,
                        boolean duplicateConflicts) {
            this(name, collection, create, update, duplicateConflicts, standard());
        }

        /** The resource of one crud-spec.json entry. */
        public static Resource of(JsonNode described) {
            JsonNode create = described.path("create");
            JsonNode update = described.path("update");
            return new Resource(described.path("name").asString(), described.path("collection").asString(),
                    create.isObject() ? (ObjectNode) create : JSON.createObjectNode(),
                    update.isObject() ? (ObjectNode) update : null, described.path("duplicateConflicts").asBoolean(),
                    described);
        }

        private static JsonNode standard() {
            ObjectNode described = JSON.createObjectNode();
            ArrayNode ops = described.putArray("ops");
            List.of("Create", "Get", "List", "Update", "Delete").forEach(ops::add);
            return described;
        }

        /** The standard operations it serves (all five when the spec names none). */
        public Set<String> ops() {
            Set<String> ops = new LinkedHashSet<>(strings(described.path("ops")));
            return ops.isEmpty() ? STANDARD : Set.copyOf(ops);
        }

        public boolean serves(String operation) {
            return ops().contains(operation);
        }

        /** Whether it serves all five standard operations. */
        public boolean standardOnly() {
            return ops().containsAll(STANDARD);
        }

        /** The statements creating the rows it refers to (run by the module test in each tenant). */
        public List<String> fixtures() {
            return strings(described.path("fixtures"));
        }

        /** The statements creating its seeded row, for a resource the API does not create. */
        public List<String> seed() {
            return strings(described.path("seed"));
        }

        /** The id of the seeded row; null when it has none. */
        public String seedId() {
            return described.has("seedId") ? described.path("seedId").asString() : null;
        }

        /** The field a list must be narrowed by (a partitioned table's key); null when none. */
        public String partition() {
            return described.has("partition") ? described.path("partition").asString() : null;
        }

        /** The data scope ({@code global} for data every tenant reads alike); empty when the spec names none. */
        public String scope() {
            return described.path("scope").asString("");
        }

        private static List<String> strings(JsonNode array) {
            List<String> values = new ArrayList<>();
            array.forEach(value -> values.add(value.asString()));
            return values;
        }
    }

    private final FileDescriptor contract;
    private final Rest rest;
    private final Supplier<Channel> channel;
    private final ProtoBridge bridge;
    private final Callers callers;

    public GrpcParityContract(FileDescriptor contract, Rest rest, Supplier<Channel> channel, ProtoBridge bridge,
                              Callers callers) {
        this.contract = contract;
        this.rest = rest;
        this.channel = channel;
        this.bridge = bridge;
        this.callers = callers;
    }

    /** The resources of a crud-spec.json on the classpath, every entry whatever operations it serves. */
    public static List<Resource> resources(String specResource) throws Exception {
        JsonNode spec = JSON.readTree(GrpcParityContract.class.getClassLoader().getResourceAsStream(specResource));
        List<Resource> resources = new ArrayList<>();
        for (JsonNode resource : spec.path("resources")) {
            resources.add(Resource.of(resource));
        }
        return resources;
    }

    // ------------------------------------------------------------------ calls

    /** An RPC of the contract by name. */
    public MethodDescriptor rpc(String name) {
        for (ServiceDescriptor service : contract.getServices()) {
            MethodDescriptor method = service.findMethodByName(name);
            if (method != null) {
                return method;
            }
        }
        throw new IllegalArgumentException("no RPC " + name + " in " + contract.getPackage());
    }

    /**
     * The List RPC of a resource, read from the contract: the RPC of the service declaring {@code Get<name>} (or
     * {@code Create<name>}) whose response's {@code items} are the resource — {@code ListBatches}, {@code ListMedia},
     * {@code ListRubricCriteria}, {@code ListStaff} — and, where the service lists the resource more than one way, the
     * one named most like the resource.
     */
    public MethodDescriptor listRpc(String name) {
        MethodDescriptor one = findRpc("Get" + name);
        if (one == null) {
            one = rpc("Create" + name);
        }
        Descriptor type = one.getOutputType().findFieldByName(resourceField(one.getOutputType())).getMessageType();
        MethodDescriptor best = null;
        for (MethodDescriptor method : one.getService().getMethods()) {
            FieldDescriptor items = method.getOutputType().findFieldByName("items");
            if (method.getName().startsWith("List") && items != null && items.isRepeated()
                    && items.getJavaType() == FieldDescriptor.JavaType.MESSAGE && items.getMessageType().equals(type)
                    && (best == null || shared("List" + name, method.getName()) > shared("List" + name, best.getName()))) {
                best = method;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("no List RPC of " + name + " in " + one.getService().getFullName());
        }
        return best;
    }

    private MethodDescriptor findRpc(String name) {
        for (ServiceDescriptor service : contract.getServices()) {
            MethodDescriptor method = service.findMethodByName(name);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private static int shared(String a, String b) {
        int length = 0;
        while (length < Math.min(a.length(), b.length()) && a.charAt(length) == b.charAt(length)) {
            length++;
        }
        return length;
    }

    public Answer grpc(MethodDescriptor rpc, DynamicMessage request, String token) {
        return GrpcParity.call(channel.get(), rpc, request, token);
    }

    public static DynamicMessage.Builder request(MethodDescriptor rpc) {
        return DynamicMessage.newBuilder(rpc.getInputType());
    }

    public static DynamicMessage.Builder set(DynamicMessage.Builder builder, String field, Object value) {
        return builder.setField(builder.getDescriptorForType().findFieldByName(field), value);
    }

    /** Both transports answered alike: the same refusal, or both succeeded. */
    public static void assertSameAnswer(Answer overRest, Answer overGrpc) {
        if (overRest.status() >= 400) {
            GrpcParity.assertSameRefusal(overRest, overGrpc);
        } else {
            assertThat(overGrpc.status()).as("gRPC answered %s where REST answered %s", overGrpc.code(),
                    overRest.status()).isEqualTo(200);
        }
    }

    // ------------------------------------------------------------------ the contract of one resource

    /**
     * The standard operations the resource serves, with their refusals, over both transports. A resource the API does
     * not create is read from its seeded row, which the module test has inserted (with {@link Resource#seed()}).
     */
    public void verify(Resource resource) throws Exception {
        String name = resource.name();
        assertThat(resource.serves("Get")).as("%s: the parity contract reads the row back with Get", name).isTrue();
        MethodDescriptor get = rpc("Get" + name);
        String field = resourceField(get.getOutputType());
        String collection = resource.collection();

        String id;
        if (resource.serves("Create")) {
            id = created(resource);
        } else {
            id = resource.seedId();
            assertThat(id).as("%s: a resource the API does not create needs a seeded row", name).isNotBlank();
        }
        Answer read = rest.call("GET", collection + "/" + id, callers.admin(), null);
        assertThat(read.status()).as("%s read: %s", name, read.code()).isEqualTo(200);
        Answer got = grpc(get, keyed(get, resource, id).build(), callers.admin());
        assertThat(got.status()).as("%s get: %s", name, got.code()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), got.body().path(json(get.getOutputType(), field)));
        assertSameAnswer(rest.call("GET", collection + "/" + UUID.randomUUID(), callers.admin(), null),
                grpc(get, keyed(get, resource, UUID.randomUUID().toString()).build(), callers.admin()));
        assertSameAnswer(rest.call("GET", collection + "/not-a-uuid", callers.admin(), null),
                grpc(get, keyed(get, resource, "not-a-uuid").build(), callers.admin()));
        assertSameAnswer(rest.call("GET", collection + "/" + id, callers.nobody(), null),
                grpc(get, keyed(get, resource, id).build(), callers.nobody()));
        if (callers.stranger() != null) {
            assertSameAnswer(rest.call("GET", collection + "/" + id, callers.stranger(), null),
                    grpc(get, keyed(get, resource, id).build(), callers.stranger()));
        }

        if (resource.serves("List")) {
            list(resource, read.body());
        }
        if (resource.serves("Update") && resource.update() != null) {
            read = updated(resource, id, read.body().path("etag").asString());
        }
        if (resource.serves("Delete")) {
            deleted(resource, id, read.body().path("etag").asString());
        }
    }

    /** Create over gRPC, read back over REST, with the refusals of a create; the new row's id. */
    private String created(Resource resource) throws Exception {
        String name = resource.name();
        String collection = resource.collection();
        ObjectNode body = resource.create();
        MethodDescriptor create = rpc("Create" + name);
        String requestField = resourceField(create.getInputType());
        Answer created = grpc(create, createRequest(create, requestField, body, resource), callers.admin());
        assertThat(created.status()).as("%s: %s", name, created.code()).isEqualTo(200);
        JsonNode row = created.body().path(json(create.getOutputType(), resourceField(create.getOutputType())));
        String id = row.path("id").asString();
        Answer read = rest.call("GET", collection + "/" + id, callers.admin(), null);
        assertThat(read.status()).isEqualTo(200);
        GrpcParity.assertSameResource(read.body(), row);

        if (resource.duplicateConflicts()) {
            assertSameAnswer(rest.call("POST", collection, callers.admin(), body.toString()),
                    grpc(create, createRequest(create, requestField, body, resource), callers.admin()));
        }
        ObjectNode empty = JSON.createObjectNode();
        Answer invalid = rest.call("POST", collection, callers.admin(), empty.toString());
        if (invalid.status() >= 400) {
            assertSameAnswer(invalid, grpc(create, createRequest(create, requestField, empty, resource),
                    callers.admin()));
        }
        assertSameAnswer(rest.call("POST", collection, callers.nobody(), body.toString()),
                grpc(create, createRequest(create, requestField, body, resource), callers.nobody()));
        return id;
    }

    /**
     * List: two pages of one row (a second row is created where the API creates rows) read alike, the next page's
     * cursor the same position over both, an invalid filter and a caller without the permission refused alike. A
     * partitioned list is refused alike without the range of its key and read within one around the row.
     */
    private void list(Resource resource, JsonNode row) throws Exception {
        String name = resource.name();
        String collection = resource.collection();
        MethodDescriptor list = listRpc(name);
        if (resource.serves("Create")) {
            MethodDescriptor create = rpc("Create" + name);
            grpc(create, createRequest(create, resourceField(create.getInputType()), unique(resource.create()),
                    resource), callers.admin());
        }
        String filter = "";
        if (resource.partition() != null) {
            assertSameAnswer(rest.call("GET", collection, callers.admin(), null),
                    grpc(list, keyed(list, resource, null).build(), callers.admin()));
            filter = range(resource.partition(), row.path(resource.partition()).asString());
        }
        String query = filter.isEmpty() ? "" : "&filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8);
        Answer first = rest.call("GET", collection + "?size=1" + query, callers.admin(), null);
        assertThat(first.status()).as("%s list: %s", name, first.code()).isEqualTo(200);
        Answer firstGrpc = grpc(list, set(keyed(list, resource, null), "filter", filter)
                .setField(list.getInputType().findFieldByName("pageable"), pageable(list, 0, 1, "")).build(),
                callers.admin());
        assertThat(firstGrpc.status()).as("%s list: %s", name, firstGrpc.code()).isEqualTo(200);
        assertThat(ids(firstGrpc.body().path("items"))).isEqualTo(ids(first.body().path("data")));
        if (!first.body().path("data").isEmpty()) {
            GrpcParity.assertSameResource(first.body().path("data").get(0), firstGrpc.body().path("items").get(0));
        }
        JsonNode pagination = firstGrpc.body().path("pagination");
        assertThat(pagination.path("page").asInt()).isEqualTo(first.body().path("paging").path("page").asInt());
        assertThat(pagination.path("size").asInt()).isEqualTo(first.body().path("paging").path("size").asInt());
        JsonNode next = first.body().path("paging").path("next");
        assertThat(pagination.path("next").asInt()).isEqualTo(next.isMissingNode() ? 0 : 2);
        if (!next.isMissingNode()) {
            String cursor = pagination.path("nextCursor").asString();
            GrpcParity.assertSameCursor(next.asString(), cursor);
            Answer second = rest.call("GET", next.asString(), callers.admin(), null);
            Answer secondGrpc = grpc(list, set(keyed(list, resource, null), "filter", filter)
                    .setField(list.getInputType().findFieldByName("pageable"), pageable(list, 2, 1, cursor)).build(),
                    callers.admin());
            assertThat(secondGrpc.status()).as("%s second page: %s", name, secondGrpc.code()).isEqualTo(200);
            assertThat(ids(secondGrpc.body().path("items"))).isEqualTo(ids(second.body().path("data")));
        }
        assertSameAnswer(rest.call("GET", collection + "?filter=nope%20%3D%201", callers.admin(), null),
                grpc(list, set(keyed(list, resource, null), "filter", "nope = 1").build(), callers.admin()));
        assertSameAnswer(rest.call("GET", collection + (query.isEmpty() ? "" : "?" + query.substring(1)),
                callers.nobody(), null), grpc(list, set(keyed(list, resource, null), "filter", filter).build(),
                callers.nobody()));
    }

    /** A filter on {@code key} reaching {@value #PARTITION_DAYS} days either side of a row's value (a date or an instant). */
    private static String range(String key, String value) {
        String from;
        String to;
        try {
            OffsetDateTime at = OffsetDateTime.parse(value);
            from = at.minusDays(PARTITION_DAYS).toString();
            to = at.plusDays(PARTITION_DAYS).toString();
        } catch (DateTimeParseException notAnInstant) {
            LocalDate day = LocalDate.parse(value);
            from = day.minusDays(PARTITION_DAYS).toString();
            to = day.plusDays(PARTITION_DAYS).toString();
        }
        return key + " >= \"" + from + "\" AND " + key + " <= \"" + to + "\"";
    }

    /** Update under its refusals (a stale etag, none, no permission), then over gRPC alike; the row as reread. */
    private Answer updated(Resource resource, String id, String etag) throws Exception {
        String name = resource.name();
        String item = resource.collection() + "/" + id;
        MethodDescriptor update = rpc("Update" + name);
        String field = resourceField(update.getInputType());
        ObjectNode changes = resource.update();
        assertSameAnswer(rest.call("PATCH", item, callers.admin(), changes.toString(), "If-Match", "\"99\""),
                grpc(update, updateRequest(update, field, resource, id, changes, "99"), callers.admin()));
        assertSameAnswer(rest.call("PATCH", item, callers.admin(), changes.toString()),
                grpc(update, updateRequest(update, field, resource, id, changes, ""), callers.admin()));
        assertSameAnswer(rest.call("PATCH", item, callers.nobody(), changes.toString(), "If-Match", etag),
                grpc(update, updateRequest(update, field, resource, id, changes, etag), callers.nobody()));
        Answer updated = grpc(update, updateRequest(update, field, resource, id, changes, etag), callers.admin());
        assertThat(updated.status()).as("%s update: %s", name, updated.code()).isEqualTo(200);
        Answer reread = rest.call("GET", item, callers.admin(), null);
        GrpcParity.assertSameResource(reread.body(), updated.body().path(json(update.getOutputType(),
                resourceField(update.getOutputType()))));
        return reread;
    }

    /** Delete under its refusals (a stale etag, none, no permission); the row is gone on both afterwards. */
    private void deleted(Resource resource, String id, String etag) throws Exception {
        String item = resource.collection() + "/" + id;
        MethodDescriptor delete = rpc("Delete" + resource.name());
        MethodDescriptor get = rpc("Get" + resource.name());
        assertSameAnswer(rest.call("DELETE", item, callers.admin(), null, "If-Match", "\"99\""),
                grpc(delete, set(keyed(delete, resource, id), "etag", "99").build(), callers.admin()));
        assertSameAnswer(rest.call("DELETE", item, callers.admin(), null),
                grpc(delete, keyed(delete, resource, id).build(), callers.admin()));
        assertSameAnswer(rest.call("DELETE", item, callers.nobody(), null, "If-Match", etag),
                grpc(delete, set(keyed(delete, resource, id), "etag", etag).build(), callers.nobody()));
        Answer deleted = grpc(delete, set(keyed(delete, resource, id), "etag", etag).build(), callers.admin());
        assertThat(deleted.status()).as("%s delete: %s", resource.name(), deleted.code()).isEqualTo(200);
        assertSameAnswer(rest.call("GET", item, callers.admin(), null),
                grpc(get, keyed(get, resource, id).build(), callers.admin()));
    }

    // ------------------------------------------------------------------ building requests from REST bodies

    /** The message's field that holds the resource: its one message field of the contract's own package. */
    private String resourceField(Descriptor type) {
        for (FieldDescriptor field : type.getFields()) {
            if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE && !field.isRepeated()
                    && field.getMessageType().getFile().equals(contract)) {
                return field.getName();
            }
        }
        throw new IllegalStateException(type.getFullName() + " carries no resource");
    }

    private static String json(Descriptor type, String field) {
        return type.findFieldByName(field).getJsonName();
    }

    /** A create request: the resource converted from the REST body like a DTO, the path ids of the resource. */
    @SuppressWarnings("unchecked")
    private DynamicMessage createRequest(MethodDescriptor create, String field, ObjectNode body, Resource resource) {
        DynamicMessage.Builder request = bridge.reply(request(create), field, JSON.convertValue(body, Map.class));
        return withPathIds(request, resource).build();
    }

    /** A request keyed by the row id (none for a list), with the parent ids a nested resource's path carries. */
    private static DynamicMessage.Builder keyed(MethodDescriptor rpc, Resource resource, String id) {
        DynamicMessage.Builder request = withPathIds(request(rpc), resource);
        if (id != null) {
            set(request, "id", id);
        }
        return request;
    }

    /**
     * The request with the parent ids REST carries in the path: a parent-id field ({@code <name>_id}, not the request
     * id) takes the create body's value of the same name, or else the id following the collection path's segment
     * naming that parent ({@code audience_group_id} ← {@code /audience-groups/<id>/members}, {@code user_account_id} ←
     * {@code /users/<id>/roles}).
     */
    private static DynamicMessage.Builder withPathIds(DynamicMessage.Builder request, Resource resource) {
        ObjectNode body = resource.create();
        for (FieldDescriptor field : request.getDescriptorForType().getFields()) {
            if (!field.getName().endsWith("_id") || "request_id".equals(field.getName()) || field.isRepeated()
                    || field.getJavaType() != FieldDescriptor.JavaType.STRING) {
                continue;
            }
            if (body.has(field.getJsonName())) {
                request.setField(field, body.path(field.getJsonName()).asString());
                continue;
            }
            String parent = field.getName().substring(0, field.getName().length() - "_id".length());
            Matcher segment = PARENT.matcher(resource.collection());
            while (segment.find()) {
                if (names(segment.group(1), parent)) {
                    request.setField(field, segment.group(2));
                    break;
                }
            }
        }
        return request;
    }

    /**
     * Whether a collection path segment names the parent a {@code <parent>_id} field refers to: the segment's singular
     * is the parent ({@code class-sections} → {@code class_section}), ends with it ({@code programming-exercises} →
     * {@code exercise}) or begins it ({@code users} → {@code user_account}).
     */
    static boolean names(String segment, String parent) {
        String plural = segment.replace('-', '_');
        String singular = plural.endsWith("ies") ? plural.substring(0, plural.length() - 3) + "y"
                : plural.matches(".*(ss|ch|sh|x)es") ? plural.substring(0, plural.length() - 2)
                : plural.endsWith("s") ? plural.substring(0, plural.length() - 1) : plural;
        return singular.equals(parent) || singular.endsWith("_" + parent) || parent.startsWith(singular + "_");
    }

    /** An update request: the row's id and etag in the resource, the changes, and a mask naming them. */
    @SuppressWarnings("unchecked")
    private DynamicMessage updateRequest(MethodDescriptor update, String field, Resource resource, String id,
                                         ObjectNode changes, String etag) {
        ObjectNode changed = changes.deepCopy();
        changed.put("id", id);
        changed.put("etag", etag);
        DynamicMessage.Builder request = withPathIds(bridge.reply(request(update), field,
                JSON.convertValue(changed, Map.class)), resource);
        FieldMask.Builder mask = FieldMask.newBuilder();
        Descriptor type = update.getInputType().findFieldByName(field).getMessageType();
        changes.propertyNames().forEach(name -> type.getFields().stream()
                .filter(f -> f.getJsonName().equals(name)).forEach(f -> mask.addPaths(f.getName())));
        return set(request, "update_mask", mask.build()).build();
    }

    private static DynamicMessage pageable(MethodDescriptor list, int page, int size, String cursor) {
        Descriptor type = list.getInputType().findFieldByName("pageable").getMessageType();
        return DynamicMessage.newBuilder(type).setField(type.findFieldByName("page"), page)
                .setField(type.findFieldByName("size"), size).setField(type.findFieldByName("cursor"), cursor).build();
    }

    /** The body with its natural keys made unique again (codes, names, titles, keys), for a second row. */
    private static ObjectNode unique(ObjectNode body) {
        ObjectNode copy = body.deepCopy();
        for (String name : List.of("code", "name", "title", "attrKey", "iso2", "iso3")) {
            if (copy.has(name)) {
                String value = copy.path(name).asString();
                copy.put(name, value.length() > 1 && value.length() <= 3 ? new StringBuilder(value).reverse().toString()
                        : value + "2");
            }
        }
        return copy;
    }

    private static List<String> ids(JsonNode items) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.path("id").asString()));
        return ids;
    }
}
