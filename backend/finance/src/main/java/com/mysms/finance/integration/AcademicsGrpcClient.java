package com.mysms.finance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.academics.v1.ClassSectionServiceGrpc;
import com.mysms.academics.v1.CourseServiceGrpc;
import com.mysms.academics.v1.ListClassSectionsRequest;
import com.mysms.academics.v1.ListClassSectionsResponse;
import com.mysms.academics.v1.ListCoursesRequest;
import com.mysms.academics.v1.ListCoursesResponse;
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
    private CourseServiceGrpc.CourseServiceBlockingStub courses;

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initClassSectionStub(ClassSectionServiceGrpc.ClassSectionServiceBlockingStub stub) {
        this.classSections = stub;
    }

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initCourseStub(CourseServiceGrpc.CourseServiceBlockingStub stub) {
        this.courses = stub;
    }

    public ListClassSectionsResponse listClassSections(ListClassSectionsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> classSections.listClassSections(request));
    }

    public ListCoursesResponse listCourses(ListCoursesRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> courses.listCourses(request));
    }
}
