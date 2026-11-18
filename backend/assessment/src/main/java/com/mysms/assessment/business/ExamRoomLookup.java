package com.mysms.assessment.business;

import com.mysms.assessment.integration.FacilityGrpcClient;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.facility.v1.GetRoomRequest;
import com.mysms.facility.v1.Room;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Reads room ownership, capacity and whether the room is in use from facility, with the caller's identity. */
@Component
public class ExamRoomLookup {

    private static final String SERVICE = "facility";
    private final FacilityGrpcClient facility;

    public ExamRoomLookup(FacilityGrpcClient facility) {
        this.facility = facility;
    }

    /** The room as facility shows it to the caller; 404 when facility does not know it (or does not show it). */
    public Room requireRoom(UUID id) {
        return Downstream.required(SERVICE, "facility.room.read", "Allocating exam rooms",
                        () -> facility.getRoom(GetRoomRequest.newBuilder().setId(id.toString()).build()).getRoom())
                .orElseThrow(() -> new ResourceNotFoundException("facility.room", id));
    }
}
