package com.mysms.assessment.business;

import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import com.mysms.sdk.facility.api.RoomApi;
import com.mysms.sdk.facility.model.Room;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Reads room ownership and capacity from facility. */
public class ExamRoomLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ExamRoomLookup.class);
    private final RoomApi rooms;

    public ExamRoomLookup(RoomApi rooms) {
        this.rooms = rooms;
    }

    public static ExamRoomLookup unconfigured() {
        return new ExamRoomLookup(null);
    }

    public Room requireRoom(UUID id) {
        if (rooms == null) {
            throw new UnavailableException("The facility service is not configured for room allocation.");
        }
        return call(() -> rooms.getRoom(id, "id,institutionId,capacity,isActive,roomType"))
                .orElseThrow(() -> new ResourceNotFoundException("facility.room", id));
    }

    private static <T> Optional<T> call(Supplier<ResponseEntity<T>> request) {
        try {
            return Optional.ofNullable(request.get().getBody());
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 403) {
                throw new PermissionDeniedException("Allocating exam rooms also needs facility.room.read.");
            }
            if (status.value() == 404) {
                return Optional.empty();
            }
            LOG.warn("facility answered {} to exam room lookup", status.value());
            throw new UnavailableException("The facility service could not provide a room.");
        } catch (RestClientException e) {
            LOG.warn("facility exam room lookup failed", e);
            throw new UnavailableException("The facility service could not provide a room.");
        }
    }
}
