package com.mysms.core.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.PageCursor;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.ValueType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The parity kit's own rules: how it names a List RPC, compares cursors and compares values across renderings. */
class GrpcParityTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PACKAGE = ".mysms.kittest.v1.";

    private static JsonNode json(String text) {
        return JSON.readTree(text);
    }

    // ------------------------------------------------------------------ List RPCs from the contract

    private static FieldDescriptorProto field(String name, int number, String type, boolean repeated) {
        FieldDescriptorProto.Builder field = FieldDescriptorProto.newBuilder().setName(name).setNumber(number)
                .setLabel(repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL);
        return type == null ? field.setType(Type.TYPE_STRING).build()
                : field.setType(Type.TYPE_MESSAGE).setTypeName(PACKAGE + type).build();
    }

    private static DescriptorProto message(String name, FieldDescriptorProto... fields) {
        DescriptorProto.Builder message = DescriptorProto.newBuilder().setName(name);
        for (FieldDescriptorProto field : fields) {
            message.addField(field);
        }
        return message.build();
    }

    private static MethodDescriptorProto method(String name, String input, String output) {
        return MethodDescriptorProto.newBuilder().setName(name).setInputType(PACKAGE + input)
                .setOutputType(PACKAGE + output).build();
    }

    /** Resources whose plurals no suffix rule forms, one listed two ways. */
    private static FileDescriptor contract() throws Exception {
        FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder().setName("mysms/kittest/v1/kittest.proto")
                .setPackage("mysms.kittest.v1").setSyntax("proto3")
                .addMessageType(message("Empty", field("id", 1, null, false)));
        ServiceDescriptorProto.Builder service = ServiceDescriptorProto.newBuilder().setName("KitService");
        for (String name : List.of("Batch", "Media", "RubricCriterion", "Staff", "Category", "StudentEnrollment")) {
            file.addMessageType(message(name, field("id", 1, null, false)))
                    .addMessageType(message("Get" + name + "Response", field("item", 1, name, false)));
            service.addMethod(method("Get" + name, "Empty", "Get" + name + "Response"));
        }
        for (String[] list : List.of(new String[] {"ListBatches", "Batch"}, new String[] {"ListMedia", "Media"},
                new String[] {"ListRubricCriteria", "RubricCriterion"}, new String[] {"ListStaff", "Staff"},
                new String[] {"ListCategories", "Category"}, new String[] {"ListClassRoster", "StudentEnrollment"},
                new String[] {"ListStudentEnrollments", "StudentEnrollment"})) {
            file.addMessageType(message(list[0] + "Response", field("items", 1, list[1], true)));
            service.addMethod(method(list[0], "Empty", list[0] + "Response"));
        }
        return FileDescriptor.buildFrom(file.addService(service).build(), new FileDescriptor[0]);
    }

    @Test
    void theListRpcIsTheOneWhoseItemsAreTheResource() throws Exception {
        GrpcParityContract kit = new GrpcParityContract(contract(), null, null, null, null);

        assertThat(kit.listRpc("Batch").getName()).isEqualTo("ListBatches");
        assertThat(kit.listRpc("Media").getName()).isEqualTo("ListMedia");
        assertThat(kit.listRpc("RubricCriterion").getName()).isEqualTo("ListRubricCriteria");
        assertThat(kit.listRpc("Staff").getName()).isEqualTo("ListStaff");
        assertThat(kit.listRpc("Category").getName()).isEqualTo("ListCategories");
        assertThat(kit.listRpc("StudentEnrollment").getName()).as("listed two ways: the one named like it")
                .isEqualTo("ListStudentEnrollments");
        assertThatThrownBy(() -> kit.listRpc("Nothing")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aResourceWithoutAnUpdateBodyIsReadFromItsSpec() {
        GrpcParityContract.Resource payment = GrpcParityContract.Resource.of(json(
                "{\"name\":\"Payment\",\"collection\":\"/v1/finance/payments\",\"ops\":[\"Get\",\"List\"],"
                        + "\"seed\":[\"INSERT 1\"],\"seedId\":\"abc\",\"partition\":\"paidAt\",\"scope\":\"global\"}"));

        assertThat(payment.update()).isNull();
        assertThat(payment.create()).isEmpty();
        assertThat(payment.ops()).containsExactlyInAnyOrder("Get", "List");
        assertThat(payment.serves("Create")).isFalse();
        assertThat(payment.standardOnly()).isFalse();
        assertThat(payment.seed()).containsExactly("INSERT 1");
        assertThat(payment.seedId()).isEqualTo("abc");
        assertThat(payment.partition()).isEqualTo("paidAt");
        assertThat(payment.scope()).isEqualTo("global");
        GrpcParityContract.Resource whole = new GrpcParityContract.Resource("Country", "/v1/master/countries",
                JSON.createObjectNode(), JSON.createObjectNode(), true);
        assertThat(whole.standardOnly()).isTrue();
        assertThat(whole.seedId()).isNull();
        assertThat(whole.partition()).isNull();
    }

    @Test
    void aPathSegmentNamesTheParentItsIdFieldRefersTo() {
        assertThat(GrpcParityContract.names("audience-groups", "audience_group")).isTrue();
        assertThat(GrpcParityContract.names("class-sections", "class_section")).isTrue();
        assertThat(GrpcParityContract.names("batches", "batch")).isTrue();
        assertThat(GrpcParityContract.names("categories", "category")).isTrue();
        assertThat(GrpcParityContract.names("media", "media")).isTrue();
        assertThat(GrpcParityContract.names("staff", "staff")).isTrue();
        assertThat(GrpcParityContract.names("programming-exercises", "exercise")).isTrue();
        assertThat(GrpcParityContract.names("users", "user_account")).isTrue();
        assertThat(GrpcParityContract.names("institution-types", "institution")).as("another parent").isFalse();
        assertThat(GrpcParityContract.names("courses", "cohort")).isFalse();
    }

    // ------------------------------------------------------------------ cursors

    private static String cursor(long second, String lastId) {
        SortSpec sort = SortSpec.parse(null, Map.of("name", new FieldSpec("name", "name", ValueType.STRING)), "name",
                "id");
        return PageCursor.encode(2, sort, "", List.of("Ann"), UUID.fromString(lastId),
                Clock.fixed(Instant.ofEpochSecond(second), ZoneOffset.UTC));
    }

    @Test
    void cursorsIssuedInDifferentSecondsForTheSamePositionAreTheSame() {
        String id = "0196f000-0000-7000-8000-000000000001";

        GrpcParity.assertSameCursor("/v1/x?size=1&cursor=" + cursor(1_000, id) + "&page=2", cursor(1_007, id));
        assertThatThrownBy(() -> GrpcParity.assertSameCursor("/v1/x?size=1&cursor=" + cursor(1_000, id),
                cursor(1_000, "0196f000-0000-7000-8000-000000000002"))).isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> GrpcParity.assertSameCursor("/v1/x?size=1", cursor(1_000, id)))
                .isInstanceOf(AssertionError.class);
    }

    // ------------------------------------------------------------------ values

    @Test
    void valuesAreComparedAcrossTheirRenderings() {
        GrpcParity.assertSameResource(json("{\"size\":\"0\",\"count\":3,\"bytes\":\"12\",\"result\":{\"rows\":2},"
                        + "\"lines\":[{\"amount\":\"1.50\",\"status\":\"open\"}],\"active\":false}"),
                json("{\"count\":\"3\",\"bytes\":\"12\",\"result\":{\"rows\":2.0},"
                        + "\"lines\":[{\"amount\":\"1.50\",\"status\":\"STATUS_OPEN\"}]}"));

        assertThatThrownBy(() -> GrpcParity.assertSameResource(json("{\"count\":3}"), json("{\"count\":\"4\"}")))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> GrpcParity.assertSameResource(json("{\"marks\":\"3\"}"), json("{\"marks\":3}")))
                .as("a decimal string is not a number").isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> GrpcParity.assertSameResource(json("{\"lines\":[{\"amount\":\"1.50\"}]}"),
                json("{\"lines\":[{\"amount\":\"1.5\",\"extra\":1}]}"))).isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> GrpcParity.assertSameResource(json("{}"), json("{\"extra\":1}")))
                .isInstanceOf(AssertionError.class);
    }
}
