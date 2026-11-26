package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.org.api.model.AcademicTerm;
import com.mysms.platform.org.api.model.AcademicTermList;
import com.mysms.platform.org.api.model.AcademicTermUpdate;
import com.mysms.platform.org.api.model.AcademicYear;
import com.mysms.platform.org.api.model.AcademicYearUpdate;
import com.mysms.platform.org.business.AcademicYearService;
import com.mysms.org.v1.AcademicYearServiceGrpc;
import com.mysms.org.v1.ActivateAcademicYearRequest;
import com.mysms.org.v1.ActivateAcademicYearResponse;
import com.mysms.org.v1.CloseAcademicYearRequest;
import com.mysms.org.v1.CloseAcademicYearResponse;
import com.mysms.org.v1.CreateAcademicTermRequest;
import com.mysms.org.v1.CreateAcademicTermResponse;
import com.mysms.org.v1.CreateAcademicYearRequest;
import com.mysms.org.v1.CreateAcademicYearResponse;
import com.mysms.org.v1.DeleteAcademicTermRequest;
import com.mysms.org.v1.DeleteAcademicTermResponse;
import com.mysms.org.v1.DeleteAcademicYearRequest;
import com.mysms.org.v1.DeleteAcademicYearResponse;
import com.mysms.org.v1.GenerateTermsRequest;
import com.mysms.org.v1.GenerateTermsResponse;
import com.mysms.org.v1.GetAcademicTermRequest;
import com.mysms.org.v1.GetAcademicTermResponse;
import com.mysms.org.v1.GetAcademicYearRequest;
import com.mysms.org.v1.GetAcademicYearResponse;
import com.mysms.org.v1.ListAcademicTermsRequest;
import com.mysms.org.v1.ListAcademicTermsResponse;
import com.mysms.org.v1.ListAcademicYearsRequest;
import com.mysms.org.v1.ListAcademicYearsResponse;
import com.mysms.org.v1.UpdateAcademicTermRequest;
import com.mysms.org.v1.UpdateAcademicTermResponse;
import com.mysms.org.v1.UpdateAcademicYearRequest;
import com.mysms.org.v1.UpdateAcademicYearResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of org.AcademicYearService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AcademicYearResolver extends AcademicYearServiceGrpc.AcademicYearServiceImplBase {

    private final AcademicYearService service;
    private final ProtoBridge bridge;

    public AcademicYearResolver(AcademicYearService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listAcademicYears(ListAcademicYearsRequest request,
            StreamObserver<ListAcademicYearsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AcademicYear> rows = service.listAcademicYears(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAcademicYearsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAcademicYear(GetAcademicYearRequest request, StreamObserver<GetAcademicYearResponse> out) {
        AcademicYear found = service.getAcademicYear(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAcademicYearResponse.newBuilder(), "academic_year", found).build());
    }

    @Override
    public void createAcademicYear(CreateAcademicYearRequest request, StreamObserver<CreateAcademicYearResponse> out) {
        AcademicYear created = service.createAcademicYear(
                bridge.body(request.getAcademicYear(), AcademicYear.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAcademicYearResponse.newBuilder(), "academic_year", created).build());
    }

    @Override
    public void updateAcademicYear(UpdateAcademicYearRequest request, StreamObserver<UpdateAcademicYearResponse> out) {
        AcademicYear updated = service.updateAcademicYear(
                bridge.id(request.getAcademicYear().getId(), "id"),
                bridge.requiredIfMatch(request.getAcademicYear().getEtag()),
                bridge.changes(request.getAcademicYear(), request.getUpdateMask(), AcademicYearUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAcademicYearResponse.newBuilder(), "academic_year", updated).build());
    }

    @Override
    public void deleteAcademicYear(DeleteAcademicYearRequest request, StreamObserver<DeleteAcademicYearResponse> out) {
        service.deleteAcademicYear(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAcademicYearResponse.getDefaultInstance());
    }

    @Override
    public void listAcademicTerms(ListAcademicTermsRequest request,
            StreamObserver<ListAcademicTermsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AcademicTerm> rows = service.listAcademicTerms(
                bridge.id(request.getAcademicYearId(), "academicYearId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAcademicTermsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAcademicTerm(GetAcademicTermRequest request, StreamObserver<GetAcademicTermResponse> out) {
        AcademicTerm found = service.getAcademicTerm(
                bridge.id(request.getAcademicYearId(), "academicYearId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAcademicTermResponse.newBuilder(), "academic_term", found).build());
    }

    @Override
    public void createAcademicTerm(CreateAcademicTermRequest request, StreamObserver<CreateAcademicTermResponse> out) {
        AcademicTerm created = service.createAcademicTerm(
                bridge.id(request.getAcademicYearId(), "academicYearId"),
                bridge.body(request.getAcademicTerm(), AcademicTerm.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAcademicTermResponse.newBuilder(), "academic_term", created).build());
    }

    @Override
    public void updateAcademicTerm(UpdateAcademicTermRequest request, StreamObserver<UpdateAcademicTermResponse> out) {
        AcademicTerm updated = service.updateAcademicTerm(
                bridge.id(request.getAcademicYearId(), "academicYearId"),
                bridge.id(request.getAcademicTerm().getId(), "id"),
                bridge.requiredIfMatch(request.getAcademicTerm().getEtag()),
                bridge.changes(request.getAcademicTerm(), request.getUpdateMask(), AcademicTermUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAcademicTermResponse.newBuilder(), "academic_term", updated).build());
    }

    @Override
    public void deleteAcademicTerm(DeleteAcademicTermRequest request, StreamObserver<DeleteAcademicTermResponse> out) {
        service.deleteAcademicTerm(
                bridge.id(request.getAcademicYearId(), "academicYearId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAcademicTermResponse.getDefaultInstance());
    }

    @Override
    public void activateAcademicYear(ActivateAcademicYearRequest request,
                                     StreamObserver<ActivateAcademicYearResponse> out) {
        AcademicYear activated = service.activateAcademicYear(
                bridge.id(request.getAcademicYearId(), "academicYearId"),
                bridge.body(request, com.mysms.platform.org.api.model.ActivateAcademicYearRequest.class, "academic_year_id",
                        "request_id"));
        ProtoBridge.send(out,
                bridge.reply(ActivateAcademicYearResponse.newBuilder(), "academic_year", activated).build());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void closeAcademicYear(CloseAcademicYearRequest request, StreamObserver<CloseAcademicYearResponse> out) {
        AcademicYear closed = service.closeAcademicYear(bridge.id(request.getAcademicYearId(), "academicYearId"),
                bridge.body(request, com.mysms.platform.org.api.model.CloseAcademicYearRequest.class, "academic_year_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CloseAcademicYearResponse.newBuilder(), "academic_year", closed).build());
    }

    @Override
    public void generateTerms(GenerateTermsRequest request, StreamObserver<GenerateTermsResponse> out) {
        AcademicTermList generated = service.generateTerms(bridge.id(request.getAcademicYearId(), "academicYearId"));
        ProtoBridge.send(out, bridge.reply(GenerateTermsResponse.newBuilder(), "items", generated.getItems()).build());
    }
}
