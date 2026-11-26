package com.mysms.academics.attendance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.timetable.v1.GetSessionRequest;
import com.mysms.timetable.v1.GetSessionResponse;
import com.mysms.timetable.v1.ListSessionStudentsRequest;
import com.mysms.timetable.v1.ListSessionStudentsResponse;
import com.mysms.timetable.v1.SessionServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the timetable service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class TimetableGrpcClient {

    /** The starter's client of the timetable service. */
    public static final String TIMETABLE_SERVICE_KEY = "timetable-service";
    private static final String SERVICE = "timetable";

    private SessionServiceGrpc.SessionServiceBlockingStub sessions;

    @GrpcClient(TIMETABLE_SERVICE_KEY)
    public void initSessionStub(SessionServiceGrpc.SessionServiceBlockingStub stub) {
        this.sessions = stub;
    }

    public GetSessionResponse getSession(GetSessionRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> sessions.getSession(request));
    }

    public ListSessionStudentsResponse listSessionStudents(ListSessionStudentsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> sessions.listSessionStudents(request));
    }
}
