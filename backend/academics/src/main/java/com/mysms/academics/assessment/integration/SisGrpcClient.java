package com.mysms.academics.assessment.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.sis.v1.GetStudentSummaryRequest;
import com.mysms.sis.v1.GetStudentSummaryResponse;
import com.mysms.sis.v1.GuardianServiceGrpc;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListMyChildrenRequest;
import com.mysms.sis.v1.ListMyChildrenResponse;
import com.mysms.sis.v1.ListSubjectRegistrationsRequest;
import com.mysms.sis.v1.ListSubjectRegistrationsResponse;
import com.mysms.sis.v1.StudentEnrollmentServiceGrpc;
import com.mysms.sis.v1.StudentServiceGrpc;
import com.mysms.sis.v1.SubjectRegistrationServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the sis service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class SisGrpcClient {

    /** The starter's client of the sis service. */
    public static final String SIS_SERVICE_KEY = "sis-service";
    private static final String SERVICE = "sis";

    private StudentServiceGrpc.StudentServiceBlockingStub students;
    private GuardianServiceGrpc.GuardianServiceBlockingStub guardians;
    private StudentEnrollmentServiceGrpc.StudentEnrollmentServiceBlockingStub studentEnrollments;
    private SubjectRegistrationServiceGrpc.SubjectRegistrationServiceBlockingStub subjectRegistrations;

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
    public void initSubjectRegistrationStub(SubjectRegistrationServiceGrpc.SubjectRegistrationServiceBlockingStub stub) {
        this.subjectRegistrations = stub;
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

    public ListSubjectRegistrationsResponse listSubjectRegistrations(ListSubjectRegistrationsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> subjectRegistrations.listSubjectRegistrations(request));
    }
}
