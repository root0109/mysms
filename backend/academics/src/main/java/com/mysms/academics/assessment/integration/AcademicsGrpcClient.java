package com.mysms.academics.assessment.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.academics.v1.CourseServiceGrpc;
import com.mysms.academics.v1.ListCourseSubjectsRequest;
import com.mysms.academics.v1.ListCourseSubjectsResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the academics service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class AcademicsGrpcClient {

    /** The starter's client of the academics service. */
    public static final String ACADEMICS_SERVICE_KEY = "academics-service";
    private static final String SERVICE = "academics";

    private CourseServiceGrpc.CourseServiceBlockingStub courses;

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initCourseStub(CourseServiceGrpc.CourseServiceBlockingStub stub) {
        this.courses = stub;
    }

    public ListCourseSubjectsResponse listCourseSubjects(ListCourseSubjectsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> courses.listCourseSubjects(request));
    }
}
