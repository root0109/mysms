package com.mysms.assessment.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.paging.PagingUtil;
import com.mysms.sdk.sis.api.GuardianApi;
import com.mysms.sdk.sis.api.StudentApi;
import com.mysms.sdk.sis.model.ChildCard;
import com.mysms.sdk.sis.model.PagingInfoChildCard;
import com.mysms.sdk.sis.model.Student;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Confirms a student result request belongs to the signed-in student or one of the caller's children. */
public class StudentResultAccessLookup {

    private static final Logger LOG = LoggerFactory.getLogger(StudentResultAccessLookup.class);
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private final StudentApi students;
    private final GuardianApi guardians;

    public StudentResultAccessLookup(StudentApi students, GuardianApi guardians) {
        this.students = students;
        this.guardians = guardians;
    }

    public static StudentResultAccessLookup unconfigured() {
        return new StudentResultAccessLookup(null, null);
    }

    public boolean isStudentOrGuardian(UUID studentId) {
        if (students == null || guardians == null) {
            throw new UnavailableException("The SIS service is not configured for student-result access checks.");
        }
        AuthContext caller = AuthContext.require();
        if (isGuardianChild(studentId)) {
            return true;
        }
        Student student = call("sis.student.read", () -> students.getStudent(studentId, "id,personId"))
                .orElseThrow(() -> new ResourceNotFoundException("sis.student", studentId));
        if (caller.personId().isPresent() && caller.personId().get().equals(student.getPersonId())) {
            return false;
        }
        throw new PermissionDeniedException("The caller cannot read this student's results.");
    }

    private boolean isGuardianChild(UUID studentId) {
        Optional<String> cursor = Optional.empty();
        for (int page = 1; page <= MAX_PAGES; page++) {
            int pageNumber = page;
            String after = cursor.orElse(null);
            PagingInfoChildCard children = call("sis.guardian.list_my_children",
                    () -> guardians.listMyChildren(pageNumber, PAGE_SIZE, after), false).orElse(null);
            if (children == null) {
                return false;
            }
            for (ChildCard child : children.getData()) {
                if (child.getStudent() != null && studentId.equals(child.getStudent().getId())) {
                    return true;
                }
            }
            cursor = PagingUtil.cursorOf(children.getPaging().getNext());
            if (cursor.isEmpty()) {
                return false;
            }
        }
        throw new UnavailableException("The SIS service returned an unexpectedly large guardian roster.");
    }
    private static <T> Optional<T> call(String permission, Supplier<ResponseEntity<T>> request) {
        return call(permission, request, true);
    }

    private static <T> Optional<T> call(String permission, Supplier<ResponseEntity<T>> request,
            boolean denyOnForbidden) {
        try {
            return Optional.ofNullable(request.get().getBody());
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 403) {
                if (!denyOnForbidden) {
                    return Optional.empty();
                }
                throw new PermissionDeniedException("Reading student results also needs " + permission + ".");
            }
            if (status.value() == 404) {
                return Optional.empty();
            }
            LOG.warn("SIS answered {} to student-result access lookup", status.value());
            throw new UnavailableException("The SIS service could not verify student-result access.");
        } catch (RestClientException e) {
            LOG.warn("SIS student-result access lookup failed", e);
            throw new UnavailableException("The SIS service could not verify student-result access.");
        }
    }
}
