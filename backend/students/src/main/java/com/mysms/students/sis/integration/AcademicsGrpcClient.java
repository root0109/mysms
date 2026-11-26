package com.mysms.students.sis.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.academics.v1.ClassSectionServiceGrpc;
import com.mysms.academics.v1.CourseServiceGrpc;
import com.mysms.academics.v1.GetClassSectionRequest;
import com.mysms.academics.v1.GetClassSectionResponse;
import com.mysms.academics.v1.GetGradeLevelRequest;
import com.mysms.academics.v1.GetGradeLevelResponse;
import com.mysms.academics.v1.GetSectionRequest;
import com.mysms.academics.v1.GetSectionResponse;
import com.mysms.academics.v1.GradeLevelServiceGrpc;
import com.mysms.academics.v1.ListClassSectionsRequest;
import com.mysms.academics.v1.ListClassSectionsResponse;
import com.mysms.academics.v1.ListCourseSubjectsRequest;
import com.mysms.academics.v1.ListCourseSubjectsResponse;
import com.mysms.academics.v1.SectionServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Calls the academics service with the caller's own identity; a failed call throws
 * {@link IntegrationGrpcClientException}.
 */
@Service
public class AcademicsGrpcClient {

    /** The starter's client of the academics service. */
    public static final String ACADEMICS_SERVICE_KEY = "academics-service";
    private static final String SERVICE = "academics";

    private ClassSectionServiceGrpc.ClassSectionServiceBlockingStub classSections;
    private GradeLevelServiceGrpc.GradeLevelServiceBlockingStub gradeLevels;
    private SectionServiceGrpc.SectionServiceBlockingStub sections;
    private CourseServiceGrpc.CourseServiceBlockingStub courses;

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initClassSectionStub(ClassSectionServiceGrpc.ClassSectionServiceBlockingStub stub) {
        this.classSections = stub;
    }

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initGradeLevelStub(GradeLevelServiceGrpc.GradeLevelServiceBlockingStub stub) {
        this.gradeLevels = stub;
    }

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initSectionStub(SectionServiceGrpc.SectionServiceBlockingStub stub) {
        this.sections = stub;
    }

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initCourseStub(CourseServiceGrpc.CourseServiceBlockingStub stub) {
        this.courses = stub;
    }

    public GetClassSectionResponse getClassSection(GetClassSectionRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> classSections.getClassSection(request));
    }

    public ListClassSectionsResponse listClassSections(ListClassSectionsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> classSections.listClassSections(request));
    }

    public GetGradeLevelResponse getGradeLevel(GetGradeLevelRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> gradeLevels.getGradeLevel(request));
    }

    public GetSectionResponse getSection(GetSectionRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> sections.getSection(request));
    }

    public ListCourseSubjectsResponse listCourseSubjects(ListCourseSubjectsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> courses.listCourseSubjects(request));
    }
}
