package com.mysms.core.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import com.mysms.common.v1.NullableBool;
import com.mysms.common.v1.NullableDuration;
import com.mysms.common.v1.NullableInt32;
import com.mysms.common.v1.NullableInt64;
import com.mysms.common.v1.NullableString;
import com.mysms.common.v1.NullableTimestamp;
import com.mysms.common.v1.Sort;
import com.mysms.common.v1.StringList;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.PreconditionRequiredException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.json.JsonMappers;
import com.mysms.core.paging.Page;
import com.mysms.core.validation.DtoValidator;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProtoBridgeTest {

    /** The REST rendering of the test resource, as a module's api.model DTO would be. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record WidgetDto(UUID id, @Size(max = 10) String name, Boolean isActive, String status, List<String> history,
                     OffsetDateTime createTime, Integer sortOrder, InnerDto inner, Map<String, String> byKey,
                     String bloodGroup, String etag, LocalDate startDate) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record InnerDto(String status, String note) {
    }

    /** A request whose list the contract requires (SetUserRoles' grants): empty means "none", never "not sent". */
    record RequiredHistoryDto(String name, @NotNull List<String> history) {
    }

    private static final Descriptor W = TestProtos.WIDGET;
    private final ProtoBridge bridge = new ProtoBridge(JsonMappers.create(),
            new DtoValidator(Validation.buildDefaultValidatorFactory().getValidator()),
            List.of(new ProtoEnumValues(Map.of("mysms.coretest.v1.Widget.BloodGroup.BLOOD_GROUP_A_POSITIVE", "A+"))));

    private static FieldDescriptor f(Descriptor type, String name) {
        return type.findFieldByName(name);
    }

    private static Object status(String name) {
        return W.findEnumTypeByName("Status").findValueByName(name);
    }

    private static DynamicMessage.Builder widget() throws ParseException {
        Descriptor inner = W.findNestedTypeByName("Inner");
        Descriptor entry = W.findNestedTypeByName("ByKeyEntry");
        return DynamicMessage.newBuilder(W)
                .setField(f(W, "name"), "Pump")
                .setField(f(W, "is_active"), true)
                .setField(f(W, "status"), status("STATUS_ACTIVE"))
                .addRepeatedField(f(W, "history"), status("STATUS_ACTIVE"))
                .addRepeatedField(f(W, "history"), status("STATUS_ON_HOLD"))
                .setField(f(W, "create_time"), Timestamps.parse("2026-11-11T10:00:00.123456Z"))
                .setField(f(W, "inner"), DynamicMessage.newBuilder(inner)
                        .setField(f(inner, "status"), status("STATUS_ON_HOLD")).build())
                .addRepeatedField(f(W, "by_key"), DynamicMessage.newBuilder(entry).setField(f(entry, "key"), "a")
                        .setField(f(entry, "value"), status("STATUS_ACTIVE")).build())
                .setField(f(W, "blood_group"), W.findEnumTypeByName("BloodGroup").findValueByName("BLOOD_GROUP_A_POSITIVE"))
                .setField(f(W, "etag"), "\"3\"")
                .setField(f(W, "start_date"), "2026-01-05");
    }

    @Test
    void readsAnotherServicesAnswerWithoutValidatingIt() throws Exception {
        WidgetDto dto = bridge.answer(widget().setField(f(W, "name"), "Longer than ten").build(), WidgetDto.class);
        assertThat(dto.name()).as("the owner's record as it keeps it").isEqualTo("Longer than ten");
        assertThat(dto.bloodGroup()).isEqualTo("A+");
        assertThat(dto.etag()).isEqualTo("\"3\"");
        assertThatThrownBy(() -> bridge.answer(widget().build(), Integer.class))
                .isInstanceOf(com.mysms.core.error.UnavailableException.class);
    }

    @Test
    void convertsARequestMessageToTheRestDto() throws Exception {
        WidgetDto dto = bridge.body(widget().build(), WidgetDto.class, "etag");
        assertThat(dto.id()).as("an empty string without presence is not sent").isNull();
        assertThat(dto.name()).isEqualTo("Pump");
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.status()).isEqualTo("active");
        assertThat(dto.history()).containsExactly("active", "on_hold");
        assertThat(dto.createTime()).isEqualTo(OffsetDateTime.parse("2026-11-11T10:00:00.123456Z"));
        assertThat(dto.sortOrder()).as("a default number is not sent").isNull();
        assertThat(dto.inner().status()).isEqualTo("on_hold");
        assertThat(dto.byKey()).containsEntry("a", "active");
        assertThat(dto.bloodGroup()).as("an irregular value").isEqualTo("A+");
        assertThat(dto.etag()).as("omitted: it travels as If-Match").isNull();
        assertThat(dto.startDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        WidgetDto unspecified = bridge.body(DynamicMessage.newBuilder(W).setField(f(W, "status"),
                status("STATUS_UNSPECIFIED")).build(), WidgetDto.class);
        assertThat(unspecified.status()).as("UNSPECIFIED is not set").isNull();
    }

    @Test
    void anEmptyRepeatedFieldIsAnEmptyListWhereTheBodyRequiresOne() {
        DynamicMessage none = DynamicMessage.newBuilder(W).setField(f(W, "name"), "Pump").build();

        assertThat(bridge.body(none, RequiredHistoryDto.class).history()).as("required").isEmpty();
        assertThat(bridge.body(none, WidgetDto.class).history()).as("optional: not sent").isNull();
        assertThat(bridge.body(none, WidgetDto.class).byKey()).as("a map is never made up").isNull();
    }

    @Test
    void validatesTheBodyAndRefusesUnreadableMessagesLikeRest() throws Exception {
        assertThatThrownBy(() -> bridge.body(widget().setField(f(W, "name"), "much too long a name").build(),
                WidgetDto.class)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> bridge.body(widget().setField(f(W, "start_date"), "5 Jan").build(), WidgetDto.class))
                .isInstanceOf(InvalidArgumentException.class).hasMessageNotContaining("5 Jan");
        assertThatThrownBy(() -> bridge.body(widget().build(), WidgetDto.class, "nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** A PATCH body (§7.2): Optional properties, never initialised; null = unchanged, empty = cleared. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class WidgetUpdate {
        private Optional<@NotNull @Size(max = 10) String> name;

        public Optional<String> getName() {
            return name;
        }

        public void setName(Optional<String> name) {
            this.name = name;
        }
    }

    @Test
    void readsTheThreeStatesOfAnUpdateMessageField() throws ParseException {
        NullableString cleared = NullableString.newBuilder().setNull(NullValue.NULL_VALUE).build();
        NullableString set = NullableString.newBuilder().setValue("Pump").build();
        assertThat(ProtoBridge.text(false, set)).as("unset: unchanged").isNull();
        assertThat(ProtoBridge.text(true, cleared)).as("null: cleared").isEmpty();
        assertThat(ProtoBridge.text(true, NullableString.getDefaultInstance())).as("an empty wrapper clears").isEmpty();
        assertThat(ProtoBridge.text(true, set)).contains("Pump");
        assertThat(ProtoBridge.text(true, "")).as("a plain optional sends its default").contains("");
        assertThat(ProtoBridge.text(false, "x")).isNull();

        UUID id = UUID.randomUUID();
        assertThat(bridge.uuid(true, NullableString.newBuilder().setValue(id.toString()).build(), "ownerId"))
                .contains(id);
        assertThat(bridge.uuid(true, cleared, "ownerId")).isEmpty();
        assertThat(bridge.uuid(false, cleared, "ownerId")).isNull();
        assertThat(bridge.uuid(true, id.toString(), "ownerId")).contains(id);
        assertThat(bridge.uuid(false, "", "ownerId")).isNull();
        assertThatThrownBy(() -> bridge.uuid(true, "nope", "ownerId")).isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Invalid value for ownerId.");

        assertThat(bridge.date(true, NullableString.newBuilder().setValue("2026-01-05").build(), "startDate"))
                .contains(LocalDate.of(2026, 1, 5));
        assertThat(bridge.date(true, cleared, "startDate")).isEmpty();
        assertThat(bridge.date(false, cleared, "startDate")).isNull();
        assertThat(bridge.date(true, "2026-01-05", "startDate")).contains(LocalDate.of(2026, 1, 5));
        assertThat(bridge.date(false, "", "startDate")).isNull();
        assertThatThrownBy(() -> bridge.date(true, "05/01/2026", "startDate"))
                .isInstanceOf(InvalidArgumentException.class).hasMessage("Invalid value for startDate.");

        assertThat(ProtoBridge.flag(true, false)).as("an optional bool sends false").contains(false);
        assertThat(ProtoBridge.flag(false, true)).isNull();
        assertThat(ProtoBridge.flag(true, NullableBool.newBuilder().setValue(true).build())).contains(true);
        assertThat(ProtoBridge.flag(true, NullableBool.newBuilder().setNull(NullValue.NULL_VALUE).build())).isEmpty();
        assertThat(ProtoBridge.flag(false, NullableBool.getDefaultInstance())).isNull();

        assertThat(ProtoBridge.number(true, 0)).contains(0);
        assertThat(ProtoBridge.number(false, 3)).isNull();
        assertThat(ProtoBridge.number(true, NullableInt32.newBuilder().setValue(7).build())).contains(7);
        assertThat(ProtoBridge.number(true, NullableInt32.newBuilder().setNull(NullValue.NULL_VALUE).build()))
                .isEmpty();
        assertThat(ProtoBridge.number(false, NullableInt32.getDefaultInstance())).isNull();

        assertThat(ProtoBridge.int64(true, 0L)).contains("0");
        assertThat(ProtoBridge.int64(false, 3L)).isNull();
        assertThat(ProtoBridge.int64(true, NullableInt64.newBuilder().setValue(5_000_000_000L).build()))
                .contains("5000000000");
        assertThat(ProtoBridge.int64(true, NullableInt64.newBuilder().setNull(NullValue.NULL_VALUE).build()))
                .isEmpty();
        assertThat(ProtoBridge.int64(false, NullableInt64.getDefaultInstance())).isNull();

        OffsetDateTime at = OffsetDateTime.parse("2026-11-11T10:00:00.123456Z");
        assertThat(ProtoBridge.time(true, Timestamps.parse("2026-11-11T10:00:00.123456Z"))).contains(at);
        assertThat(ProtoBridge.time(false, Timestamps.parse("2026-11-11T10:00:00Z"))).isNull();
        assertThat(ProtoBridge.time(true, NullableTimestamp.newBuilder()
                .setValue(Timestamps.parse("2026-11-11T10:00:00.123456Z")).build())).contains(at);
        assertThat(ProtoBridge.time(true, NullableTimestamp.newBuilder().setNull(NullValue.NULL_VALUE).build()))
                .isEmpty();
        assertThat(ProtoBridge.time(false, NullableTimestamp.getDefaultInstance())).isNull();

        assertThat(ProtoBridge.duration(true, Durations.fromSeconds(5400))).contains("5400s");
        assertThat(ProtoBridge.duration(false, Durations.fromSeconds(1))).isNull();
        assertThat(ProtoBridge.duration(true, NullableDuration.newBuilder().setValue(Durations.fromMillis(1500)).build()))
                .contains("1.500s");
        assertThat(ProtoBridge.duration(true, NullableDuration.newBuilder().setNull(NullValue.NULL_VALUE).build()))
                .isEmpty();
        assertThat(ProtoBridge.duration(false, NullableDuration.getDefaultInstance())).isNull();

        assertThat(ProtoBridge.texts(true, StringList.newBuilder().addValues("a").addValues("b").build()))
                .contains(List.of("a", "b"));
        assertThat(ProtoBridge.texts(true, StringList.getDefaultInstance())).as("set and empty: no entries")
                .contains(List.of());
        assertThat(ProtoBridge.texts(false, StringList.getDefaultInstance())).isNull();
    }

    @Test
    void readsEnumsJsonAndMessagesOfAnUpdateMessage() {
        assertThat(bridge.choice(true, Sort.Direction.DIRECTION_DESC, String::toUpperCase)).contains("DESC");
        assertThat(bridge.choice(true, Sort.Direction.DIRECTION_UNSPECIFIED, String::toUpperCase))
                .as("UNSPECIFIED is the enum's null").isEmpty();
        assertThat(bridge.choice(false, Sort.Direction.DIRECTION_ASC, String::toUpperCase)).isNull();
        assertThatThrownBy(() -> bridge.choice(true, Sort.Direction.UNRECOGNIZED, String::toUpperCase))
                .isInstanceOf(InvalidArgumentException.class);

        assertThat(bridge.choice(true, "DIRECTION_DESC", Sort.Direction::valueOf)).contains(Sort.Direction.DIRECTION_DESC);
        assertThat(bridge.choice(false, "DIRECTION_DESC", Sort.Direction::valueOf)).isNull();
        assertThatThrownBy(() -> bridge.choice(true, "sideways", Sort.Direction::valueOf))
                .isInstanceOf(InvalidArgumentException.class);
        assertThat(bridge.choice(true, NullableString.newBuilder().setValue("DIRECTION_ASC").build(),
                Sort.Direction::valueOf)).contains(Sort.Direction.DIRECTION_ASC);
        assertThat(bridge.choice(true, NullableString.newBuilder().setNull(NullValue.NULL_VALUE).build(),
                Sort.Direction::valueOf)).isEmpty();
        assertThat(bridge.choice(false, NullableString.getDefaultInstance(), Sort.Direction::valueOf)).isNull();

        Value tokens = Value.newBuilder().setStructValue(Struct.newBuilder()
                .putFields("primary", Value.newBuilder().setStringValue("#123456").build())).build();
        assertThat(bridge.json(true, tokens)).contains(Map.of("primary", "#123456"));
        assertThat(bridge.json(true, Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())).isEmpty();
        assertThat(bridge.json(true, Value.getDefaultInstance())).isEmpty();
        assertThat(bridge.json(false, tokens)).isNull();

        Sort criterion = Sort.newBuilder().setField("name").setDirection(Sort.Direction.DIRECTION_DESC).build();
        assertThat(bridge.message(true, criterion, SortDto.class)).contains(new SortDto("name", "desc"));
        assertThat(bridge.message(false, criterion, SortDto.class)).isNull();
    }

    /** The REST rendering of a nested message of a PATCH body. */
    record SortDto(String field, String direction) {
    }

    @Test
    void validatesTheChangesLikeRest() {
        WidgetUpdate valid = new WidgetUpdate();
        valid.setName(Optional.of("Pump"));
        assertThat(bridge.patch(valid)).isSameAs(valid);
        WidgetUpdate cleared = new WidgetUpdate();
        cleared.setName(Optional.empty());
        assertThatThrownBy(() -> bridge.patch(cleared)).as("a required field cleared")
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void convertsIdsEtagsAndPageables() {
        UUID id = UUID.randomUUID();
        assertThat(bridge.id(" " + id + " ", "widgetId")).isEqualTo(id);
        assertThatThrownBy(() -> bridge.id("", "widgetId")).isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Invalid value for widgetId.");
        assertThat(bridge.ifMatch(null)).isNull();
        assertThat(bridge.ifMatch(" ")).isNull();
        assertThat(bridge.ifMatch("3")).isEqualTo("\"3\"");
        assertThat(bridge.ifMatch("\"3\"")).isEqualTo("\"3\"");
        assertThat(bridge.ifMatch("W/\"3\"")).isEqualTo("W/\"3\"");
        assertThat(bridge.requiredIfMatch("3")).isEqualTo("\"3\"");
        assertThatThrownBy(() -> bridge.requiredIfMatch("")).isInstanceOf(PreconditionRequiredException.class);
        assertThatThrownBy(() -> bridge.requiredIfMatch("*")).isInstanceOf(PreconditionRequiredException.class);
        assertThat(bridge.orNull("")).isNull();
        assertThat(bridge.orNull("a")).isEqualTo("a");

        Descriptor sort = TestProtos.SORT;
        Descriptor pageable = TestProtos.PAGEABLE;
        Message request = DynamicMessage.newBuilder(pageable)
                .setField(f(pageable, "page"), 2).setField(f(pageable, "size"), 10).setField(f(pageable, "cursor"), "c")
                .addRepeatedField(f(pageable, "sort"), DynamicMessage.newBuilder(sort).setField(f(sort, "field"), "name")
                        .setField(f(sort, "direction"), sort.findEnumTypeByName("Direction")
                                .findValueByName("DIRECTION_DESC")).build())
                .addRepeatedField(f(pageable, "sort"), DynamicMessage.newBuilder(sort).setField(f(sort, "field"), "id")
                        .setField(f(sort, "direction"), sort.findEnumTypeByName("Direction")
                                .findValueByName("DIRECTION_ASC")).build())
                .addRepeatedField(f(pageable, "sort"), DynamicMessage.newBuilder(sort).setField(f(sort, "field"),
                        "status").build())
                .build();
        assertThat(bridge.list(request)).isEqualTo(new ProtoBridge.ListArguments(2, 10, "c",
                List.of("name,desc", "id,asc", "status")));
        assertThat(bridge.list(DynamicMessage.getDefaultInstance(pageable)))
                .isEqualTo(new ProtoBridge.ListArguments(null, null, null, null));
    }

    @Test
    void writesAResponseFromTheRestDto() throws Exception {
        WidgetDto dto = new WidgetDto(UUID.randomUUID(), "Pump", false, "on_hold", List.of("active"),
                OffsetDateTime.parse("2026-11-11T10:00:00Z"), 4, new InnerDto("active", "n"), Map.of("k", "on_hold"),
                "A+", "\"2\"", LocalDate.of(2026, 1, 5));
        Message reply = bridge.reply(DynamicMessage.newBuilder(TestProtos.GET_RESPONSE), "widget", dto).build();
        Message widget = (Message) reply.getField(f(TestProtos.GET_RESPONSE, "widget"));
        assertThat(widget.getField(f(W, "id"))).isEqualTo(dto.id().toString());
        assertThat(widget.getField(f(W, "status"))).isEqualTo(status("STATUS_ON_HOLD"));
        assertThat(widget.getField(f(W, "blood_group"))).isEqualTo(W.findEnumTypeByName("BloodGroup")
                .findValueByName("BLOOD_GROUP_A_POSITIVE"));
        Message created = (Message) widget.getField(f(W, "create_time"));
        assertThat(Timestamps.toString(com.google.protobuf.Timestamp.parseFrom(created.toByteString())))
                .isEqualTo("2026-11-11T10:00:00Z");
        assertThat(widget.getField(f(W, "etag"))).isEqualTo("\"2\"");
        assertThat(widget.getField(f(W, "start_date"))).isEqualTo("2026-01-05");
        assertThat(widget.getField(f(W, "sort_order"))).isEqualTo(4);
        assertThatThrownBy(() -> bridge.reply(DynamicMessage.newBuilder(TestProtos.GET_RESPONSE), "widget",
                new WidgetDto(null, null, null, "archived", null, null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("archived");
    }

    @Test
    void writesAListResponseWithItsPagination() {
        WidgetDto one = new WidgetDto(UUID.randomUUID(), "A", true, null, null, null, null, null, null, null, null, null);
        WidgetDto two = new WidgetDto(UUID.randomUUID(), "B", true, null, null, null, null, null, null, null, null, null);
        Descriptor list = TestProtos.LIST_RESPONSE;
        Message middle = bridge.page(DynamicMessage.newBuilder(list), new Page<>(List.of(one, two), 2, 2, "next"))
                .build();
        assertThat(middle.getRepeatedFieldCount(f(list, "items"))).isEqualTo(2);
        Message pagination = (Message) middle.getField(f(list, "pagination"));
        Descriptor p = pagination.getDescriptorForType();
        assertThat(pagination.getField(f(p, "size"))).isEqualTo(2);
        assertThat(pagination.getField(f(p, "first"))).isEqualTo(1);
        assertThat(pagination.getField(f(p, "previous"))).isEqualTo(1);
        assertThat(pagination.getField(f(p, "next"))).isEqualTo(3);
        assertThat(pagination.getField(f(p, "page"))).isEqualTo(2);
        assertThat(pagination.getField(f(p, "next_cursor"))).isEqualTo("next");
        Message last = bridge.page(DynamicMessage.newBuilder(list), new Page<>(List.of(one), 1, 2, null)).build();
        Message lastPagination = (Message) last.getField(f(list, "pagination"));
        assertThat(lastPagination.getField(f(p, "next"))).isEqualTo(0);
        assertThat(lastPagination.getField(f(p, "previous"))).isEqualTo(0);
        assertThat(lastPagination.getField(f(p, "next_cursor"))).isEqualTo("");
    }
}
