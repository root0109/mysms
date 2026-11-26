package com.mysms.students.sis.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.students.sis.api.model.ChildCard;
import com.mysms.students.sis.api.model.Person;
import com.mysms.students.sis.api.model.StudentSummary;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.domain.StudentEnrollmentEntity;
import com.mysms.students.sis.domain.StudentEntity;
import com.mysms.students.sis.mapper.ProgramEnrollmentMapper;
import com.mysms.students.sis.mapper.StudentEnrollmentMapper;
import com.mysms.students.sis.mapper.StudentGuardianMapper;
import com.mysms.students.sis.mapper.StudentMapper;
import com.mysms.students.sis.repository.DisciplineIncidentRepository;
import com.mysms.students.sis.repository.FamilyLinkRepository;
import com.mysms.students.sis.repository.ProgramEnrollmentRepository;
import com.mysms.students.sis.repository.StudentEnrollmentRepository;
import com.mysms.students.sis.repository.StudentGuardianRepository;
import com.mysms.students.sis.repository.StudentRepository;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The read models of a student that are not one table: sis.GetStudentSummary (the student 360° overview) and
 * sis.ListMyChildren (the signed-in guardian's children as cards).
 *
 * <p><b>Who sees what.</b> A student's own family — an active guardian link of the caller's account or person — and the
 * student themself (the record's account or person) read the summary under the operation's permission alone, wherever
 * the student goes to school; anyone else reads it as staff, which also needs sis.student.read and the student's
 * institution in their scope; to everyone else the student does not exist (404). The family and the student count only
 * the incidents the school shared with guardians; staff count all open ones. sis.ListMyChildren is the caller's own
 * data only: the students of their active guardian links, never filtered by institution scope and never anyone else's.
 *
 * <p><b>Other modules' parts</b> are read after sis's own transaction, with the caller's token, and each is optional
 * (left out when the service is not configured, cannot answer, or does not show it to this caller): the person from
 * people (without it the person carries the student's admission number as its name), the institution's name and the
 * class label. The summary's attendance, open loans and unpaid fees are left out: attendance, library and finance
 * depend on sis, so sis never calls them — the clients read those parts from their owners.
 */
@Service
public class StudentSummaryService extends AppTransactionManager {

    static final String PERMISSION_SUMMARY = "sis.student.get_student_summary";
    /** What staff need besides the summary permission: the ordinary read of student records. */
    static final String PERMISSION_STUDENT_READ = StudentService.PERMISSION_GET;

    /** What sis itself knows for a summary, read in one transaction. */
    private record Owned(StudentEntity student, StudentSummary summary) {
    }

    /** What sis itself knows for a page of child cards. */
    private record Family(Page<StudentEntity> students, Map<UUID, String> relationships,
                          Map<UUID, UUID> classSections) {
    }

    private final StudentRepository students;
    private final StudentEnrollmentRepository enrollments;
    private final ProgramEnrollmentRepository programEnrollments;
    private final StudentGuardianRepository guardianLinks;
    private final DisciplineIncidentRepository incidents;
    private final FamilyLinkRepository families;
    private final StudentMapper studentMapper;
    private final StudentEnrollmentMapper enrollmentMapper;
    private final ProgramEnrollmentMapper programEnrollmentMapper;
    private final StudentGuardianMapper guardianLinkMapper;
    private final StudentDetailsLookup details;
    private final AcademicsLookup academics;
    private final Clock clock;

    public StudentSummaryService(StudentRepository students, StudentEnrollmentRepository enrollments,
            ProgramEnrollmentRepository programEnrollments, StudentGuardianRepository guardianLinks,
            DisciplineIncidentRepository incidents, FamilyLinkRepository families, StudentMapper studentMapper,
            StudentEnrollmentMapper enrollmentMapper, ProgramEnrollmentMapper programEnrollmentMapper,
            StudentGuardianMapper guardianLinkMapper, StudentDetailsLookup details,
            AcademicsLookup academics,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.students = students;
        this.enrollments = enrollments;
        this.programEnrollments = programEnrollments;
        this.guardianLinks = guardianLinks;
        this.incidents = incidents;
        this.families = families;
        this.studentMapper = studentMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.programEnrollmentMapper = programEnrollmentMapper;
        this.guardianLinkMapper = guardianLinkMapper;
        this.details = details;
        this.academics = academics;
        this.clock = clock;
    }

    /**
     * sis.GetStudentSummary: the student, their person, the placement of the given year (else the open one, else
     * the latest), course registrations, guardian links, open incidents and the other modules' parts.
     */
    public StudentSummary getStudentSummary(UUID id, UUID academicYearId) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_SUMMARY);
        Owned owned = doInPrimaryReadTransaction(() -> {
            StudentEntity student = students.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(StudentService.AGGREGATE, id));
            boolean family = isOwnOrFamily(caller, student);
            if (!family && (!caller.hasPermission(PERMISSION_STUDENT_READ)
                    || !scope.canRead(Optional.ofNullable(student.getInstitutionId())))) {
                throw new ResourceNotFoundException(StudentService.AGGREGATE, id);
            }
            Optional<StudentEnrollmentEntity> placement =
                    enrollments.currentOfStudent(id, Optional.ofNullable(academicYearId));
            StudentSummary summary = new StudentSummary(studentMapper.toDto(student), null)
                    .currentEnrollment(placement.map(enrollmentMapper::toDto).orElse(null))
                    .programEnrollments(programEnrollments.ofStudent(id).stream()
                            .map(programEnrollmentMapper::toDto).toList())
                    .guardians(guardianLinks.ofStudent(id).stream().map(guardianLinkMapper::toDto).toList())
                    .openIncidents(incidents.countOpenOfStudent(id, family));
            return new Owned(student, summary);
        });
        StudentSummary summary = owned.summary();
        summary.setPerson(personOf(owned.student(), details.persons(List.of(owned.student().getPersonId()))));
        return summary;
    }

    /** sis.ListMyChildren: the caller's children, page by page in the order they were recorded. */
    public Page<ChildCard> listMyChildren(Integer pageNumber, Integer pageSize, String cursor) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(null, StudentRepository.SORT_FIELDS, StudentRepository.DEFAULT_ORDER,
                StudentRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, "children|" + caller.userId(), clock);
        Family family = doInPrimaryReadTransaction(() -> {
            Page<StudentEntity> rows =
                    students.list(page, FamilyLinkRepository.childrenOf(caller.userId(), caller.personId()));
            List<UUID> ids = rows.items().stream().map(StudentEntity::getId).toList();
            if (ids.isEmpty()) {
                return new Family(rows, Map.of(), Map.of());
            }
            Map<UUID, UUID> classes = new HashMap<>();
            for (StudentEnrollmentEntity placement : enrollments.openOfStudents(ids)) {
                if (placement.getClassSectionId() != null) {
                    classes.put(placement.getStudentId(), placement.getClassSectionId());
                }
            }
            return new Family(rows, families.relationships(ids, caller.userId(), caller.personId()), classes);
        });
        Map<UUID, Person> persons = details.persons(
                family.students().items().stream().map(StudentEntity::getPersonId).toList());
        Map<UUID, Optional<String>> institutionNames = new HashMap<>();
        Map<UUID, Optional<String>> classLabels = new HashMap<>();
        return family.students().map(student -> {
            UUID classSection = family.classSections().get(student.getId());
            return new ChildCard(studentMapper.toDto(student), personOf(student, persons))
                    .relationship(family.relationships().get(student.getId()))
                    .institutionName(institutionNames.computeIfAbsent(student.getInstitutionId(),
                            details::institutionName).orElse(null))
                    .classSectionLabel(classSection == null ? null
                            : classLabels.computeIfAbsent(classSection, academics::classLabel).orElse(null));
        });
    }

    /** The caller is the student themself, or an active guardian of the student. */
    private boolean isOwnOrFamily(AuthContext caller, StudentEntity student) {
        boolean self = caller.userId().equals(student.getUserId())
                || caller.personId().map(person -> person.equals(student.getPersonId())).orElse(false);
        return self || families.isGuardianOf(student.getId(), caller.userId(), caller.personId());
    }

    /** The person from people, or — when people does not show it — one named by the admission number. */
    private Person personOf(StudentEntity student, Map<UUID, Person> persons) {
        Person found = persons.get(student.getPersonId());
        if (found != null) {
            return found;
        }
        String name = student.getAdmissionNumber() == null ? student.getId().toString() : student.getAdmissionNumber();
        return new Person(name).id(student.getPersonId());
    }
}
