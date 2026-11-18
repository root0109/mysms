package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.SalesEnquiry;
import com.mysms.platform.api.model.SalesEnquiryUpdate;
import com.mysms.platform.business.SalesEnquiryService;
import com.mysms.platform.v1.CreateSalesEnquiryRequest;
import com.mysms.platform.v1.CreateSalesEnquiryResponse;
import com.mysms.platform.v1.DeleteSalesEnquiryRequest;
import com.mysms.platform.v1.DeleteSalesEnquiryResponse;
import com.mysms.platform.v1.GetSalesEnquiryRequest;
import com.mysms.platform.v1.GetSalesEnquiryResponse;
import com.mysms.platform.v1.ListSalesEnquiriesRequest;
import com.mysms.platform.v1.ListSalesEnquiriesResponse;
import com.mysms.platform.v1.MarkEnquiryContactedRequest;
import com.mysms.platform.v1.MarkEnquiryContactedResponse;
import com.mysms.platform.v1.SalesEnquiryServiceGrpc;
import com.mysms.platform.v1.UpdateSalesEnquiryRequest;
import com.mysms.platform.v1.UpdateSalesEnquiryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.SalesEnquiryService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SalesEnquiryResolver extends SalesEnquiryServiceGrpc.SalesEnquiryServiceImplBase {

    private final SalesEnquiryService service;
    private final ProtoBridge bridge;

    public SalesEnquiryResolver(SalesEnquiryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSalesEnquiries(ListSalesEnquiriesRequest request, StreamObserver<ListSalesEnquiriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SalesEnquiry> rows = service.listSalesEnquirys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSalesEnquiriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSalesEnquiry(GetSalesEnquiryRequest request, StreamObserver<GetSalesEnquiryResponse> out) {
        SalesEnquiry found = service.getSalesEnquiry(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSalesEnquiryResponse.newBuilder(), "sales_enquiry", found).build());
    }

    @Override
    public void createSalesEnquiry(CreateSalesEnquiryRequest request, StreamObserver<CreateSalesEnquiryResponse> out) {
        SalesEnquiry created = service.createSalesEnquiry(
                bridge.body(request.getSalesEnquiry(), SalesEnquiry.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSalesEnquiryResponse.newBuilder(), "sales_enquiry", created).build());
    }

    @Override
    public void updateSalesEnquiry(UpdateSalesEnquiryRequest request, StreamObserver<UpdateSalesEnquiryResponse> out) {
        SalesEnquiry updated = service.updateSalesEnquiry(
                bridge.id(request.getSalesEnquiry().getId(), "id"),
                bridge.requiredIfMatch(request.getSalesEnquiry().getEtag()),
                bridge.changes(request.getSalesEnquiry(), request.getUpdateMask(), SalesEnquiryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSalesEnquiryResponse.newBuilder(), "sales_enquiry", updated).build());
    }

    @Override
    public void deleteSalesEnquiry(DeleteSalesEnquiryRequest request, StreamObserver<DeleteSalesEnquiryResponse> out) {
        service.deleteSalesEnquiry(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSalesEnquiryResponse.getDefaultInstance());
    }

    @Override
    public void markEnquiryContacted(MarkEnquiryContactedRequest request,
            StreamObserver<MarkEnquiryContactedResponse> out) {
        SalesEnquiry marked = service.markEnquiryContacted(bridge.id(request.getSalesEnquiryId(), "salesEnquiryId"),
                bridge.body(request, com.mysms.platform.api.model.MarkEnquiryContactedRequest.class,
                        "sales_enquiry_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkEnquiryContactedResponse.newBuilder(), "sales_enquiry", marked).build());
    }
}
