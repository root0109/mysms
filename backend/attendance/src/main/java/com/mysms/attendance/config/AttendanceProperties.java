package com.mysms.attendance.config;


/**
 * `mysms.attendance.*`: where attendance reads what other modules own when a sheet is opened for a timetable session
 * (attendance.TakeAttendanceForSession) — the session and its students from timetable, the class roster from sis and
 * the institution's time zone from org. Each is the base URL of that service, or of the monolith; while one is empty
 * the operation answers 503 and every other attendance operation works unchanged. Built by
 * {@link AttendanceClientsConfig} from the properties or, failing that, the environment variables named below.
 *
 * @param timetableBaseUrl base URL of the timetable service (MYSMS_TIMETABLE_URL)
 * @param sisBaseUrl       base URL of the sis service (MYSMS_SIS_URL)
 * @param orgBaseUrl       base URL of the org service (MYSMS_ORG_URL)
 */
public record AttendanceProperties(String timetableBaseUrl, String sisBaseUrl, String orgBaseUrl) {

    /** Whether every service the session lookup calls has an address. */
    public boolean sessionLookupConfigured() {
        return isSet(timetableBaseUrl) && isSet(sisBaseUrl) && isSet(orgBaseUrl);
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
