package com.mysms.core.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.JsonFormat;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.PreconditionRequiredException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.validation.DtoValidator;
import io.grpc.stub.StreamObserver;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * The proto ↔ DTO bridge of the gRPC resolvers. The proto and the REST contract are two renderings of one
 * specification, so a message is converted through its
 * canonical JSON — protobuf's JsonFormat (lowerCamelCase field names, RFC 3339 timestamps, int64 as strings) on one
 * side and core's {@code JsonMappers} on the other — with the few places the renderings differ handled once, here:
 * <ul>
 * <li>enums: {@code STATUS_PRESENT} ↔ {@code "present"} (the value name without the enum's prefix, which is the name
 * of its {@code _UNSPECIFIED = 0} value; irregular values come from {@link ProtoEnumValues} beans); UNSPECIFIED is
 * "not set";</li>
 * <li>proto3 defaults: a field without presence that holds its default is "not sent", like an absent JSON property —
 * except a bool or number named in an update mask, which is sent as {@code false} / {@code 0};</li>
 * <li>paging: {@code pageable} ({@code page}, {@code size}, {@code sort}, {@code cursor}) ↔ the list arguments, and a
 * {@link Page} ↔ {@code items} + {@code pagination};</li>
 * <li>{@code etag} ↔ If-Match, {@code update_mask} ↔ updateMask, path ids ↔ UUIDs;</li>
 * <li>an empty repeated field of a request body is {@code []} where the DTO requires the list (proto3 cannot tell an
 * empty list from an absent one, and a required list is never absent).</li>
 * </ul>
 * A request body is validated with the same Jakarta validator as REST's {@code @Valid}. Conversion failures are
 * answered as on REST: 400 INVALID_ARGUMENT, never a library message.
 */
public final class ProtoBridge {

    private static final JsonFormat.Printer PRINTER = JsonFormat.printer().omittingInsignificantWhitespace();
    private static final JsonFormat.Parser PARSER = JsonFormat.parser().ignoringUnknownFields();
    private static final String UNSPECIFIED = "UNSPECIFIED";
    private static final String NOT_READABLE = "The request message is not valid for this operation.";
    private static final Set<FieldDescriptor.JavaType> SCALARS_SENT_WHEN_MASKED = Set.of(FieldDescriptor.JavaType.BOOLEAN,
            FieldDescriptor.JavaType.INT, FieldDescriptor.JavaType.LONG, FieldDescriptor.JavaType.FLOAT,
            FieldDescriptor.JavaType.DOUBLE);

    private final JsonMapper json;
    private final DtoValidator validator;
    private final Map<String, String> restValues;
    private final Map<String, String> protoValues;

    public ProtoBridge(JsonMapper json, DtoValidator validator, List<ProtoEnumValues> irregularEnums) {
        this.json = json;
        this.validator = validator;
        Map<String, String> rest = new HashMap<>();
        Map<String, String> proto = new HashMap<>();
        for (ProtoEnumValues values : irregularEnums) {
            values.restValues().forEach((value, restValue) -> {
                rest.put(value, restValue);
                int dot = value.lastIndexOf('.');
                proto.put(value.substring(0, dot) + "#" + restValue, value.substring(dot + 1));
            });
        }
        this.restValues = Map.copyOf(rest);
        this.protoValues = Map.copyOf(proto);
    }

    /**
     * The list arguments of a {@code pageable} ({@code mysms.common.v1.PageableRequest}): page and size (0 = the
     * default), the sort criteria as REST's {@code field,asc|desc}, the cursor (empty = none).
     */
    public record ListArguments(Integer page, Integer size, String cursor, List<String> sort) {

        public ListArguments {
            sort = sort == null ? null : List.copyOf(sort);
        }
    }

    // ---------------------------------------------------------------- requests: proto → DTO

    /**
     * A request body: the message as the REST DTO, validated like REST's {@code @Valid}. Fields that travel elsewhere
     * on REST (path parameters, the idempotency key) are named in {@code omitted} by their proto names.
     */
    public <T> T body(MessageOrBuilder message, Class<T> type, String... omitted) {
        ObjectNode tree = toRestTree(message, Set.of());
        for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
            if (field.isRepeated() && !field.isMapField() && !tree.has(field.getJsonName())
                    && required(type, field.getJsonName())) {
                tree.putArray(field.getJsonName()); // proto3 sends an empty list as nothing; REST's [] is meant
            }
        }
        for (String field : omitted) {
            tree.remove(field(message.getDescriptorForType(), field).getJsonName());
        }
        return validated(tree, type);
    }

    /**
     * A message another module answered (through a {@code <Callee>GrpcClient}), as this module's DTO of the same
     * contract schema: converted like a request body, not validated — it is the owner's record as the owner keeps it.
     * Fails with 503 when it does not fit, which only a contract drift between the two copies of the proto can cause.
     */
    public <T> T answer(MessageOrBuilder message, Class<T> type) {
        try {
            return json.treeToValue(toRestTree(message, Set.of()), type);
        } catch (JacksonException | InvalidArgumentException e) {
            throw new UnavailableException("The answer of another service does not fit this service's contract.");
        }
    }

    /**
     * The changes of an update: the resource message as the REST update DTO. With a mask, a masked bool or number
     * that holds its default is sent as that value (false, 0), so a mask can switch a flag off; every other default
     * is "not sent", which with a mask clears the field and without one leaves it unchanged — REST's rules.
     */
    public <T> T changes(MessageOrBuilder resource, FieldMask mask, Class<T> type) {
        Set<String> masked = new HashSet<>();
        for (String path : mask.getPathsList()) {
            masked.add(path.split("\\.", 2)[0]);
        }
        return validated(toRestTree(resource, masked), type);
    }

    /** REST's {@code updateMask} (comma-separated lowerCamelCase names), or null for an empty mask. */
    public String updateMask(FieldMask mask) {
        return mask.getPathsCount() == 0 ? null : FieldMaskUtil.toJsonString(mask);
    }

    /** A path id: a UUID, or 400 like REST's path binding. */
    public UUID id(String value, String name) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidArgumentException("Invalid value for " + name + ".");
        }
    }

    /**
     * The If-Match value an {@code etag} field stands for: the ETag as the API returned it ({@code "\"3\""}) or bare
     * ({@code 3}); null when empty, which the service answers 428 PRECONDITION_REQUIRED on an update or delete.
     */
    public String ifMatch(String etag) {
        if (etag == null || etag.isBlank()) {
            return null;
        }
        String trimmed = etag.trim();
        return trimmed.startsWith("\"") || trimmed.startsWith("W/") ? trimmed : "\"" + trimmed + "\"";
    }

    /**
     * The If-Match of an update or delete, where REST requires the header: an empty {@code etag} (or {@code *}) is
     * 428 PRECONDITION_REQUIRED before anything else runs, as on REST.
     */
    public String requiredIfMatch(String etag) {
        String ifMatch = ifMatch(etag);
        if (ifMatch == null || "\"*\"".equals(ifMatch)) {
            throw new PreconditionRequiredException("etag with the ETag of the last read is required.");
        }
        return ifMatch;
    }

    /** An optional text parameter ({@code filter}, a query): proto3's empty string is "not sent". */
    public String orNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /** Answers a unary call. */
    public static <M extends Message> void send(StreamObserver<M> out, M message) {
        out.onNext(message);
        out.onCompleted();
    }

    /** The list arguments of a request's {@code pageable}. */
    public ListArguments list(MessageOrBuilder pageable) {
        Descriptor type = pageable.getDescriptorForType();
        int page = ((Number) pageable.getField(field(type, "page"))).intValue();
        int size = ((Number) pageable.getField(field(type, "size"))).intValue();
        String cursor = (String) pageable.getField(field(type, "cursor"));
        List<String> sort = new ArrayList<>();
        FieldDescriptor sorts = field(type, "sort");
        for (int i = 0; i < pageable.getRepeatedFieldCount(sorts); i++) {
            Message criterion = (Message) pageable.getRepeatedField(sorts, i);
            Descriptor criterionType = criterion.getDescriptorForType();
            String name = (String) criterion.getField(field(criterionType, "field"));
            EnumValueDescriptor direction = (EnumValueDescriptor) criterion.getField(field(criterionType, "direction"));
            sort.add(direction.getName().endsWith("_DESC") ? name + ",desc"
                    : direction.getName().endsWith("_ASC") ? name + ",asc" : name);
        }
        return new ListArguments(page == 0 ? null : page, size == 0 ? null : size, cursor.isEmpty() ? null : cursor,
                sort.isEmpty() ? null : sort);
    }

    // ---------------------------------------------------------------- responses: DTO → proto

    /** The response with one field set from a DTO: {@code reply(GetCountryResponse.newBuilder(), "country", dto)}. */
    public <B extends Message.Builder> B reply(B builder, String field, Object dto) {
        ObjectNode tree = JsonNodeFactory.instance.objectNode();
        tree.set(field(builder.getDescriptorForType(), field).getJsonName(), json.valueToTree(dto));
        return merge(builder, tree);
    }

    /** A list response: the page's items and its {@code pagination}. */
    public <B extends Message.Builder> B page(B builder, Page<?> page) {
        ObjectNode tree = JsonNodeFactory.instance.objectNode();
        ArrayNode items = tree.putArray("items");
        page.items().forEach(item -> items.add((JsonNode) json.valueToTree(item)));
        int number = page.page();
        ObjectNode pagination = tree.putObject("pagination");
        pagination.put("size", page.items().size());
        pagination.put("first", 1);
        pagination.put("previous", number > 1 && PageRequest.reachableByOffset(number - 1, page.size()) ? number - 1 : 0);
        pagination.put("next", page.hasNext() ? number + 1 : 0);
        pagination.put("page", number);
        pagination.put("nextCursor", page.nextCursor());
        return merge(builder, tree);
    }

    private <B extends Message.Builder> B merge(B builder, ObjectNode tree) {
        toProto(builder.getDescriptorForType(), tree);
        try {
            PARSER.merge(json.writeValueAsString(tree), builder);
        } catch (InvalidProtocolBufferException e) {
            // the DTO and the proto are renderings of one contract; a mismatch is a programming error
            throw new IllegalStateException("the response does not fit " + builder.getDescriptorForType().getFullName(), e);
        }
        return builder;
    }

    // ---------------------------------------------------------------- conversion

    private ObjectNode toRestTree(MessageOrBuilder message, Set<String> masked) {
        ObjectNode tree;
        try {
            tree = (ObjectNode) json.readTree(PRINTER.print(message));
        } catch (InvalidProtocolBufferException | JacksonException e) {
            throw new InvalidArgumentException(NOT_READABLE);
        }
        for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
            if (masked.contains(field.getName()) && !field.hasPresence() && !field.isRepeated()
                    && SCALARS_SENT_WHEN_MASKED.contains(field.getJavaType()) && !tree.has(field.getJsonName())) {
                tree.set(field.getJsonName(), json.valueToTree(message.getField(field)));
            }
        }
        toRest(message.getDescriptorForType(), tree);
        return tree;
    }

    /**
     * Whether a request DTO requires the property (the contract's {@code required}, rendered as {@code @NotNull} on
     * its getter or field): a required list left empty over gRPC is then REST's {@code []}, never "not sent".
     */
    private static boolean required(Class<?> type, String property) {
        String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            if (type.getMethod("get" + suffix).isAnnotationPresent(NotNull.class)) {
                return true;
            }
        } catch (NoSuchMethodException noGetter) {
            // a record or a bean without a getter: its field carries the constraint
        }
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            try {
                return owner.getDeclaredField(property).isAnnotationPresent(NotNull.class);
            } catch (NoSuchFieldException notDeclaredHere) {
                // look in the superclass
            }
        }
        return false;
    }

    private <T> T validated(ObjectNode tree, Class<T> type) {
        T dto;
        try {
            dto = json.treeToValue(tree, type);
        } catch (JacksonException e) {
            throw new InvalidArgumentException(NOT_READABLE);
        }
        validator.validate(dto);
        return dto;
    }

    /** Proto enum names → REST values, in place, through the message's fields (well-known types hold none). */
    private void toRest(Descriptor type, ObjectNode node) {
        walk(type, node, true);
    }

    /** REST values → proto enum names, in place. */
    private void toProto(Descriptor type, ObjectNode node) {
        walk(type, node, false);
    }

    private void walk(Descriptor type, ObjectNode node, boolean toRest) {
        for (FieldDescriptor field : type.getFields()) {
            JsonNode value = node.get(field.getJsonName());
            if (value == null || value.isNull()) {
                continue;
            }
            if (field.isMapField()) {
                FieldDescriptor mapValue = field.getMessageType().findFieldByName("value");
                if (value instanceof ObjectNode map) {
                    for (String key : new ArrayList<>(map.propertyNames())) {
                        replace(map, key, convert(mapValue, map.get(key), toRest));
                    }
                }
            } else if (field.isRepeated() && value instanceof ArrayNode array) {
                for (int i = 0; i < array.size(); i++) {
                    JsonNode converted = convert(field, array.get(i), toRest);
                    array.set(i, converted == null ? JsonNodeFactory.instance.nullNode() : converted);
                }
            } else {
                replace(node, field.getJsonName(), convert(field, value, toRest));
            }
        }
    }

    private static void replace(ObjectNode node, String name, JsonNode value) {
        if (value == null) {
            node.remove(name);
        } else {
            node.set(name, value);
        }
    }

    /** The converted value; null when it means "not set" (an UNSPECIFIED enum). */
    private JsonNode convert(FieldDescriptor field, JsonNode value, boolean toRest) {
        if (field.getJavaType() == FieldDescriptor.JavaType.ENUM && value.isString()) {
            String converted = toRest ? restValue(field.getEnumType(), value.asString())
                    : protoValue(field.getEnumType(), value.asString());
            return converted == null ? null : JsonNodeFactory.instance.stringNode(converted);
        }
        if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE && value instanceof ObjectNode object
                && !field.getMessageType().getFullName().startsWith("google.protobuf.")) {
            walk(field.getMessageType(), object, toRest);
        }
        return value;
    }

    private String restValue(EnumDescriptor type, String name) {
        String irregular = restValues.get(type.getFullName() + "." + name);
        if (irregular != null) {
            return irregular;
        }
        String prefix = prefix(type);
        if (!name.startsWith(prefix) || name.equals(prefix + UNSPECIFIED)) {
            return null;
        }
        return name.substring(prefix.length()).toLowerCase(Locale.ROOT);
    }

    private String protoValue(EnumDescriptor type, String restValue) {
        String irregular = protoValues.get(type.getFullName() + "#" + restValue);
        if (irregular != null) {
            return irregular;
        }
        String name = prefix(type) + restValue.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
        if (type.findValueByName(name) == null) {
            throw new IllegalStateException("no " + type.getFullName() + " value for \"" + restValue + "\"");
        }
        return name;
    }

    /** The enum's value prefix: the name of its {@code <ENUM>_UNSPECIFIED = 0} value without UNSPECIFIED. */
    private static String prefix(EnumDescriptor type) {
        String unspecified = type.findValueByNumber(0).getName();
        if (!unspecified.endsWith(UNSPECIFIED)) {
            throw new IllegalStateException(type.getFullName() + " does not start with an _UNSPECIFIED value");
        }
        return unspecified.substring(0, unspecified.length() - UNSPECIFIED.length());
    }

    private static FieldDescriptor field(Descriptor type, String name) {
        FieldDescriptor field = type.findFieldByName(name);
        if (field == null) {
            throw new IllegalArgumentException(type.getFullName() + " has no field " + name);
        }
        return field;
    }
}
