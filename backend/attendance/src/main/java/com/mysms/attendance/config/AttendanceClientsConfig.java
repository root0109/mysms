package com.mysms.attendance.config;

import com.mysms.attendance.business.SessionLookup;
import com.mysms.core.sdk.SdkClientFactory;
import com.mysms.sdk.org.api.InstitutionApi;
import com.mysms.sdk.sis.api.StudentApi;
import com.mysms.sdk.sis.api.StudentEnrollmentApi;
import com.mysms.sdk.timetable.api.SessionApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SDK clients attendance calls other modules with (JAVA-CODING-RULES §2: over HTTP even inside the monolith, with
 * the caller's own token). They exist only when all three services have an address; without them
 * attendance.TakeAttendanceForSession, and a mark for a student the sheet has no record of yet, answer 503; nothing
 * else is affected.
 */
@Configuration(proxyBeanMethods = false)
public class AttendanceClientsConfig {

    @Bean
    public AttendanceProperties attendanceProperties(
            @Value("${mysms.attendance.timetable-base-url:${MYSMS_TIMETABLE_URL:}}") String timetableBaseUrl,
            @Value("${mysms.attendance.sis-base-url:${MYSMS_SIS_URL:}}") String sisBaseUrl,
            @Value("${mysms.attendance.org-base-url:${MYSMS_ORG_URL:}}") String orgBaseUrl) {
        return new AttendanceProperties(timetableBaseUrl, sisBaseUrl, orgBaseUrl);
    }

    @Bean
    public SessionLookup sessionLookup(AttendanceProperties properties, SdkClientFactory clients) {
        if (!properties.sessionLookupConfigured()) {
            return SessionLookup.unconfigured();
        }
        return new SessionLookup(clients.create(SessionApi.class, properties.timetableBaseUrl()),
                clients.create(StudentEnrollmentApi.class, properties.sisBaseUrl()),
                clients.create(StudentApi.class, properties.sisBaseUrl()),
                clients.create(InstitutionApi.class, properties.orgBaseUrl()));
    }
}
