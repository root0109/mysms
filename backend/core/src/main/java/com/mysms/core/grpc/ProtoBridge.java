package com.mysms.core.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Duration;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.JsonFormat;
import com.mysms.common.v1.NullableBool;
import com.mysms.common.v1.NullableDuration;
import com.mysms.common.v1.NullableInt32;
import com.mysms.common.v1.NullableInt64;
import com.mysms.common.v1.NullableString;
import com.mysms.common.v1.NullableTimestamp;
import com.mysms.common.v1.StringList;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.PreconditionRequiredException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.validation.DtoValidator;
import io.grpc.stub.StreamObserver;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.lang.Nullable;
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
 * <li>proto3 defaults: a field without presence that holds its default is "not sent", like an absent JSON
 * property;</li>
 * <li>updates: a {@code <Resource>Update} message is read field by field into REST's {@code <Resource>Update} by the
 * resource's own proto bridge, through the {@code Nullable*}/{@code StringList} readers here — unset is absent
 * (unchanged), a wrapper holding null (or an enum's UNSPECIFIED, a {@code Value} holding null) is cleared, a value is
 * set;</li>
 * <li>paging: {@code pageable} ({@code page}, {@code size}, {@code sort}, {@code cursor}) ↔ the list arguments, and a
 * {@link Page} ↔ {@code items} + {@code pagination};</li>
 * <li>{@code etag} ↔ If-Match, path ids ↔ UUIDs;</li>
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
    private static final String WELL_KNOWN = "google.protobuf.";
    private static final String NOT_READABLE = "The request message is not valid for this operation.";

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
        ObjectNode tree = toRestTree(message);
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
            return json.treeToValue(toRestTree(message), type);
        } catch (JacksonException | InvalidArgumentException e) {
            throw new UnavailableException("The answer of another service does not fit this service's contract.");
        }
    }

    // ---------------------------------------------------------------- updates: <Resource>Update → REST's PATCH body

    /**
     * The changes a resource's proto bridge read from its {@code <Resource>Update} message, validated like REST's
     * {@code @Valid} body: a required field set to blank, a value out of range, a null where the type argument is
     * {@code @NotNull} (an enum's UNSPECIFIED) is 422, as on REST.
     */
    public <T> T patch(T changes) {
        validator.validate(changes);
        return changes;
    }

    /** A text field that is never cleared ({@code optional string}): unset → absent, set → the value. */
    public static @Nullable Optional<String> text(boolean present, String value) {
        return present ? Optional.of(value) : null;
    }

    /** A text field that may be cleared: unset → absent, the wrapper's null → cleared, its value → set. */
    public static @Nullable Optional<String> text(boolean present, NullableString value) {
        if (!present) {
            return null;
        }
        return value.getKindCase() == NullableString.KindCase.VALUE ? Optional.of(value.getValue()) : Optional.empty();
    }

    /** A UUID field that is never cleared; a value that is no UUID is 400, like REST's binding. */
    public @Nullable Optional<UUID> uuid(boolean present, String value, String name) {
        return present ? Optional.of(id(value, name)) : null;
    }

    /** A UUID field that may be cleared; a value that is no UUID is 400. */
    public @Nullable Optional<UUID> uuid(boolean present, NullableString value, String name) {
        Optional<String> text = text(present, value);
        return text == null ? null : text.map(id -> id(id, name));
    }

    /** An ISO-8601 date field that is never cleared; a value that is no date is 400. */
    public @Nullable Optional<LocalDate> date(boolean present, String value, String name) {
        return present ? Optional.of(day(value, name)) : null;
    }

    /** An ISO-8601 date field that may be cleared; a value that is no date is 400. */
    public @Nullable Optional<LocalDate> date(boolean present, NullableString value, String name) {
        Optional<String> text = text(present, value);
        return text == null ? null : text.map(day -> day(day, name));
    }

    /** A flag that is never cleared ({@code optional bool}: false is sent, unlike a plain proto3 bool). */
    public static @Nullable Optional<Boolean> flag(boolean present, boolean value) {
        return present ? Optional.of(value) : null;
    }

    /** A flag that may be cleared. */
    public static @Nullable Optional<Boolean> flag(boolean present, NullableBool value) {
        if (!present) {
            return null;
        }
        return value.getKindCase() == NullableBool.KindCase.VALUE ? Optional.of(value.getValue()) : Optional.empty();
    }

    /** A 32-bit number that is never cleared ({@code optional int32}: 0 is sent). */
    public static @Nullable Optional<Integer> number(boolean present, int value) {
        return present ? Optional.of(value) : null;
    }

    /** A 32-bit number that may be cleared. */
    public static @Nullable Optional<Integer> number(boolean present, NullableInt32 value) {
        if (!present) {
            return null;
        }
        return value.getKindCase() == NullableInt32.KindCase.VALUE ? Optional.of(value.getValue()) : Optional.empty();
    }

    /** A 64-bit number that is never cleared ({@code optional int64}), as REST's decimal string. */
    public static @Nullable Optional<String> int64(boolean present, long value) {
        return present ? Optional.of(Long.toString(value)) : null;
    }

    /** A 64-bit number that may be cleared, as REST's decimal string. */
    public static @Nullable Optional<String> int64(boolean present, NullableInt64 value) {
        if (!present) {
            return null;
        }
        return value.getKindCase() == NullableInt64.KindCase.VALUE ? Optional.of(Long.toString(value.getValue()))
                : Optional.empty();
    }

    /** An instant that is never cleared (a {@code Timestamp} message field), in UTC like REST's. */
    public static @Nullable Optional<OffsetDateTime> time(boolean present, Timestamp value) {
        return present ? Optional.of(instant(value)) : null;
    }

    /** An instant that may be cleared. */
    public static @Nullable Optional<OffsetDateTime> time(boolean present, NullableTimestamp value) {
        if (!present) {
            return null;
        }
        return value.getKindCase() == NullableTimestamp.KindCase.VALUE ? Optional.of(instant(value.getValue()))
                : Optional.empty();
    }

    /** A duration that is never cleared (a {@code Duration} message field), as REST's {@code "<seconds>s"}. */
    public static @Nullable Optional<String> duration(boolean present, Duration value) {
        return present ? Optional.of(Durations.toString(value)) : null;
    }

    /** A duration that may be cleared, as REST's {@code "<seconds>s"}. */
    public static @Nullable Optional<String> duration(boolean present, NullableDuration value) {
        if (!present) {
            return null;
        }
        return value.getKindCase() == NullableDuration.KindCase.VALUE ? Optional.of(Durations.toString(value.getValue()))
                : Optional.empty();
    }

    /**
     * An enum field ({@code optional <Enum>}): unset → absent; its {@code _UNSPECIFIED} value is its null (cleared, or
     * 422 where the field is required); a value → the REST enum of the same name ({@code restValue}: the DTO enum's
     * {@code fromValue}). A number the contract does not know is 400.
     */
    @Nullable
    public <E> Optional<E> choice(boolean present, ProtocolMessageEnum value, Function<String, E> restValue) {
        if (!present) {
            return null;
        }
        EnumValueDescriptor named;
        try {
            named = value.getValueDescriptor();
        } catch (IllegalStateException | IllegalArgumentException unknownNumber) { // UNRECOGNIZED
            throw new InvalidArgumentException(NOT_READABLE);
        }
        String rest = restValue(named.getType(), named.getName());
        return rest == null ? Optional.empty() : Optional.of(restEnum(rest, restValue));
    }

    /**
     * An enum field the proto carries as its REST value in text, as the resource message does ({@code optional
     * string}): unset → absent, a value → the REST enum ({@code restValue}: the DTO enum's {@code fromValue}); a value
     * the contract does not know is 400.
     */
    @Nullable
    public <E> Optional<E> choice(boolean present, String value, Function<String, E> restValue) {
        return present ? Optional.of(restEnum(value, restValue)) : null;
    }

    /** An enum field carried as text that may be cleared: the wrapper's null → cleared. */
    @Nullable
    public <E> Optional<E> choice(boolean present, NullableString value, Function<String, E> restValue) {
        Optional<String> text = text(present, value);
        return text == null ? null : text.map(rest -> restEnum(rest, restValue));
    }

    private static <E> E restEnum(String value, Function<String, E> restValue) {
        try {
            return restValue.apply(value);
        } catch (IllegalArgumentException unknown) {
            throw new InvalidArgumentException(NOT_READABLE);
        }
    }

    /** A free-form JSON field ({@code google.protobuf.Value}): unset → absent, a null value → cleared, else set. */
    public @Nullable Optional<Object> json(boolean present, Value value) {
        if (!present) {
            return null;
        }
        if (value.getKindCase() == Value.KindCase.NULL_VALUE || value.getKindCase() == Value.KindCase.KIND_NOT_SET) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.readValue(PRINTER.print(value), Object.class));
        } catch (InvalidProtocolBufferException | JacksonException e) {
            throw new InvalidArgumentException(NOT_READABLE);
        }
    }

    /** A list field that is never cleared ({@code optional StringList}): unset → absent, set → its values. */
    public static @Nullable Optional<List<String>> texts(boolean present, StringList value) {
        return present ? Optional.of(List.copyOf(value.getValuesList())) : null;
    }

    /**
     * A message field that is never cleared: unset → absent, set → the message as the REST DTO (enums and defaults as
     * for a request body), validated with the whole patch by {@link #patch}.
     */
    @Nullable
    public <T> Optional<T> message(boolean present, MessageOrBuilder value, Class<T> type) {
        if (!present) {
            return null;
        }
        try {
            return Optional.of(json.treeToValue(toRestTree(value), type));
        } catch (JacksonException e) {
            throw new InvalidArgumentException(NOT_READABLE);
        }
    }

    private static OffsetDateTime instant(Timestamp value) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(value.getSeconds(), value.getNanos()), ZoneOffset.UTC);
    }

    private static LocalDate day(String value, String name) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidArgumentException("Invalid value for " + name + ".");
        }
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

    private ObjectNode toRestTree(MessageOrBuilder message) {
        ObjectNode tree;
        try {
            tree = (ObjectNode) json.readTree(PRINTER.print(message));
        } catch (InvalidProtocolBufferException | JacksonException e) {
            throw new InvalidArgumentException(NOT_READABLE);
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
        if (field.getJavaType() == FieldDescriptor.JavaType.ENUM && value.isString()
                && !field.getEnumType().getFullName().startsWith(WELL_KNOWN)) { // NullValue is JSON's null
            String converted = toRest ? restValue(field.getEnumType(), value.asString())
                    : protoValue(field.getEnumType(), value.asString());
            return converted == null ? null : JsonNodeFactory.instance.stringNode(converted);
        }
        if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE && value instanceof ObjectNode object
                && !field.getMessageType().getFullName().startsWith(WELL_KNOWN)) {
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
