package com.mysms.admission.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.sis.v1.CreateGuardianRequest;
import com.mysms.sis.v1.CreateGuardianResponse;
import com.mysms.sis.v1.CreatePortalAccountRequest;
import com.mysms.sis.v1.CreatePortalAccountResponse;
import com.mysms.sis.v1.CreateProgramEnrollmentRequest;
import com.mysms.sis.v1.CreateProgramEnrollmentResponse;
import com.mysms.sis.v1.CreateStudentEnrollmentRequest;
import com.mysms.sis.v1.CreateStudentEnrollmentResponse;
import com.mysms.sis.v1.CreateStudentGuardianRequest;
import com.mysms.sis.v1.CreateStudentGuardianResponse;
import com.mysms.sis.v1.CreateStudentPortalAccountRequest;
import com.mysms.sis.v1.CreateStudentPortalAccountResponse;
import com.mysms.sis.v1.CreateStudentRequest;
import com.mysms.sis.v1.CreateStudentResponse;
import com.mysms.sis.v1.GetStudentRequest;
import com.mysms.sis.v1.GetStudentResponse;
import com.mysms.sis.v1.GuardianServiceGrpc;
import com.mysms.sis.v1.ListGuardiansRequest;
import com.mysms.sis.v1.ListGuardiansResponse;
import com.mysms.sis.v1.ListProgramEnrollmentsRequest;
import com.mysms.sis.v1.ListProgramEnrollmentsResponse;
import com.mysms.sis.v1.ListRelationshipTypesRequest;
import com.mysms.sis.v1.ListRelationshipTypesResponse;
import com.mysms.sis.v1.ListStudentEnrollmentsRequest;
import com.mysms.sis.v1.ListStudentEnrollmentsResponse;
import com.mysms.sis.v1.ListStudentGuardiansRequest;
import com.mysms.sis.v1.ListStudentGuardiansResponse;
import com.mysms.sis.v1.ListStudentsRequest;
import com.mysms.sis.v1.ListStudentsResponse;
import com.mysms.sis.v1.ProgramEnrollmentServiceGrpc;
import com.mysms.sis.v1.RelationshipTypeServiceGrpc;
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

    private StudentServiceGrpc.StudentServiceBlockingStub students;
    private StudentEnrollmentServiceGrpc.StudentEnrollmentServiceBlockingStub studentEnrollments;
    private ProgramEnrollmentServiceGrpc.ProgramEnrollmentServiceBlockingStub programEnrollments;
    private GuardianServiceGrpc.GuardianServiceBlockingStub guardians;
    private RelationshipTypeServiceGrpc.RelationshipTypeServiceBlockingStub relationshipTypes;

    @GrpcClient(SIS_SERVICE_KEY)
    public void initStudentStub(StudentServiceGrpc.StudentServiceBlockingStub stub) {
        this.students = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initStudentEnrollmentStub(StudentEnrollmentServiceGrpc.StudentEnrollmentServiceBlockingStub stub) {
        this.studentEnrollments = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initProgramEnrollmentStub(ProgramEnrollmentServiceGrpc.ProgramEnrollmentServiceBlockingStub stub) {
        this.programEnrollments = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initGuardianStub(GuardianServiceGrpc.GuardianServiceBlockingStub stub) {
        this.guardians = stub;
    }

    @GrpcClient(SIS_SERVICE_KEY)
    public void initRelationshipTypeStub(RelationshipTypeServiceGrpc.RelationshipTypeServiceBlockingStub stub) {
        this.relationshipTypes = stub;
    }

    public GetStudentResponse getStudent(GetStudentRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.getStudent(request));
    }

    public ListStudentsResponse listStudents(ListStudentsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.listStudents(request));
    }

    public CreateStudentResponse createStudent(CreateStudentRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.createStudent(request));
    }

    public ListStudentGuardiansResponse listStudentGuardians(ListStudentGuardiansRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.listStudentGuardians(request));
    }

    public CreateStudentGuardianResponse createStudentGuardian(CreateStudentGuardianRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.createStudentGuardian(request));
    }

    public CreateStudentPortalAccountResponse createStudentPortalAccount(CreateStudentPortalAccountRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> students.createStudentPortalAccount(request));
    }

    public ListStudentEnrollmentsResponse listStudentEnrollments(ListStudentEnrollmentsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> studentEnrollments.listStudentEnrollments(request));
    }

    public CreateStudentEnrollmentResponse createStudentEnrollment(CreateStudentEnrollmentRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> studentEnrollments.createStudentEnrollment(request));
    }

    public ListProgramEnrollmentsResponse listProgramEnrollments(ListProgramEnrollmentsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> programEnrollments.listProgramEnrollments(request));
    }

    public CreateProgramEnrollmentResponse createProgramEnrollment(CreateProgramEnrollmentRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> programEnrollments.createProgramEnrollment(request));
    }

    public ListGuardiansResponse listGuardians(ListGuardiansRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> guardians.listGuardians(request));
    }

    public CreateGuardianResponse createGuardian(CreateGuardianRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> guardians.createGuardian(request));
    }

    public CreatePortalAccountResponse createPortalAccount(CreatePortalAccountRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> guardians.createPortalAccount(request));
    }

    public ListRelationshipTypesResponse listRelationshipTypes(ListRelationshipTypesRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> relationshipTypes.listRelationshipTypes(request));
    }
}
