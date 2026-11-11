package com.mysms.assessment.config;

import com.mysms.assessment.business.ExamRoomLookup;
import com.mysms.assessment.business.ExamRosterLookup;
import com.mysms.assessment.business.ExamCoursePlanLookup;
import com.mysms.assessment.business.StudentResultAccessLookup;
import com.mysms.core.sdk.SdkClientFactory;
import com.mysms.sdk.academics.api.CourseApi;
import com.mysms.sdk.facility.api.RoomApi;
import com.mysms.sdk.sis.api.StudentEnrollmentApi;
import com.mysms.sdk.sis.api.StudentApi;
import com.mysms.sdk.sis.api.GuardianApi;
import com.mysms.sdk.sis.api.SubjectRegistrationApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** HTTP clients for SIS and facility data; downstream requests use the caller's token. */
@Configuration(proxyBeanMethods = false)
public class AssessmentClientsConfig {

    @Bean
    public AssessmentProperties assessmentProperties(
            @Value("${mysms.assessment.sis-base-url:${MYSMS_SIS_URL:}}") String sisBaseUrl,
            @Value("${mysms.assessment.facility-base-url:${MYSMS_FACILITY_URL:}}") String facilityBaseUrl,
            @Value("${mysms.assessment.academics-base-url:${MYSMS_ACADEMICS_URL:}}") String academicsBaseUrl) {
        return new AssessmentProperties(sisBaseUrl, facilityBaseUrl, academicsBaseUrl);
    }

    @Bean
    public ExamRosterLookup examRosterLookup(AssessmentProperties properties, SdkClientFactory clients) {
        if (!properties.rostersConfigured()) {
            return ExamRosterLookup.unconfigured();
        }
        return new ExamRosterLookup(clients.create(StudentEnrollmentApi.class, properties.sisBaseUrl()),
                clients.create(SubjectRegistrationApi.class, properties.sisBaseUrl()));
    }

    @Bean
    public ExamRoomLookup examRoomLookup(AssessmentProperties properties, SdkClientFactory clients) {
        if (!properties.roomsConfigured()) {
            return ExamRoomLookup.unconfigured();
        }
        return new ExamRoomLookup(clients.create(RoomApi.class, properties.facilityBaseUrl()));
    }

    @Bean
    public ExamCoursePlanLookup examCoursePlanLookup(AssessmentProperties properties, SdkClientFactory clients) {
        if (!properties.coursePlansConfigured()) {
            return ExamCoursePlanLookup.unconfigured();
        }
        return new ExamCoursePlanLookup(clients.create(CourseApi.class, properties.academicsBaseUrl()));
    }

    @Bean
    public StudentResultAccessLookup studentResultAccessLookup(AssessmentProperties properties, SdkClientFactory clients) {
        if (!properties.rostersConfigured()) {
            return StudentResultAccessLookup.unconfigured();
        }
        return new StudentResultAccessLookup(clients.create(StudentApi.class, properties.sisBaseUrl()),
                clients.create(GuardianApi.class, properties.sisBaseUrl()));
    }
}
