package com.mysms.finance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.sis.v1.GetProgramEnrollmentRequest;
import com.mysms.sis.v1.GetProgramEnrollmentResponse;
import com.mysms.sis.v1.GetStudentSummaryRequest;
import com.mysms.sis.v1.GetStudentSummaryResponse;
import com.mysms.sis.v1.GuardianServiceGrpc;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListMyChildrenRequest;
import com.mysms.sis.v1.ListMyChildrenResponse;
import com.mysms.sis.v1.ListProgramEnrollmentsRequest;
import com.mysms.sis.v1.ListProgramEnrollmentsResponse;
import com.mysms.sis.v1.ProgramEnrollmentServiceGrpc;
import com.mysms.sis.v1.StudentEnrollmentServiceGrpc;
import com.mysms.sis.v1.StudentServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Calls the sis service with the caller's own identity; a failed call throws {@link IntegrationGrpcClientException}.
 */
@Service
public class SisGrpcClient {

    /** The starter's client of the sis service. */
    public static final String SIS_SERVICE_KEY = "sis-service";
    private static final String SERVICE = "sis";

    private StudentServiceGrpc.StudentServiceBlockingStub students;
    private GuardianServiceGrpc.GuardianServiceBlockingStub guardians;
    private StudentEnrollmentServiceGrpc.StudentEnrollmentServiceBlockingStub studentEnrollments;
    private ProgramEnrollmentServiceGrpc.ProgramEnrollmentServiceBlockingStub programEnrollments;

    @GrpcClient(SIS_SERVICE_KEY)
    public void initStudentStub(StudentServiceGrpc.StudentServiceBlockingStub stub) {
        this.students = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initGuardianStub(GuardianServiceGrpc.GuardianServiceBlockingStub stub) {
        this.guardians = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initStudentEnrollmentStub(StudentEnrollmentServiceGrpc.StudentEnrollmentServiceBlockingStub stub) {
        this.studentEnrollments = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initProgramEnrollmentStub(ProgramEnrollmentServiceGrpc.ProgramEnrollmentServiceBlockingStub stub) {
        this.programEnrollments = stub;
    }

    public GetStudentSummaryResponse getStudentSummary(GetStudentSummaryRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.getStudentSummary(request));
    }

    public ListMyChildrenResponse listMyChildren(ListMyChildrenRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> guardians.listMyChildren(request));
    }

    public ListClassRosterResponse listClassRoster(ListClassRosterRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> studentEnrollments.listClassRoster(request));
    }

    public GetProgramEnrollmentResponse getProgramEnrollment(GetProgramEnrollmentRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> programEnrollments.getProgramEnrollment(request));
    }

    public ListProgramEnrollmentsResponse listProgramEnrollments(ListProgramEnrollmentsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> programEnrollments.listProgramEnrollments(request));
    }
}
