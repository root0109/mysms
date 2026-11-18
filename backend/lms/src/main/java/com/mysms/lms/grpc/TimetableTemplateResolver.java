package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.TimetableTemplate;
import com.mysms.lms.api.model.TimetableTemplateUpdate;
import com.mysms.lms.business.TimetableTemplateService;
import com.mysms.lms.v1.CreateTimetableTemplateRequest;
import com.mysms.lms.v1.CreateTimetableTemplateResponse;
import com.mysms.lms.v1.DeleteTimetableTemplateRequest;
import com.mysms.lms.v1.DeleteTimetableTemplateResponse;
import com.mysms.lms.v1.GetTimetableTemplateRequest;
import com.mysms.lms.v1.GetTimetableTemplateResponse;
import com.mysms.lms.v1.ListTimetableTemplatesRequest;
import com.mysms.lms.v1.ListTimetableTemplatesResponse;
import com.mysms.lms.v1.TimetableTemplateServiceGrpc;
import com.mysms.lms.v1.UpdateTimetableTemplateRequest;
import com.mysms.lms.v1.UpdateTimetableTemplateResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.TimetableTemplateService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class TimetableTemplateResolver extends TimetableTemplateServiceGrpc.TimetableTemplateServiceImplBase {

    private final TimetableTemplateService service;
    private final ProtoBridge bridge;

    public TimetableTemplateResolver(TimetableTemplateService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTimetableTemplates(ListTimetableTemplatesRequest request,
            StreamObserver<ListTimetableTemplatesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TimetableTemplate> rows = service.listTimetableTemplates(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTimetableTemplatesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTimetableTemplate(GetTimetableTemplateRequest request, StreamObserver<GetTimetableTemplateResponse> out) {
        TimetableTemplate found = service.getTimetableTemplate(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTimetableTemplateResponse.newBuilder(), "timetable_template", found).build());
    }

    @Override
    public void createTimetableTemplate(CreateTimetableTemplateRequest request,
            StreamObserver<CreateTimetableTemplateResponse> out) {
        TimetableTemplate created = service.createTimetableTemplate(
                bridge.body(request.getTimetableTemplate(), TimetableTemplate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTimetableTemplateResponse.newBuilder(), "timetable_template", created).build());
    }

    @Override
    public void updateTimetableTemplate(UpdateTimetableTemplateRequest request,
            StreamObserver<UpdateTimetableTemplateResponse> out) {
        TimetableTemplate updated = service.updateTimetableTemplate(bridge.id(request.getTimetableTemplate().getId(), "id"),
                bridge.requiredIfMatch(request.getTimetableTemplate().getEtag()),
                bridge.changes(request.getTimetableTemplate(), request.getUpdateMask(), TimetableTemplateUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateTimetableTemplateResponse.newBuilder(), "timetable_template", updated).build());
    }

    @Override
    public void deleteTimetableTemplate(DeleteTimetableTemplateRequest request,
            StreamObserver<DeleteTimetableTemplateResponse> out) {
        service.deleteTimetableTemplate(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTimetableTemplateResponse.getDefaultInstance());
    }
}
