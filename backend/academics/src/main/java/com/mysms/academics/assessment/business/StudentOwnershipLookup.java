package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.integration.IamGrpcClient;
import com.mysms.academics.assessment.integration.SisGrpcClient;
import com.mysms.authclient.AuthContext;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.UnavailableException;
import com.mysms.iam.v1.GetMeRequest;
import com.mysms.iam.v1.Me;
import com.mysms.sis.v1.ChildCard;
import com.mysms.sis.v1.GetStudentSummaryRequest;
import com.mysms.sis.v1.ListMyChildrenRequest;
import com.mysms.sis.v1.ListMyChildrenResponse;
import com.mysms.sis.v1.Student;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Which students' records are the caller's own (an Adapter over sis and iam, asked with the caller's identity): the
 * student themself or a child of the caller. sis decides, through its owner-aware operations only —
 * sis.GetStudentSummary shows a student to the student, their guardians and in-scope staff, so the student is the
 * caller's own when its account or person is the caller's; sis.ListMyChildren lists exactly the caller's children.
 * iam.GetMe names the student records linked to the caller's account, the candidates sis then confirms.
 *
 * <p>When sis (or, for {@link #ownStudents}, iam) cannot answer, the check is 503: the answer would otherwise be a
 * guess about personal data.
 */
@Component
public class StudentOwnershipLookup {

    /** How the caller relates to a student they own. */
    public enum Relation { SELF, GUARDIAN }

    private static final String SIS = "sis";
    private static final String IAM = "iam";
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private final SisGrpcClient sis;
    private final IamGrpcClient iam;

    public StudentOwnershipLookup(SisGrpcClient sis, IamGrpcClient iam) {
        this.sis = sis;
        this.iam = iam;
    }

    /** The caller's relation to the student; empty when the student is not theirs (or not shown to them). */
    public Optional<Relation> relationTo(AuthContext caller, UUID studentId) {
        if (isSelf(caller, studentId)) {
            return Optional.of(Relation.SELF);
        }
        return children().contains(studentId) ? Optional.of(Relation.GUARDIAN) : Optional.empty();
    }

    /** Every student the caller owns: their own student records and their children. */
    public Set<UUID> ownStudents(AuthContext caller) {
        Set<UUID> own = new LinkedHashSet<>(children());
        Optional<Me> me = Downstream.ownerCheck(IAM, () -> iam.getMe(GetMeRequest.getDefaultInstance()).getResult());
        List<String> candidates = me.<List<String>>map(Me::getStudentIdsList).orElse(List.of());
        for (UUID candidate : candidates.stream().map(UUID::fromString).toList()) {
            if (!own.contains(candidate) && isSelf(caller, candidate)) {
                own.add(candidate);
            }
        }
        return own;
    }

    /** sis shows the student to the caller, and the student's account or person is the caller's. */
    private boolean isSelf(AuthContext caller, UUID studentId) {
        Optional<Student> student = Downstream.ownerCheck(SIS, () -> sis.getStudentSummary(GetStudentSummaryRequest
                .newBuilder().setStudentId(studentId.toString()).build()).getResult().getStudent());
        if (student.isEmpty() || !studentId.toString().equals(student.get().getId())) {
            return false;
        }
        return caller.userId().toString().equals(student.get().getUserId())
                || caller.personId().map(person -> person.toString().equals(student.get().getPersonId())).orElse(false);
    }

    /** The caller's children as sis lists them; none when the caller is no guardian (403). */
    private Set<UUID> children() {
        Set<UUID> children = new LinkedHashSet<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = PageableRequest.newBuilder().setPage(page).setSize(PAGE_SIZE).setCursor(cursor)
                    .build();
            Optional<ListMyChildrenResponse> found = Downstream.ownerCheck(SIS,
                    () -> sis.listMyChildren(ListMyChildrenRequest.newBuilder().setPageable(pageable).build()));
            if (found.isEmpty()) {
                return children;
            }
            for (ChildCard child : found.get().getItemsList()) {
                if (!child.getStudent().getId().isEmpty()) {
                    children.add(UUID.fromString(child.getStudent().getId()));
                }
            }
            cursor = found.get().getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return children;
            }
        }
        throw new UnavailableException("The sis service listed more children than a guardian can have.");
    }
}
