package com.mysms.attendance.business;

import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.paging.PagingUtil;
import com.mysms.sdk.org.api.InstitutionApi;
import com.mysms.sdk.sis.api.StudentApi;
import com.mysms.sdk.sis.api.StudentEnrollmentApi;
import com.mysms.sdk.sis.model.PagingInfoStudent;
import com.mysms.sdk.sis.model.PagingInfoStudentEnrollment;
import com.mysms.sdk.sis.model.Student;
import com.mysms.sdk.sis.model.StudentEnrollment;
import com.mysms.sdk.timetable.api.SessionApi;
import com.mysms.sdk.timetable.model.PagingInfoSessionStudent;
import com.mysms.sdk.timetable.model.Session;
import com.mysms.sdk.timetable.model.SessionStudent;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Gateway to what other modules own about a timetable session and about students (an Adapter over their SDK clients,
 * so the attendance workflow never sees HTTP): the session itself from timetable, its expected students — the
 * session's own student list, or the class roster from sis when it has none — the institution's time zone from org,
 * which decides the attendance date of the session, and whether a student id names a student of an institution
 * (the reference check of a new mark, JAVA-CODING-RULES §2). Every call carries the caller's own token, so the
 * caller sees through this gateway exactly what they may read in those modules.
 */
public class SessionLookup {

    /** Page size of the downstream lists; the largest the API serves. */
    static final int PAGE_SIZE = 500;
    /** Pages read per list before the lookup gives up: 4 pages of 500 is far beyond any class or session. */
    static final int MAX_PAGES = 4;
    /** Student ids per reference check: the list filter takes 50 terms and 2,000 characters; 40 ids fit both. */
    static final int ID_CHUNK = 40;
    private static final Logger LOG = LoggerFactory.getLogger(SessionLookup.class);

    /**
     * What attendance needs to open a sheet for a session.
     *
     * @param date       the session's start, as a date in its institution's time zone
     * @param studentIds the expected students, in the order their owners list them, without repeats
     */
    public record SessionPlan(UUID sessionId, UUID institutionId, LocalDate date, UUID classSectionId, UUID courseId,
                              UUID batchId, UUID subjectId, UUID staffId, List<UUID> studentIds) {

        public SessionPlan {
            studentIds = List.copyOf(studentIds);
        }
    }

    private final SessionApi sessions;
    private final StudentEnrollmentApi enrollments;
    private final StudentApi students;
    private final InstitutionApi institutions;

    /** The SDK clients of the three services — all of them, or none when the deployment does not configure them. */
    public SessionLookup(SessionApi sessions, StudentEnrollmentApi enrollments, StudentApi students,
            InstitutionApi institutions) {
        this.sessions = sessions;
        this.enrollments = enrollments;
        this.students = students;
        this.institutions = institutions;
    }

    /** The lookup of a deployment without the three services: every request is answered 503. */
    public static SessionLookup unconfigured() {
        return new SessionLookup(null, null, null, null);
    }

    /**
     * Reads the session and its expected students. An unknown or invisible session is a validation failure of the
     * request's field (422); a cancelled one cannot have attendance taken (409); a caller without the read
     * permissions in the other modules is refused (403); an unconfigured or failing service is 503.
     */
    public SessionPlan plan(UUID sessionId) {
        requireConfigured();
        Session session = call("timetable", "timetable.session.read", () -> sessions.getSession(sessionId, null))
                .orElseThrow(() -> new ValidationFailedException(List.of(
                        new FieldError("timetableSessionId", "timetable session does not exist", "NOT_FOUND"))));
        if (session.getInstitutionId() == null || session.getStartAt() == null) {
            LOG.warn("the timetable service answered a session without its institution or start");
            throw new UnavailableException("The timetable service could not answer.");
        }
        if (session.getStatus() == Session.StatusEnum.CANCELLED) {
            throw new InvalidStateException("The timetable session is cancelled.");
        }
        ZoneId zone = call("org", "org.institution.read",
                () -> institutions.getInstitution(session.getInstitutionId(), null))
                .map(institution -> zoneOf(institution.getTimezone()))
                .orElseThrow(() -> new PermissionDeniedException(
                        "The session's institution is outside the caller's scope."));
        LocalDate date = session.getStartAt().atZoneSameInstant(zone).toLocalDate();
        List<UUID> students = sessionStudents(sessionId);
        if (students.isEmpty() && session.getClassSectionId() != null) {
            students = classRoster(session.getClassSectionId(), date);
        }
        return new SessionPlan(sessionId, session.getInstitutionId(), date, session.getClassSectionId(),
                session.getCourseId(), session.getBatchId(), session.getSubjectId(), session.getStaffId(), students);
    }

    /**
     * The ids among these that do not name a student of the institution, as far as the caller can see — the
     * reference check of a mark for a student the sheet has no record of yet. A student placed in the sheet's class
     * on its date is known from the class roster (a teacher's permission); any other is looked up as a student of
     * the institution (which needs the permission to read students).
     *
     * @param classSectionId the class of the sheet's register, or null when the register is not a class's
     */
    public Set<UUID> unknownStudents(UUID institutionId, UUID classSectionId, LocalDate date,
            Collection<UUID> studentIds) {
        requireConfigured();
        Set<UUID> unknown = new LinkedHashSet<>(studentIds);
        if (classSectionId != null && !unknown.isEmpty()) {
            try {
                unknown.removeAll(classRoster(classSectionId, date));
            } catch (PermissionDeniedException withoutRoster) {
                // a caller who may not list the roster is judged by the student lookup alone
                LOG.debug("the class roster is not readable for this caller; checking students directly");
            }
        }
        List<UUID> remaining = List.copyOf(unknown);
        for (int start = 0; start < remaining.size(); start += ID_CHUNK) {
            List<UUID> chunk = remaining.subList(start, Math.min(start + ID_CHUNK, remaining.size()));
            String filter = chunk.stream().map(id -> "id = \"" + id + "\"").collect(Collectors.joining(" OR "));
            PagingInfoStudent found = call("sis", "sis.student.read",
                    () -> students.listStudents(1, ID_CHUNK, null, filter, null, null))
                    .orElseThrow(() -> new UnavailableException("The sis service did not list the students."));
            for (Student student : found.getData()) {
                if (institutionId.equals(student.getInstitutionId())) {
                    unknown.remove(student.getId());
                }
            }
        }
        return unknown;
    }

    private void requireConfigured() {
        if (sessions == null || enrollments == null || students == null || institutions == null) {
            throw new UnavailableException("Attendance needs the timetable, sis and org services for this, and "
                    + "they are not configured for this deployment.");
        }
    }

    /** The students named on the session itself (elective groups, combined classes). */
    private List<UUID> sessionStudents(UUID sessionId) {
        Set<UUID> students = new LinkedHashSet<>();
        Optional<String> cursor = Optional.empty();
        for (int page = 1; page <= MAX_PAGES; page++) {
            int number = page;
            String after = cursor.orElse(null);
            PagingInfoSessionStudent list = call("timetable", "timetable.session_student.read",
                    () -> sessions.listSessionStudents(sessionId, number, PAGE_SIZE, after, null, null, null))
                    .orElseThrow(() -> new UnavailableException("The timetable service did not list the session."));
            for (SessionStudent student : list.getData()) {
                students.add(student.getStudentId());
            }
            cursor = PagingUtil.cursorOf(list.getPaging().getNext());
            if (cursor.isEmpty()) {
                return new ArrayList<>(students);
            }
        }
        throw new InvalidStateException("The session lists more students than attendance can pre-fill.");
    }

    /** The students placed in the class on the date: an enrollment that starts later or ended before is not expected. */
    private List<UUID> classRoster(UUID classSectionId, LocalDate date) {
        Set<UUID> students = new LinkedHashSet<>();
        Optional<String> cursor = Optional.empty();
        for (int page = 1; page <= MAX_PAGES; page++) {
            int number = page;
            String after = cursor.orElse(null);
            PagingInfoStudentEnrollment list = call("sis", "sis.student_enrollment.list_class_roster",
                    () -> enrollments.listClassRoster(classSectionId, number, PAGE_SIZE, after))
                    .orElseThrow(() -> new UnavailableException("The sis service did not list the class roster."));
            for (StudentEnrollment enrollment : list.getData()) {
                LocalDate from = enrollment.getEffectiveFrom();
                LocalDate to = enrollment.getEffectiveTo();
                if ((from == null || !from.isAfter(date)) && (to == null || !to.isBefore(date))) {
                    students.add(enrollment.getStudentId());
                }
            }
            cursor = PagingUtil.cursorOf(list.getPaging().getNext());
            if (cursor.isEmpty()) {
                return new ArrayList<>(students);
            }
        }
        throw new InvalidStateException("The class roster is larger than attendance can pre-fill.");
    }

    /** An institution without a (valid) time zone keeps its sessions' dates in UTC; an invalid one is logged. */
    private static ZoneId zoneOf(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            LOG.warn("an institution has an invalid time zone; its attendance dates are taken in UTC");
            return ZoneOffset.UTC;
        }
    }

    /**
     * One downstream read: the body, or empty when the other module answers 404 (the row does not exist for this
     * caller). 403 becomes this operation's 403 naming the permission the caller lacks; anything else is 503 — the
     * downstream status and message never reach the client.
     */
    private static <T> Optional<T> call(String service, String permission, Supplier<ResponseEntity<T>> request) {
        try {
            return Optional.ofNullable(request.get().getBody());
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                return Optional.empty();
            }
            if (status.value() == 403) {
                throw new PermissionDeniedException("Taking attendance for a session also needs " + permission + ".");
            }
            LOG.warn("the {} service answered {} to the session lookup", service, status.value());
            throw new UnavailableException("The " + service + " service could not answer.");
        } catch (RestClientException e) {
            LOG.warn("the {} service could not be reached for the session lookup ({})", service,
                    e.getClass().getSimpleName());
            throw new UnavailableException("The " + service + " service could not answer.");
        }
    }
}
