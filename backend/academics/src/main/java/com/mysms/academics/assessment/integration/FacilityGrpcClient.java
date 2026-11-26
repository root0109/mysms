package com.mysms.academics.assessment.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.facility.v1.GetRoomRequest;
import com.mysms.facility.v1.GetRoomResponse;
import com.mysms.facility.v1.RoomServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the facility service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class FacilityGrpcClient {

    /** The starter's client of the facility service. */
    public static final String FACILITY_SERVICE_KEY = "facility-service";
    private static final String SERVICE = "facility";

    private RoomServiceGrpc.RoomServiceBlockingStub rooms;

    @GrpcClient(FACILITY_SERVICE_KEY)
    public void initRoomStub(RoomServiceGrpc.RoomServiceBlockingStub stub) {
        this.rooms = stub;
    }

    public GetRoomResponse getRoom(GetRoomRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> rooms.getRoom(request));
    }
}
