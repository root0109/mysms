package com.mysms.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.library.api.model.Reservation;
import com.mysms.library.api.model.ReservationUpdate;
import com.mysms.library.business.ReservationService;
import com.mysms.library.v1.AcceptReservationRequest;
import com.mysms.library.v1.AcceptReservationResponse;
import com.mysms.library.v1.CreateReservationRequest;
import com.mysms.library.v1.CreateReservationResponse;
import com.mysms.library.v1.DeleteReservationRequest;
import com.mysms.library.v1.DeleteReservationResponse;
import com.mysms.library.v1.GetReservationRequest;
import com.mysms.library.v1.GetReservationResponse;
import com.mysms.library.v1.ListReservationsRequest;
import com.mysms.library.v1.ListReservationsResponse;
import com.mysms.library.v1.RejectReservationRequest;
import com.mysms.library.v1.RejectReservationResponse;
import com.mysms.library.v1.RequestReservationAgainRequest;
import com.mysms.library.v1.RequestReservationAgainResponse;
import com.mysms.library.v1.ReservationServiceGrpc;
import com.mysms.library.v1.UpdateReservationRequest;
import com.mysms.library.v1.UpdateReservationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.ReservationService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ReservationResolver extends ReservationServiceGrpc.ReservationServiceImplBase {

    private final ReservationService service;
    private final ProtoBridge bridge;

    public ReservationResolver(ReservationService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listReservations(ListReservationsRequest request, StreamObserver<ListReservationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Reservation> rows = service.listReservations(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListReservationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getReservation(GetReservationRequest request, StreamObserver<GetReservationResponse> out) {
        Reservation found = service.getReservation(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetReservationResponse.newBuilder(), "reservation", found).build());
    }

    @Override
    public void createReservation(CreateReservationRequest request, StreamObserver<CreateReservationResponse> out) {
        Reservation created = service.createReservation(
                bridge.body(request.getReservation(), Reservation.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateReservationResponse.newBuilder(), "reservation", created).build());
    }

    @Override
    public void updateReservation(UpdateReservationRequest request, StreamObserver<UpdateReservationResponse> out) {
        Reservation updated = service.updateReservation(
                bridge.id(request.getReservation().getId(), "id"),
                bridge.requiredIfMatch(request.getReservation().getEtag()),
                bridge.changes(request.getReservation(), request.getUpdateMask(), ReservationUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateReservationResponse.newBuilder(), "reservation", updated).build());
    }

    @Override
    public void deleteReservation(DeleteReservationRequest request, StreamObserver<DeleteReservationResponse> out) {
        service.deleteReservation(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteReservationResponse.getDefaultInstance());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void acceptReservation(AcceptReservationRequest request, StreamObserver<AcceptReservationResponse> out) {
        Reservation changed = service.acceptReservation(
                bridge.id(request.getReservationId(), "reservationId"),
                bridge.body(request, com.mysms.library.api.model.AcceptReservationRequest.class,
                        "reservation_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(AcceptReservationResponse.newBuilder(), "reservation", changed).build());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void rejectReservation(RejectReservationRequest request, StreamObserver<RejectReservationResponse> out) {
        Reservation changed = service.rejectReservation(
                bridge.id(request.getReservationId(), "reservationId"),
                bridge.body(request, com.mysms.library.api.model.RejectReservationRequest.class,
                        "reservation_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectReservationResponse.newBuilder(), "reservation", changed).build());
    }

    @Override
    public void requestReservationAgain(RequestReservationAgainRequest request,
            StreamObserver<RequestReservationAgainResponse> out) {
        Reservation changed = service.requestReservationAgain(
                bridge.id(request.getReservationId(), "reservationId"),
                bridge.body(request, com.mysms.library.api.model.RequestReservationAgainRequest.class,
                        "reservation_id", "request_id"));
        ProtoBridge.send(out,
                bridge.reply(RequestReservationAgainResponse.newBuilder(), "reservation", changed).build());
    }
}
