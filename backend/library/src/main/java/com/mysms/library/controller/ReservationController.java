package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.ReservationApi;
import com.mysms.library.api.model.AcceptReservationRequest;
import com.mysms.library.api.model.RejectReservationRequest;
import com.mysms.library.api.model.RequestReservationAgainRequest;
import com.mysms.library.api.model.Reservation;
import com.mysms.library.api.model.ReservationUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.ReservationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Reservation operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ReservationController implements ReservationApi {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<Reservation> acceptReservation(UUID reservationId,
            AcceptReservationRequest acceptReservationRequest, UUID idempotencyKey) {
        Reservation changed = reservationService.acceptReservation(reservationId, acceptReservationRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Reservation> createReservation(Reservation reservation, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Reservation created = reservationService.createReservation(reservation, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/reservations/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteReservation(UUID reservationId, String ifMatch) {
        reservationService.deleteReservation(reservationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Reservation> getReservation(UUID reservationId, String fields) {
        Reservation found = reservationService.getReservation(reservationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Reservation>> listReservations(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Reservation> list =
                PagingUtil.pagingInfo(reservationService.listReservations(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Reservation> rejectReservation(UUID reservationId,
            RejectReservationRequest rejectReservationRequest, UUID idempotencyKey) {
        Reservation changed = reservationService.rejectReservation(reservationId, rejectReservationRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Reservation> requestReservationAgain(UUID reservationId,
            RequestReservationAgainRequest requestReservationAgainRequest, UUID idempotencyKey) {
        Reservation changed = reservationService.requestReservationAgain(reservationId,
                requestReservationAgainRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Reservation> updateReservation(UUID reservationId, String ifMatch,
            ReservationUpdate reservationUpdate, String updateMask, Boolean validateOnly) {
        Reservation updated =
                reservationService.updateReservation(reservationId, ifMatch, reservationUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
