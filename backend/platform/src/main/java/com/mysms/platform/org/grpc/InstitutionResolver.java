package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.org.api.model.Institution;
import com.mysms.platform.org.api.model.InstitutionCard;
import com.mysms.platform.org.api.model.InstitutionAttribute;
import com.mysms.platform.org.api.model.InstitutionAttributeUpdate;
import com.mysms.platform.org.api.model.InstitutionUpdate;
import com.mysms.platform.org.business.InstitutionService;
import com.mysms.platform.org.business.SchoolFinder;
import com.mysms.org.v1.CreateInstitutionAttributeRequest;
import com.mysms.org.v1.CreateInstitutionAttributeResponse;
import com.mysms.org.v1.CreateInstitutionRequest;
import com.mysms.org.v1.CreateInstitutionResponse;
import com.mysms.org.v1.DeleteInstitutionAttributeRequest;
import com.mysms.org.v1.DeleteInstitutionAttributeResponse;
import com.mysms.org.v1.DeleteInstitutionRequest;
import com.mysms.org.v1.DeleteInstitutionResponse;
import com.mysms.org.v1.FindInstitutionsRequest;
import com.mysms.org.v1.FindInstitutionsResponse;
import com.mysms.org.v1.GetInstitutionAttributeRequest;
import com.mysms.org.v1.GetInstitutionAttributeResponse;
import com.mysms.org.v1.GetInstitutionRequest;
import com.mysms.org.v1.GetInstitutionResponse;
import com.mysms.org.v1.InstitutionServiceGrpc;
import com.mysms.org.v1.ListActiveInstitutionsRequest;
import com.mysms.org.v1.ListActiveInstitutionsResponse;
import com.mysms.org.v1.ListInstitutionAttributesRequest;
import com.mysms.org.v1.ListInstitutionAttributesResponse;
import com.mysms.org.v1.ListInstitutionsRequest;
import com.mysms.org.v1.ListInstitutionsResponse;
import com.mysms.org.v1.UpdateInstitutionAttributeRequest;
import com.mysms.org.v1.UpdateInstitutionAttributeResponse;
import com.mysms.org.v1.UpdateInstitutionRequest;
import com.mysms.org.v1.UpdateInstitutionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of org.InstitutionService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class InstitutionResolver extends InstitutionServiceGrpc.InstitutionServiceImplBase {

    private final InstitutionService service;
    private final SchoolFinder schoolFinder;
    private final ProtoBridge bridge;

    public InstitutionResolver(InstitutionService service, SchoolFinder schoolFinder, ProtoBridge bridge) {
        this.service = service;
        this.schoolFinder = schoolFinder;
        this.bridge = bridge;
    }


    @Override
    public void listInstitutions(ListInstitutionsRequest request,
            StreamObserver<ListInstitutionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Institution> rows = service.listInstitutions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListInstitutionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void listActiveInstitutions(ListActiveInstitutionsRequest request,
            StreamObserver<ListActiveInstitutionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Institution> rows = service.listActiveInstitutions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()));
        ProtoBridge.send(out, bridge.page(ListActiveInstitutionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getInstitution(GetInstitutionRequest request, StreamObserver<GetInstitutionResponse> out) {
        Institution found = service.getInstitution(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetInstitutionResponse.newBuilder(), "institution", found).build());
    }

    @Override
    public void createInstitution(CreateInstitutionRequest request, StreamObserver<CreateInstitutionResponse> out) {
        Institution created = service.createInstitution(
                bridge.body(request.getInstitution(), Institution.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateInstitutionResponse.newBuilder(), "institution", created).build());
    }

    @Override
    public void updateInstitution(UpdateInstitutionRequest request, StreamObserver<UpdateInstitutionResponse> out) {
        Institution updated = service.updateInstitution(
                bridge.id(request.getInstitution().getId(), "id"),
                bridge.requiredIfMatch(request.getInstitution().getEtag()),
                bridge.changes(request.getInstitution(), request.getUpdateMask(), InstitutionUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateInstitutionResponse.newBuilder(), "institution", updated).build());
    }

    @Override
    public void deleteInstitution(DeleteInstitutionRequest request, StreamObserver<DeleteInstitutionResponse> out) {
        service.deleteInstitution(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteInstitutionResponse.getDefaultInstance());
    }

    @Override
    public void listInstitutionAttributes(ListInstitutionAttributesRequest request,
            StreamObserver<ListInstitutionAttributesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<InstitutionAttribute> rows = service.listInstitutionAttributes(
                bridge.id(request.getInstitutionId(), "institutionId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListInstitutionAttributesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getInstitutionAttribute(GetInstitutionAttributeRequest request,
            StreamObserver<GetInstitutionAttributeResponse> out) {
        InstitutionAttribute found = service.getInstitutionAttribute(
                bridge.id(request.getInstitutionId(), "institutionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetInstitutionAttributeResponse.newBuilder(), "institution_attribute",
                found).build());
    }

    @Override
    public void createInstitutionAttribute(CreateInstitutionAttributeRequest request,
            StreamObserver<CreateInstitutionAttributeResponse> out) {
        InstitutionAttribute created = service.createInstitutionAttribute(
                bridge.id(request.getInstitutionId(), "institutionId"),
                bridge.body(request.getInstitutionAttribute(), InstitutionAttribute.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateInstitutionAttributeResponse.newBuilder(), "institution_attribute",
                created).build());
    }

    @Override
    public void updateInstitutionAttribute(UpdateInstitutionAttributeRequest request,
            StreamObserver<UpdateInstitutionAttributeResponse> out) {
        InstitutionAttribute updated = service.updateInstitutionAttribute(
                bridge.id(request.getInstitutionId(), "institutionId"),
                bridge.id(request.getInstitutionAttribute().getId(), "id"),
                bridge.requiredIfMatch(request.getInstitutionAttribute().getEtag()),
                bridge.changes(request.getInstitutionAttribute(), request.getUpdateMask(),
                        InstitutionAttributeUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateInstitutionAttributeResponse.newBuilder(), "institution_attribute",
                updated).build());
    }

    @Override
    public void deleteInstitutionAttribute(DeleteInstitutionAttributeRequest request,
            StreamObserver<DeleteInstitutionAttributeResponse> out) {
        service.deleteInstitutionAttribute(
                bridge.id(request.getInstitutionId(), "institutionId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteInstitutionAttributeResponse.getDefaultInstance());
    }

    @Override
    public void findInstitutions(FindInstitutionsRequest request, StreamObserver<FindInstitutionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<InstitutionCard> found = schoolFinder.findInstitutions(request.getQuery(),
                request.hasCountryCode() ? request.getCountryCode() : null, page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(FindInstitutionsResponse.newBuilder(), found).build());
    }
}
