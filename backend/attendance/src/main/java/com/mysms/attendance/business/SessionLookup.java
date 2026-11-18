package com.mysms.attendance.business;

import com.mysms.attendance.integration.OrgGrpcClient;
import com.mysms.attendance.integration.SisGrpcClient;
import com.mysms.attendance.integration.TimetableGrpcClient;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.org.v1.GetInstitutionRequest;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListStudentsRequest;
import com.mysms.sis.v1.ListStudentsResponse;
import com.mysms.sis.v1.Student;
import com.mysms.sis.v1.StudentEnrollment;
import com.mysms.timetable.v1.GetSessionRequest;
import com.mysms.timetable.v1.ListSessionStudentsRequest;
import com.mysms.timetable.v1.ListSessionStudentsResponse;
import com.mysms.timetable.v1.Session;
import com.mysms.timetable.v1.SessionStudent;
import java.time.DateTimeException;
import java.time.Instant;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Adapter over the timetable, sis and org gRPC clients: the session, its expected students (its own list, else the
 * class roster from sis), the institution's time zone that decides the attendance date, and whether a student id names
 * a student of an institution. Every call carries the caller's own identity, so the caller sees exactly what they may
 * read in those modules.
 */
@Component
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

    private final TimetableGrpcClient timetable;
    private final SisGrpcClient sis;
    private final OrgGrpcClient org;

    public SessionLookup(TimetableGrpcClient timetable, SisGrpcClient sis, OrgGrpcClient org) {
        this.timetable = timetable;
        this.sis = sis;
        this.org = org;
    }

    /**
     * Reads the session and its expected students. An unknown or invisible session is a validation failure of the
     * request's field (422); a cancelled one cannot have attendance taken (409); a caller without the read
     * permissions in the other modules is refused (403); an unreachable or failing service is 503.
     */
    public SessionPlan plan(UUID sessionId) {
        Session session = call("timetable", "timetable.session.read", () -> timetable.getSession(GetSessionRequest
                .newBuilder().setId(sessionId.toString()).build()).getSession())
                .orElseThrow(() -> new ValidationFailedException(List.of(
                        new FieldError("timetableSessionId", "timetable session does not exist", "NOT_FOUND"))));
        if (session.getInstitutionId().isEmpty() || !session.hasStartAt()) {
            LOG.warn("the timetable service answered a session without its institution or start");
            throw new UnavailableException("The timetable service could not answer.");
        }
        if (session.getStatus() == Session.Status.STATUS_CANCELLED) {
            throw new InvalidStateException("The timetable session is cancelled.");
        }
        UUID institutionId = UUID.fromString(session.getInstitutionId());
        ZoneId zone = call("org", "org.institution.read", () -> org.getInstitution(GetInstitutionRequest.newBuilder()
                .setId(session.getInstitutionId()).build()).getInstitution())
                .map(institution -> zoneOf(institution.getTimezone()))
                .orElseThrow(() -> new PermissionDeniedException(
                        "The session's institution is outside the caller's scope."));
        LocalDate date = Instant.ofEpochSecond(session.getStartAt().getSeconds(), session.getStartAt().getNanos())
                .atZone(zone).toLocalDate();
        UUID classSectionId = id(session.getClassSectionId());
        List<UUID> students = sessionStudents(sessionId);
        if (students.isEmpty() && classSectionId != null) {
            students = classRoster(classSectionId, date);
        }
        return new SessionPlan(sessionId, institutionId, date, classSectionId, id(session.getCourseId()),
                id(session.getBatchId()), id(session.getSubjectId()), id(session.getStaffId()), students);
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
            ListStudentsResponse found = call("sis", "sis.student.read", () -> sis.listStudents(ListStudentsRequest
                    .newBuilder().setFilter(filter)
                    .setPageable(PageableRequest.newBuilder().setPage(1).setSize(ID_CHUNK)).build()))
                    .orElseThrow(() -> new UnavailableException("The sis service did not list the students."));
            for (Student student : found.getItemsList()) {
                if (institutionId.toString().equals(student.getInstitutionId())) {
                    unknown.remove(UUID.fromString(student.getId()));
                }
            }
        }
        return unknown;
    }

    /** The students named on the session itself (elective groups, combined classes). */
    private List<UUID> sessionStudents(UUID sessionId) {
        Set<UUID> students = new LinkedHashSet<>();
        String cursor = null;
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = page(page, cursor);
            ListSessionStudentsResponse list = call("timetable", "timetable.session_student.read",
                    () -> timetable.listSessionStudents(ListSessionStudentsRequest.newBuilder()
                            .setSessionId(sessionId.toString()).setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The timetable service did not list the session."));
            for (SessionStudent student : list.getItemsList()) {
                students.add(UUID.fromString(student.getStudentId()));
            }
            cursor = list.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return new ArrayList<>(students);
            }
        }
        throw new InvalidStateException("The session lists more students than attendance can pre-fill.");
    }

    /** The students placed in the class on the date: an enrollment that starts later or ended before is not expected. */
    private List<UUID> classRoster(UUID classSectionId, LocalDate date) {
        Set<UUID> students = new LinkedHashSet<>();
        String cursor = null;
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = page(page, cursor);
            ListClassRosterResponse list = call("sis", "sis.student_enrollment.list_class_roster",
                    () -> sis.listClassRoster(ListClassRosterRequest.newBuilder()
                            .setClassSectionId(classSectionId.toString()).setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The sis service did not list the class roster."));
            for (StudentEnrollment enrollment : list.getItemsList()) {
                LocalDate from = enrollment.hasEffectiveFrom() ? LocalDate.parse(enrollment.getEffectiveFrom()) : null;
                LocalDate to = enrollment.hasEffectiveTo() ? LocalDate.parse(enrollment.getEffectiveTo()) : null;
                if ((from == null || !from.isAfter(date)) && (to == null || !to.isBefore(date))) {
                    students.add(UUID.fromString(enrollment.getStudentId()));
                }
            }
            cursor = list.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return new ArrayList<>(students);
            }
        }
        throw new InvalidStateException("The class roster is larger than attendance can pre-fill.");
    }

    /** One page of a downstream list: the first by number, the next ones by the cursor the previous page gave. */
    private static PageableRequest page(int number, String cursor) {
        PageableRequest.Builder page = PageableRequest.newBuilder().setPage(number).setSize(PAGE_SIZE);
        if (cursor != null) {
            page.setCursor(cursor);
        }
        return page.build();
    }

    /** An optional id of a proto message: empty is "not set". */
    private static UUID id(String value) {
        return value.isEmpty() ? null : UUID.fromString(value);
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
     * One downstream read: the answer, or empty when the other module answers 404 (the row does not exist for this
     * caller). 403 becomes this operation's 403 naming the permission the caller lacks; anything else is 503 — the
     * downstream status and message never reach the client.
     */
    private static <T> Optional<T> call(String service, String permission, Supplier<T> request) {
        try {
            return Optional.of(request.get());
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            if (e.httpStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException("Taking attendance for a session also needs " + permission + ".");
            }
            LOG.warn("the {} service answered {} ({}) to the session lookup", service, e.httpStatus(), e.statusCode());
            throw new UnavailableException("The " + service + " service could not answer.");
        }
    }
}
