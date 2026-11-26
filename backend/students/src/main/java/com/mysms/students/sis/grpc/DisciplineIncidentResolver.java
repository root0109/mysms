package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.students.sis.api.model.DisciplineIncident;
import com.mysms.students.sis.api.model.DisciplineIncidentUpdate;
import com.mysms.students.sis.business.DisciplineIncidentService;
import com.mysms.sis.v1.CreateDisciplineIncidentRequest;
import com.mysms.sis.v1.CreateDisciplineIncidentResponse;
import com.mysms.sis.v1.DeleteDisciplineIncidentRequest;
import com.mysms.sis.v1.DeleteDisciplineIncidentResponse;
import com.mysms.sis.v1.DisciplineIncidentServiceGrpc;
import com.mysms.sis.v1.GetDisciplineIncidentRequest;
import com.mysms.sis.v1.GetDisciplineIncidentResponse;
import com.mysms.sis.v1.ListDisciplineIncidentsRequest;
import com.mysms.sis.v1.ListDisciplineIncidentsResponse;
import com.mysms.sis.v1.MarkIncidentActionTakenRequest;
import com.mysms.sis.v1.MarkIncidentActionTakenResponse;
import com.mysms.sis.v1.MarkIncidentSeenRequest;
import com.mysms.sis.v1.MarkIncidentSeenResponse;
import com.mysms.sis.v1.UpdateDisciplineIncidentRequest;
import com.mysms.sis.v1.UpdateDisciplineIncidentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.DisciplineIncidentService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DisciplineIncidentResolver extends DisciplineIncidentServiceGrpc.DisciplineIncidentServiceImplBase {

    private final DisciplineIncidentService service;
    private final ProtoBridge bridge;

    public DisciplineIncidentResolver(DisciplineIncidentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listDisciplineIncidents(ListDisciplineIncidentsRequest request,
            StreamObserver<ListDisciplineIncidentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<DisciplineIncident> rows = service.listDisciplineIncidents(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDisciplineIncidentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDisciplineIncident(GetDisciplineIncidentRequest request, StreamObserver<GetDisciplineIncidentResponse> out) {
        DisciplineIncident found = service.getDisciplineIncident(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetDisciplineIncidentResponse.newBuilder(), "discipline_incident", found).build());
    }

    @Override
    public void createDisciplineIncident(CreateDisciplineIncidentRequest request,
            StreamObserver<CreateDisciplineIncidentResponse> out) {
        DisciplineIncident created = service.createDisciplineIncident(
                bridge.body(request.getDisciplineIncident(), DisciplineIncident.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateDisciplineIncidentResponse.newBuilder(), "discipline_incident", created).build());
    }

    @Override
    public void updateDisciplineIncident(UpdateDisciplineIncidentRequest request,
            StreamObserver<UpdateDisciplineIncidentResponse> out) {
        DisciplineIncident updated = service.updateDisciplineIncident(
                bridge.id(request.getDisciplineIncident().getId(), "id"),
                bridge.requiredIfMatch(request.getDisciplineIncident().getEtag()),
                bridge.changes(request.getDisciplineIncident(), request.getUpdateMask(), DisciplineIncidentUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateDisciplineIncidentResponse.newBuilder(), "discipline_incident", updated).build());
    }

    @Override
    public void deleteDisciplineIncident(DeleteDisciplineIncidentRequest request,
            StreamObserver<DeleteDisciplineIncidentResponse> out) {
        service.deleteDisciplineIncident(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDisciplineIncidentResponse.getDefaultInstance());
    }

    @Override
    public void markIncidentActionTaken(MarkIncidentActionTakenRequest request,
            StreamObserver<MarkIncidentActionTakenResponse> out) {
        DisciplineIncident changed = service.markIncidentActionTaken(
                bridge.id(request.getDisciplineIncidentId(), "disciplineIncidentId"),
                bridge.body(request, com.mysms.students.sis.api.model.MarkIncidentActionTakenRequest.class,
                        "discipline_incident_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(
                MarkIncidentActionTakenResponse.newBuilder(), "discipline_incident", changed).build());
    }

    @Override
    public void markIncidentSeen(MarkIncidentSeenRequest request, StreamObserver<MarkIncidentSeenResponse> out) {
        DisciplineIncident changed = service.markIncidentSeen(
                bridge.id(request.getDisciplineIncidentId(), "disciplineIncidentId"),
                bridge.body(request, com.mysms.students.sis.api.model.MarkIncidentSeenRequest.class, "discipline_incident_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkIncidentSeenResponse.newBuilder(), "discipline_incident", changed).build());
    }
}
