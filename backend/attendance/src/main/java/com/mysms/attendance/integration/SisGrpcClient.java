package com.mysms.attendance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListStudentsRequest;
import com.mysms.sis.v1.ListStudentsResponse;
import com.mysms.sis.v1.StudentEnrollmentServiceGrpc;
import com.mysms.sis.v1.StudentServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the sis service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class SisGrpcClient {

    /** The starter's client of the sis service. */
    public static final String SIS_SERVICE_KEY = "sis-service";
    private static final String SERVICE = "sis";

    private StudentEnrollmentServiceGrpc.StudentEnrollmentServiceBlockingStub studentEnrollments;
    private StudentServiceGrpc.StudentServiceBlockingStub students;

    @GrpcClient(SIS_SERVICE_KEY)
    public void initStudentEnrollmentStub(StudentEnrollmentServiceGrpc.StudentEnrollmentServiceBlockingStub stub) {
        this.studentEnrollments = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initStudentStub(StudentServiceGrpc.StudentServiceBlockingStub stub) {
        this.students = stub;
    }

    public ListClassRosterResponse listClassRoster(ListClassRosterRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> studentEnrollments.listClassRoster(request));
    }

    public ListStudentsResponse listStudents(ListStudentsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.listStudents(request));
    }
}
