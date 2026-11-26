package com.mysms.core.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.util.Timestamps;
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
        private Optional<Boolean> isActive;
        private Optional<Integer> sortOrder;
        private Optional<List<String>> history;
        private Optional<String> startDate;

        public Optional<String> getName() {
            return name;
        }

        public void setName(Optional<String> name) {
            this.name = name;
        }

        public Optional<Boolean> getIsActive() {
            return isActive;
        }

        public void setIsActive(Optional<Boolean> isActive) {
            this.isActive = isActive;
        }

        public Optional<Integer> getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Optional<Integer> sortOrder) {
            this.sortOrder = sortOrder;
        }

        public Optional<List<String>> getHistory() {
            return history;
        }

        public void setHistory(Optional<List<String>> history) {
            this.history = history;
        }

        public Optional<String> getStartDate() {
            return startDate;
        }

        public void setStartDate(Optional<String> startDate) {
            this.startDate = startDate;
        }
    }

    /** A PATCH body whose list may change but never be cleared. */
    public static final class RequiredHistoryUpdate {
        private Optional<@NotNull List<String>> history;

        public Optional<List<String>> getHistory() {
            return history;
        }

        public void setHistory(Optional<List<String>> history) {
            this.history = history;
        }
    }

    @Test
    void anUpdateMaskDecidesTheThreeStatesOfTheUpdateDto() {
        DynamicMessage changes = DynamicMessage.newBuilder(W).setField(f(W, "is_active"), false)
                .setField(f(W, "name"), "Pump").setField(f(W, "start_date"), "2026-01-05").build();
        FieldMask mask = FieldMask.newBuilder().addPaths("isActive").addPaths("sort_order").addPaths("name")
                .addPaths("history").build();
        WidgetUpdate masked = bridge.changes(changes, mask, WidgetUpdate.class);
        assertThat(masked.getIsActive()).as("a masked flag (by its JSON name) is sent as its default").contains(false);
        assertThat(masked.getSortOrder()).as("a masked number is sent as its default").contains(0);
        assertThat(masked.getName()).as("masked and held: set").contains("Pump");
        assertThat(masked.getHistory()).as("masked, not held, not required: cleared").isEmpty();
        assertThat(masked.getStartDate()).as("held but unmasked: unchanged, never replaced").isNull();

        WidgetUpdate unmasked = bridge.changes(changes, FieldMask.getDefaultInstance(), WidgetUpdate.class);
        assertThat(unmasked.getIsActive()).as("without a mask a default is not sent").isNull();
        assertThat(unmasked.getName()).contains("Pump");
        assertThat(unmasked.getHistory()).isNull();
        assertThat(unmasked.getStartDate()).contains("2026-01-05");
    }

    @Test
    void aMaskMayNotClearARequiredFieldNorNameAFieldTheUpdateLacks() {
        DynamicMessage none = DynamicMessage.newBuilder(W).build();
        assertThatThrownBy(() -> bridge.changes(none, FieldMask.newBuilder().addPaths("name").build(),
                WidgetUpdate.class)).as("clearing a required field").isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> bridge.changes(none, FieldMask.newBuilder().addPaths("history").build(),
                RequiredHistoryUpdate.class)).as("a required list masked but empty is refused, never made up")
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> bridge.changes(none, FieldMask.newBuilder().addPaths("etag").build(),
                WidgetUpdate.class)).isInstanceOf(InvalidArgumentException.class).hasMessageContaining("etag");
        assertThatThrownBy(() -> bridge.changes(none, FieldMask.newBuilder().addPaths("nope").build(),
                WidgetUpdate.class)).isInstanceOf(InvalidArgumentException.class);
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
