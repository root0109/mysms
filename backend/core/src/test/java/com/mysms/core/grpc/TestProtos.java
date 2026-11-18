package com.mysms.core.grpc;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.FieldMaskProto;
import com.google.protobuf.TimestampProto;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;

/**
 * A small contract ({@code mysms.coretest.v1}) built in code, so core's tests need no generated stubs: a resource with
 * every kind of field the bridge converts, paging, and a list, a get and a keyed create.
 */
final class TestProtos {

    static final FileDescriptor FILE = build();
    static final Descriptor WIDGET = FILE.findMessageTypeByName("Widget");
    static final Descriptor SORT = FILE.findMessageTypeByName("Sort");
    static final Descriptor PAGEABLE = FILE.findMessageTypeByName("PageableRequest");
    static final Descriptor LIST_RESPONSE = FILE.findMessageTypeByName("ListWidgetsResponse");
    static final Descriptor GET_RESPONSE = FILE.findMessageTypeByName("GetWidgetResponse");
    static final Descriptor CREATE_REQUEST = FILE.findMessageTypeByName("CreateWidgetRequest");

    private TestProtos() {
    }

    /** A unary method of the test service whose messages are dynamic. */
    static MethodDescriptor<DynamicMessage, DynamicMessage> method(String name, Descriptor request, Descriptor response) {
        return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName("mysms.coretest.v1.WidgetService", name))
                .setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(request)))
                .setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(response)))
                .build();
    }

    private static FileDescriptor build() {
        EnumDescriptorProto status = EnumDescriptorProto.newBuilder().setName("Status")
                .addValue(value("STATUS_UNSPECIFIED", 0)).addValue(value("STATUS_ACTIVE", 1))
                .addValue(value("STATUS_ON_HOLD", 2)).build();
        EnumDescriptorProto blood = EnumDescriptorProto.newBuilder().setName("BloodGroup")
                .addValue(value("BLOOD_GROUP_UNSPECIFIED", 0)).addValue(value("BLOOD_GROUP_A_POSITIVE", 1)).build();
        DescriptorProto inner = DescriptorProto.newBuilder().setName("Inner")
                .addField(field("status", 1, Type.TYPE_ENUM, ".mysms.coretest.v1.Widget.Status"))
                .addField(field("note", 2, Type.TYPE_STRING, null)).build();
        DescriptorProto entry = DescriptorProto.newBuilder().setName("ByKeyEntry")
                .setOptions(MessageOptions.newBuilder().setMapEntry(true))
                .addField(field("key", 1, Type.TYPE_STRING, null))
                .addField(field("value", 2, Type.TYPE_ENUM, ".mysms.coretest.v1.Widget.Status")).build();
        DescriptorProto widget = DescriptorProto.newBuilder().setName("Widget")
                .addEnumType(status).addEnumType(blood).addNestedType(inner).addNestedType(entry)
                .addField(field("id", 1, Type.TYPE_STRING, null))
                .addField(optional("name", 2, Type.TYPE_STRING, 0))
                .addField(field("is_active", 3, Type.TYPE_BOOL, null))
                .addField(field("status", 4, Type.TYPE_ENUM, ".mysms.coretest.v1.Widget.Status"))
                .addField(field("history", 5, Type.TYPE_ENUM, ".mysms.coretest.v1.Widget.Status")
                        .toBuilder().setLabel(Label.LABEL_REPEATED).build())
                .addField(field("create_time", 6, Type.TYPE_MESSAGE, ".google.protobuf.Timestamp"))
                .addField(field("sort_order", 7, Type.TYPE_INT32, null))
                .addField(field("inner", 8, Type.TYPE_MESSAGE, ".mysms.coretest.v1.Widget.Inner"))
                .addField(field("by_key", 9, Type.TYPE_MESSAGE, ".mysms.coretest.v1.Widget.ByKeyEntry")
                        .toBuilder().setLabel(Label.LABEL_REPEATED).build())
                .addField(field("blood_group", 10, Type.TYPE_ENUM, ".mysms.coretest.v1.Widget.BloodGroup"))
                .addField(field("etag", 11, Type.TYPE_STRING, null))
                .addField(field("start_date", 12, Type.TYPE_STRING, null))
                .addOneofDecl(OneofDescriptorProto.newBuilder().setName("_name"))
                .build();
        DescriptorProto sort = DescriptorProto.newBuilder().setName("Sort")
                .addEnumType(EnumDescriptorProto.newBuilder().setName("Direction")
                        .addValue(value("DIRECTION_UNSPECIFIED", 0)).addValue(value("DIRECTION_ASC", 1))
                        .addValue(value("DIRECTION_DESC", 2)))
                .addField(field("field", 1, Type.TYPE_STRING, null))
                .addField(field("direction", 2, Type.TYPE_ENUM, ".mysms.coretest.v1.Sort.Direction")).build();
        DescriptorProto pageable = DescriptorProto.newBuilder().setName("PageableRequest")
                .addField(field("page", 1, Type.TYPE_INT32, null))
                .addField(field("size", 2, Type.TYPE_INT32, null))
                .addField(field("sort", 3, Type.TYPE_MESSAGE, ".mysms.coretest.v1.Sort").toBuilder()
                        .setLabel(Label.LABEL_REPEATED).build())
                .addField(field("cursor", 4, Type.TYPE_STRING, null)).build();
        DescriptorProto pagination = DescriptorProto.newBuilder().setName("Pagination")
                .addField(field("size", 1, Type.TYPE_INT32, null))
                .addField(field("first", 2, Type.TYPE_INT32, null))
                .addField(field("previous", 3, Type.TYPE_INT32, null))
                .addField(field("next", 4, Type.TYPE_INT32, null))
                .addField(field("page", 5, Type.TYPE_INT32, null))
                .addField(field("next_cursor", 6, Type.TYPE_STRING, null)).build();
        DescriptorProto list = DescriptorProto.newBuilder().setName("ListWidgetsResponse")
                .addField(field("items", 1, Type.TYPE_MESSAGE, ".mysms.coretest.v1.Widget").toBuilder()
                        .setLabel(Label.LABEL_REPEATED).build())
                .addField(field("pagination", 4, Type.TYPE_MESSAGE, ".mysms.coretest.v1.Pagination")).build();
        DescriptorProto get = DescriptorProto.newBuilder().setName("GetWidgetResponse")
                .addField(field("widget", 1, Type.TYPE_MESSAGE, ".mysms.coretest.v1.Widget")).build();
        DescriptorProto create = DescriptorProto.newBuilder().setName("CreateWidgetRequest")
                .addField(field("widget", 1, Type.TYPE_MESSAGE, ".mysms.coretest.v1.Widget"))
                .addField(field("request_id", 2, Type.TYPE_STRING, null))
                .addField(field("validate_only", 3, Type.TYPE_BOOL, null)).build();
        FileDescriptorProto file = FileDescriptorProto.newBuilder().setName("mysms/coretest/v1/coretest.proto")
                .setPackage("mysms.coretest.v1").setSyntax("proto3")
                .addDependency("google/protobuf/timestamp.proto").addDependency("google/protobuf/field_mask.proto")
                .addMessageType(widget).addMessageType(sort).addMessageType(pageable).addMessageType(pagination)
                .addMessageType(list).addMessageType(get).addMessageType(create).build();
        try {
            return FileDescriptor.buildFrom(file, new FileDescriptor[] {TimestampProto.getDescriptor(),
                    FieldMaskProto.getDescriptor()});
        } catch (DescriptorValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static EnumValueDescriptorProto value(String name, int number) {
        return EnumValueDescriptorProto.newBuilder().setName(name).setNumber(number).build();
    }

    private static FieldDescriptorProto field(String name, int number, Type type, String typeName) {
        FieldDescriptorProto.Builder field = FieldDescriptorProto.newBuilder().setName(name).setNumber(number)
                .setType(type).setLabel(Label.LABEL_OPTIONAL).setJsonName(jsonName(name));
        if (typeName != null) {
            field.setTypeName(typeName);
        }
        return field.build();
    }

    /** A proto3 {@code optional} field (presence through its synthetic oneof). */
    private static FieldDescriptorProto optional(String name, int number, Type type, int oneof) {
        return field(name, number, type, null).toBuilder().setProto3Optional(true).setOneofIndex(oneof).build();
    }

    private static String jsonName(String name) {
        StringBuilder json = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                json.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return json.toString();
    }
}
