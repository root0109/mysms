package com.mysms.assessment.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.assessment.integration.FacilityGrpcClient;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.facility.v1.GetRoomRequest;
import com.mysms.facility.v1.GetRoomResponse;
import com.mysms.facility.v1.Room;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

/** How another module's answers become assessment's (Downstream), seen through the room lookup. */
class DownstreamTest {

    @Test
    void aRequiredReadMapsTheOtherModulesStatus() {
        assertThat(Downstream.required("facility", "facility.room.read", "Allocating exam rooms", () -> "room"))
                .contains("room");
        assertThat(Downstream.required("facility", "facility.room.read", "Allocating exam rooms",
                failing(HttpStatus.NOT_FOUND))).isEmpty();
        assertThatThrownBy(() -> Downstream.required("facility", "facility.room.read", "Allocating exam rooms",
                failing(HttpStatus.FORBIDDEN)))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessage("Allocating exam rooms also needs facility.room.read.");
        assertThatThrownBy(() -> Downstream.required("facility", "facility.room.read", "Allocating exam rooms",
                failing(HttpStatus.INTERNAL_SERVER_ERROR)))
                .isInstanceOf(UnavailableException.class).hasMessage("The facility service could not answer.");
        assertThatThrownBy(() -> Downstream.required("facility", "facility.room.read", "Allocating exam rooms",
                () -> {
                    throw GrpcRefusals.unreachable("facility");
                })).isInstanceOf(UnavailableException.class);
    }

    @Test
    void anOwnerCheckTreatsARefusalAsNotTheCallers() {
        assertThat(Downstream.ownerCheck("sis", failing(HttpStatus.FORBIDDEN))).isEmpty();
        assertThat(Downstream.ownerCheck("sis", failing(HttpStatus.NOT_FOUND))).isEmpty();
        assertThatThrownBy(() -> Downstream.ownerCheck("sis", failing(HttpStatus.TOO_MANY_REQUESTS)))
                .isInstanceOf(UnavailableException.class);
    }

    @Test
    void theRoomLookupAnswersTheRoomOr404() {
        FacilityGrpcClient facility = Mockito.mock(FacilityGrpcClient.class);
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        Mockito.when(facility.getRoom(GetRoomRequest.newBuilder().setId(known.toString()).build()))
                .thenReturn(GetRoomResponse.newBuilder().setRoom(Room.newBuilder().setCapacity(30)).build());
        Mockito.when(facility.getRoom(GetRoomRequest.newBuilder().setId(unknown.toString()).build()))
                .thenThrow(GrpcRefusals.refused("facility", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        ExamRoomLookup lookup = new ExamRoomLookup(facility);

        assertThat(lookup.requireRoom(known).getCapacity()).isEqualTo(30);
        assertThatThrownBy(() -> lookup.requireRoom(unknown)).isInstanceOf(ResourceNotFoundException.class);
    }

    /** A call that fails the way the gRPC client does when the callee refuses with this status. */
    private static Supplier<String> failing(HttpStatus status) {
        return () -> {
            throw GrpcRefusals.refused("facility", status, "REFUSED");
        };
    }
}
