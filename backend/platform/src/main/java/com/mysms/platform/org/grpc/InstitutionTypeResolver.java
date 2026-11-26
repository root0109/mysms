package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.org.api.model.InstitutionType;
import com.mysms.platform.org.api.model.InstitutionTypeAttribute;
import com.mysms.platform.org.api.model.InstitutionTypeAttributeUpdate;
import com.mysms.platform.org.api.model.InstitutionTypeUpdate;
import com.mysms.platform.org.business.InstitutionTypeService;
import com.mysms.org.v1.CreateInstitutionTypeAttributeRequest;
import com.mysms.org.v1.CreateInstitutionTypeAttributeResponse;
import com.mysms.org.v1.CreateInstitutionTypeRequest;
import com.mysms.org.v1.CreateInstitutionTypeResponse;
import com.mysms.org.v1.DeleteInstitutionTypeAttributeRequest;
import com.mysms.org.v1.DeleteInstitutionTypeAttributeResponse;
import com.mysms.org.v1.DeleteInstitutionTypeRequest;
import com.mysms.org.v1.DeleteInstitutionTypeResponse;
import com.mysms.org.v1.GetInstitutionTypeAttributeRequest;
import com.mysms.org.v1.GetInstitutionTypeAttributeResponse;
import com.mysms.org.v1.GetInstitutionTypeRequest;
import com.mysms.org.v1.GetInstitutionTypeResponse;
import com.mysms.org.v1.InstitutionTypeServiceGrpc;
import com.mysms.org.v1.ListInstitutionTypeAttributesRequest;
import com.mysms.org.v1.ListInstitutionTypeAttributesResponse;
import com.mysms.org.v1.ListInstitutionTypesRequest;
import com.mysms.org.v1.ListInstitutionTypesResponse;
import com.mysms.org.v1.UpdateInstitutionTypeAttributeRequest;
import com.mysms.org.v1.UpdateInstitutionTypeAttributeResponse;
import com.mysms.org.v1.UpdateInstitutionTypeRequest;
import com.mysms.org.v1.UpdateInstitutionTypeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of org.InstitutionTypeService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class InstitutionTypeResolver extends InstitutionTypeServiceGrpc.InstitutionTypeServiceImplBase {

    private final InstitutionTypeService service;
    private final ProtoBridge bridge;

    public InstitutionTypeResolver(InstitutionTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listInstitutionTypes(ListInstitutionTypesRequest request,
            StreamObserver<ListInstitutionTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<InstitutionType> rows = service.listInstitutionTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListInstitutionTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getInstitutionType(GetInstitutionTypeRequest request, StreamObserver<GetInstitutionTypeResponse> out) {
        InstitutionType found = service.getInstitutionType(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetInstitutionTypeResponse.newBuilder(), "institution_type", found).build());
    }

    @Override
    public void createInstitutionType(CreateInstitutionTypeRequest request,
            StreamObserver<CreateInstitutionTypeResponse> out) {
        InstitutionType created = service.createInstitutionType(
                bridge.body(request.getInstitutionType(), InstitutionType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateInstitutionTypeResponse.newBuilder(), "institution_type",
                created).build());
    }

    @Override
    public void updateInstitutionType(UpdateInstitutionTypeRequest request,
            StreamObserver<UpdateInstitutionTypeResponse> out) {
        InstitutionType updated = service.updateInstitutionType(
                bridge.id(request.getInstitutionType().getId(), "id"),
                bridge.requiredIfMatch(request.getInstitutionType().getEtag()),
                bridge.changes(request.getInstitutionType(), request.getUpdateMask(), InstitutionTypeUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateInstitutionTypeResponse.newBuilder(), "institution_type",
                updated).build());
    }

    @Override
    public void deleteInstitutionType(DeleteInstitutionTypeRequest request,
            StreamObserver<DeleteInstitutionTypeResponse> out) {
        service.deleteInstitutionType(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteInstitutionTypeResponse.getDefaultInstance());
    }

    @Override
    public void listInstitutionTypeAttributes(ListInstitutionTypeAttributesRequest request,
            StreamObserver<ListInstitutionTypeAttributesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<InstitutionTypeAttribute> rows = service.listInstitutionTypeAttributes(
                bridge.id(request.getInstitutionTypeId(), "institutionTypeId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListInstitutionTypeAttributesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getInstitutionTypeAttribute(GetInstitutionTypeAttributeRequest request,
            StreamObserver<GetInstitutionTypeAttributeResponse> out) {
        InstitutionTypeAttribute found = service.getInstitutionTypeAttribute(
                bridge.id(request.getInstitutionTypeId(), "institutionTypeId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetInstitutionTypeAttributeResponse.newBuilder(),
                "institution_type_attribute", found).build());
    }

    @Override
    public void createInstitutionTypeAttribute(CreateInstitutionTypeAttributeRequest request,
            StreamObserver<CreateInstitutionTypeAttributeResponse> out) {
        InstitutionTypeAttribute created = service.createInstitutionTypeAttribute(
                bridge.id(request.getInstitutionTypeId(), "institutionTypeId"),
                bridge.body(request.getInstitutionTypeAttribute(), InstitutionTypeAttribute.class),
                        request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateInstitutionTypeAttributeResponse.newBuilder(),
                "institution_type_attribute", created).build());
    }

    @Override
    public void updateInstitutionTypeAttribute(UpdateInstitutionTypeAttributeRequest request,
            StreamObserver<UpdateInstitutionTypeAttributeResponse> out) {
        InstitutionTypeAttribute updated = service.updateInstitutionTypeAttribute(
                bridge.id(request.getInstitutionTypeId(), "institutionTypeId"),
                bridge.id(request.getInstitutionTypeAttribute().getId(), "id"),
                bridge.requiredIfMatch(request.getInstitutionTypeAttribute().getEtag()),
                bridge.changes(request.getInstitutionTypeAttribute(), request.getUpdateMask(),
                        InstitutionTypeAttributeUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateInstitutionTypeAttributeResponse.newBuilder(),
                "institution_type_attribute", updated).build());
    }

    @Override
    public void deleteInstitutionTypeAttribute(DeleteInstitutionTypeAttributeRequest request,
            StreamObserver<DeleteInstitutionTypeAttributeResponse> out) {
        service.deleteInstitutionTypeAttribute(
                bridge.id(request.getInstitutionTypeId(), "institutionTypeId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteInstitutionTypeAttributeResponse.getDefaultInstance());
    }
}
