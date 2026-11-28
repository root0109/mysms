package com.mysms.students.admission.business;

import com.mysms.authclient.SiblingAccess;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.db.InProcessIdempotency;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.core.mapping.WireValues;
import com.mysms.people.v1.CreatePersonRequest;
import com.mysms.people.v1.ListPersonsRequest;
import com.mysms.people.v1.Person;
import com.mysms.students.admission.domain.ApplicationEntity;
import com.mysms.students.admission.domain.ApplicationGuardianEntity;
import com.mysms.students.admission.integration.PeopleGrpcClient;
import com.mysms.students.sis.api.model.CreatePortalAccountRequest;
import com.mysms.students.sis.api.model.CreateStudentPortalAccountRequest;
import com.mysms.students.sis.api.model.Guardian;
import com.mysms.students.sis.api.model.ProgramEnrollment;
import com.mysms.students.sis.api.model.RelationshipType;
import com.mysms.students.sis.api.model.Student;
import com.mysms.students.sis.api.model.StudentEnrollment;
import com.mysms.students.sis.api.model.StudentGuardian;
import com.mysms.students.sis.business.GuardianService;
import com.mysms.students.sis.business.PortalAccountService;
import com.mysms.students.sis.business.ProgramEnrollmentService;
import com.mysms.students.sis.business.RelationshipTypeService;
import com.mysms.students.sis.business.StudentEnrollmentService;
import com.mysms.students.sis.business.StudentService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Adapter for enrolment over the people gRPC client and the sibling sis area's business services (in process): reads
 * and writes the applicant's person, the student, the placement, the guardians and their links, and the portal
 * accounts. Every call runs as the caller — people's with the caller's token, sis's with the caller's own permission
 * for the sis operation and sis's scope — so only someone who may create those records enrols. <p>Every create
 * carries a {@code request_id} derived from the application and the step (sis's through {@link InProcessIdempotency},
 * in sis's own store), so a repeated enrolment gets the first record back. Answers map as: 404 on a read is "not
 * there"; 403, 409 and 422 keep their meaning; anything else, or an unreachable service, is 503. Their messages never
 * reach the client.
 */
@Component
public class EnrollmentWriter {

    /** The most rows one of the small lists (a student's guardians, placements, relationship types) is read with. */
    static final int PAGE_SIZE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(EnrollmentWriter.class);
    private static final String NEGATIVE = "-";
    private static final String PEOPLE = "people";
    private static final String SIS = "sis";
    private static final String GUARDIANS = "guardians";
    private static final String PORTAL_ACCOUNTS = "createPortalAccounts";

    /** A student as enrolment needs to know it. */
    public record StudentRef(UUID id, UUID institutionId, UUID personId) {
    }

    /**
     * The other module answered that it does not serve the operation yet (501): to this operation's caller the
     * dependency is unavailable (503), and the portal-account operation records it as not implemented.
     */
    public static final class NotServedException extends ApiException {

        private static final long serialVersionUID = 1L;

        NotServedException(String service) {
            super(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE", "The " + service + " service does not serve a step "
                    + "of enrolment yet.");
        }
    }

    /** The sibling sis area's business services enrolment reads and writes through. */
    private record Sis(StudentService students, StudentEnrollmentService studentEnrollments,
                      ProgramEnrollmentService programEnrollments, GuardianService guardians,
                      RelationshipTypeService relationshipTypes, PortalAccountService portalAccounts) {
    }

    private final PeopleGrpcClient people;
    private final Sis sis;
    private final SiblingAccess sibling;
    private final InProcessIdempotency idempotency;

    public EnrollmentWriter(PeopleGrpcClient people, StudentService students,
            StudentEnrollmentService studentEnrollments, ProgramEnrollmentService programEnrollments,
            GuardianService guardians, RelationshipTypeService relationshipTypes, PortalAccountService portalAccounts,
            SiblingAccess sibling, InProcessIdempotency idempotency) {
        this.people = people;
        this.sis = new Sis(students, studentEnrollments, programEnrollments, guardians, relationshipTypes,
                portalAccounts);
        this.sibling = sibling;
        this.idempotency = idempotency;
    }

    /** Creates the person the applicant becomes, from what the application says about them. */
    public UUID createApplicantPerson(ApplicationEntity application, UUID key) {
        Person.Builder person = Person.newBuilder().setDisplayName(application.getFullName());
        set(application.getHonorificId(), id -> person.setHonorificId(id.toString()));
        set(application.getFirstName(), person::setFirstName);
        set(application.getMiddleName(), person::setMiddleName);
        set(application.getLastName(), person::setLastName);
        set(application.getDateOfBirth(), date -> person.setDateOfBirth(date.toString()));
        gender(application.getGender()).ifPresent(person::setGender);
        bloodGroup(application.getBloodGroup()).ifPresent(person::setBloodGroup);
        set(application.getBirthPlace(), person::setBirthPlace);
        set(application.getMotherTongue(), person::setMotherTongue);
        set(application.getReligion(), person::setReligion);
        set(application.getSocialCategory(), person::setSocialCategory);
        set(application.getEmail(), person::setPrimaryEmail);
        set(application.getMobile() != null ? application.getMobile() : application.getPhone(), person::setPrimaryPhone);
        set(application.getMobile() != null ? application.getPhone() : null, person::setAlternatePhone);
        set(application.getPhotoFileId(), id -> person.setPhotoFileId(id.toString()));
        return uuid(fromPeople("people.person.create", "application", () -> people.createPerson(
                CreatePersonRequest.newBuilder().setPerson(person).setRequestId(key.toString()).build())
                .getPerson().getId()));
    }

    /** The student, as far as the caller can see it. */
    public Optional<StudentRef> findStudent(UUID studentId) {
        return sisRead("sis.student.read", () -> sis.students().getStudent(studentId)).map(EnrollmentWriter::ref);
    }

    /** The student a person already is in the institution, if any (one person is one student per institution). */
    public Optional<StudentRef> findStudentOfPerson(UUID personId, UUID institutionId) {
        String filter = "personId = \"" + personId + "\" AND institutionId = \"" + institutionId + "\"";
        List<Student> found = fromSis("sis.student.read", null,
                () -> sis.students().listStudents(1, 1, null, filter, null).items());
        return found.stream().findFirst().map(EnrollmentWriter::ref);
    }

    /** Creates the student of the institution for the person; the application number becomes the admission number. */
    public StudentRef createStudent(UUID personId, ApplicationEntity application, LocalDate admissionDate, UUID key) {
        Student student = new Student(personId, application.getInstitutionId())
                .admissionNumber(application.getApplicationNumber()).admissionDate(admissionDate);
        return ref(fromSis("sis.student.create", "application", () -> idempotency.call("sis.CreateStudent", key,
                student, Student.class, () -> sis.students().createStudent(student, false))));
    }

    /** Whether the student already has a class placement in the academic year. */
    public boolean placedInYear(UUID studentId, UUID academicYearId) {
        String filter = "studentId = \"" + studentId + "\" AND academicYearId = \"" + academicYearId + "\"";
        return !fromSis("sis.student_enrollment.read", null, () -> sis.studentEnrollments()
                .listStudentEnrollments(1, 1, null, filter, null).items()).isEmpty();
    }

    /** Places the student in a class section for the academic year (K-12). */
    public void placeInClass(UUID studentId, UUID academicYearId, UUID classSectionId, String rollNumber,
            LocalDate enrolledOn, UUID key) {
        StudentEnrollment placement = new StudentEnrollment(studentId, academicYearId).classSectionId(classSectionId)
                .rollNumber(rollNumber).enrolledOn(enrolledOn);
        fromSis("sis.student_enrollment.create", "classSectionId", () -> idempotency.call(
                "sis.CreateStudentEnrollment", key, placement, StudentEnrollment.class,
                () -> sis.studentEnrollments().createStudentEnrollment(placement, false)));
    }

    /** Whether the student is already enrolled in the course. */
    public boolean enrolledInCourse(UUID studentId, UUID courseId) {
        String filter = "studentId = \"" + studentId + "\" AND courseId = \"" + courseId + "\"";
        return !fromSis("sis.program_enrollment.read", null, () -> sis.programEnrollments()
                .listProgramEnrollments(1, 1, null, filter, null).items()).isEmpty();
    }

    /** Enrols the student in a course and batch (higher education). */
    public void enrollInCourse(UUID studentId, UUID courseId, UUID batchId, UUID academicYearId, String rollNumber,
            UUID key) {
        ProgramEnrollment placement = new ProgramEnrollment(studentId, courseId).batchId(batchId)
                .academicYearId(academicYearId).rollNumber(rollNumber);
        fromSis("sis.program_enrollment.create", "batchId", () -> idempotency.call("sis.CreateProgramEnrollment", key,
                placement, ProgramEnrollment.class,
                () -> sis.programEnrollments().createProgramEnrollment(placement, false)));
    }

    /** The guardians linked to the student. */
    public List<UUID> guardianIds(UUID studentId) {
        List<StudentGuardian> links = fromSis("sis.student_guardian.read", null, () -> sis.students()
                .listStudentGuardians(studentId, 1, PAGE_SIZE, null, null, null).items());
        return links.stream().map(StudentGuardian::getGuardianId).filter(Objects::nonNull).toList();
    }

    /**
     * The names (lower case) of the people who are guardians of the student already, given the guardians linked to
     * them: how a repeated enrolment recognises a guardian of the application it made before when the application
     * gives no mobile or e-mail to look the guardian up by.
     */
    public Set<String> guardianNames(List<UUID> linked) {
        if (linked.isEmpty()) {
            return Set.of();
        }
        List<Guardian> rows = fromSis("sis.guardian.read", null, () -> sis.guardians()
                .listGuardians(1, PAGE_SIZE, null, anyOf(linked), null).items());
        List<UUID> persons = rows.stream().map(Guardian::getPersonId).filter(Objects::nonNull).toList();
        if (persons.isEmpty()) {
            return Set.of();
        }
        List<Person> named = fromPeople("people.person.read", null, () -> people.listPersons(ListPersonsRequest
                .newBuilder().setFilter(anyOf(persons)).setPageable(page(PAGE_SIZE)).build()).getItemsList());
        return named.stream().map(Person::getDisplayName).filter(name -> !name.isEmpty())
                .map(EnrollmentWriter::nameKey).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** How a guardian's name is compared: trimmed, lower case. */
    public static String nameKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * The guardian sis already has for this parent, found by e-mail first and then by mobile (a parent of a sibling
     * is one guardian of both); empty when the application gives neither, or sis knows neither.
     */
    public Optional<UUID> findGuardian(String email, String mobile) {
        for (String filter : new String[] {contactFilter("email", email), contactFilter("mobile", mobile)}) {
            if (filter == null) {
                continue;
            }
            List<Guardian> found = fromSis("sis.guardian.read", null,
                    () -> sis.guardians().listGuardians(1, 1, null, filter, null).items());
            Optional<UUID> guardian = found.stream().map(Guardian::getId).filter(Objects::nonNull).findFirst();
            if (guardian.isPresent()) {
                return guardian;
            }
        }
        return Optional.empty();
    }

    /**
     * The school's relationship types, by lower-case code and name (read once per enrolment): how father, mother
     * and guardian are named in sis.
     */
    public Map<String, UUID> relationshipTypes() {
        List<RelationshipType> types = fromSis("sis.relationship_type.read", null, () -> sis.relationshipTypes()
                .listRelationshipTypes(1, PAGE_SIZE, null, null, null).items());
        Map<String, UUID> byName = new HashMap<>();
        for (RelationshipType type : types) {
            UUID typeId = type.getId();
            if (typeId == null) {
                continue;
            }
            String name = type.getName();
            if (name != null && !name.isEmpty()) {
                byName.putIfAbsent(nameKey(name), typeId);
            }
            String code = type.getCode();
            if (code != null) {
                byName.put(nameKey(code), typeId);
            }
        }
        return byName;
    }

    /** Creates the guardian of one guardian of the application: their person, then their sis guardian record. */
    public UUID createGuardian(ApplicationGuardianEntity source, UUID personKey, UUID guardianKey) {
        Person.Builder person = Person.newBuilder().setDisplayName(source.getName());
        set(source.getMobile(), person::setPrimaryPhone);
        set(source.getEmail(), person::setPrimaryEmail);
        set(source.getPhotoFileId(), id -> person.setPhotoFileId(id.toString()));
        String personId = fromPeople("people.person.create", GUARDIANS, () -> people.createPerson(
                CreatePersonRequest.newBuilder().setPerson(person).setRequestId(personKey.toString()).build())
                .getPerson().getId());
        Guardian guardian = new Guardian(uuid(personId)).subOccupation(source.getOccupation())
                .designation(source.getDesignation()).organizationName(source.getOrganisation())
                .annualIncome(source.getAnnualIncome() == null ? null : WireValues.decimal(source.getAnnualIncome()))
                .qualificationId(source.getQualificationId()).mobile(source.getMobile()).email(source.getEmail());
        return fromSis("sis.guardian.create", GUARDIANS, () -> idempotency.call("sis.CreateGuardian", guardianKey,
                guardian, Guardian.class, () -> sis.guardians().createGuardian(guardian, false))).getId();
    }

    /** Links a guardian to the student, as the given relationship and, for the first, as the first contact. */
    public void linkGuardian(UUID studentId, UUID guardianId, UUID relationshipTypeId, boolean primaryContact,
            UUID key) {
        StudentGuardian link = new StudentGuardian(guardianId).relationshipTypeId(relationshipTypeId)
                .isPrimaryContact(primaryContact);
        fromSis("sis.student_guardian.create", GUARDIANS, () -> idempotency.call("sis.CreateStudentGuardian", key,
                keyed("studentId", studentId, link), StudentGuardian.class,
                () -> sis.students().createStudentGuardian(studentId, link, false)));
    }

    /** Creates the student's login account (no invitation: message delivery is not built, and sis refuses one). */
    public void createStudentPortalAccount(UUID studentId, UUID key) {
        CreateStudentPortalAccountRequest request = new CreateStudentPortalAccountRequest().sendInvite(false);
        fromSis("sis.student.create_student_portal_account", PORTAL_ACCOUNTS, () -> idempotency.call(
                "sis.CreateStudentPortalAccount", key, keyed("studentId", studentId, request), Student.class,
                () -> sis.portalAccounts().createStudentPortalAccount(studentId, request)));
    }

    /** Creates a guardian's login account (no invitation, as for the student). */
    public void createGuardianPortalAccount(UUID guardianId, UUID key) {
        CreatePortalAccountRequest request = new CreatePortalAccountRequest().sendInvite(false);
        fromSis("sis.guardian.create_portal_account", PORTAL_ACCOUNTS, () -> idempotency.call(
                "sis.CreatePortalAccount", key, keyed("guardianId", guardianId, request), Guardian.class,
                () -> sis.portalAccounts().createGuardianPortalAccount(guardianId, request)));
    }

    private static StudentRef ref(Student student) {
        return new StudentRef(student.getId(), student.getInstitutionId(), student.getPersonId());
    }

    /** What identifies a request on a path: its parent's id and its body, as REST and gRPC send them. */
    private static Map<String, Object> keyed(String name, UUID id, Object body) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put(name, id);
        request.put("body", body);
        return request;
    }

    /** {@code male} → GENDER_MALE; empty for no value or one people's contract does not know. */
    private static Optional<Person.Gender> gender(String value) {
        return enumValue(Person.Gender.class, "GENDER_", value);
    }

    /** {@code AB+} → BLOOD_GROUP_AB_POSITIVE, {@code O-} → BLOOD_GROUP_O_NEGATIVE (people's proto names). */
    private static Optional<Person.BloodGroup> bloodGroup(String value) {
        if (value == null || value.length() < 2) {
            return Optional.empty();
        }
        String sign = value.substring(value.length() - 1);
        String group = value.substring(0, value.length() - 1);
        return enumValue(Person.BloodGroup.class, "BLOOD_GROUP_",
                group + ("+".equals(sign) ? "_POSITIVE" : NEGATIVE.equals(sign) ? "_NEGATIVE" : "_" + sign));
    }

    private static <E extends Enum<E>> Optional<E> enumValue(Class<E> type, String prefix, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String name = prefix + value.trim().toUpperCase(Locale.ROOT);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(name)) {
                return Optional.of(constant);
            }
        }
        return Optional.empty();
    }

    /** Sets an optional proto field: proto builders take no null, an absent value is simply not sent. */
    private static <T> void set(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value);
    }

    private static PageableRequest page(int size) {
        return PageableRequest.newBuilder().setPage(1).setSize(size).build();
    }

    /** A filter on one contact field of a guardian; null without a value. Quotes cannot end the literal. */
    private static String contactFilter(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return field + " = \"" + value.trim().replace("\\", "").replace("\"", "") + "\"";
    }

    private static String anyOf(List<UUID> ids) {
        return ids.stream().map(id -> "id = \"" + id + "\"").collect(Collectors.joining(" OR "));
    }

    /** A sis read by id, in process: the record, or empty when sis answers 404 (not there for this caller). */
    private <T> Optional<T> sisRead(String permission, Supplier<T> request) {
        requireSis(permission);
        try {
            return Optional.of(request.get());
        } catch (ApiException e) {
            if (e.status() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw refused(SIS, permission, null, e.status().value(), e.code(), fieldsOf(e.errors()));
        }
    }

    /**
     * A sis list or create, in process: what sis answered. The caller needs the sis operation's permission, the check
     * the gRPC edge made; sis then applies the caller's scope.
     *
     * @param field the request field a refused write is reported under when sis names none (null for a read)
     */
    private <T> T fromSis(String permission, String field, Supplier<T> request) {
        requireSis(permission);
        try {
            return request.get();
        } catch (ApiException e) {
            throw refused(SIS, permission, field, e.status().value(), e.code(), fieldsOf(e.errors()));
        }
    }

    private void requireSis(String permission) {
        if (!sibling.permits(permission)) {
            throw denied(permission);
        }
    }

    /** A people call over gRPC: what people answered. */
    private static <T> T fromPeople(String permission, String field, Supplier<T> request) {
        try {
            return request.get();
        } catch (IntegrationGrpcClientException e) {
            throw refused(PEOPLE, permission, field, e.httpStatus(), e.statusCode(),
                    e.calleeErrors().stream().map(FieldError::field).toList());
        }
    }

    private static List<String> fieldsOf(List<FieldError> errors) {
        return errors.stream().map(FieldError::field).toList();
    }

    private static PermissionDeniedException denied(String permission) {
        return new PermissionDeniedException("Enrolling an applicant also needs " + permission + ".");
    }

    /**
     * The other module's refusal as this operation's error.
     *
     * @param field  the request field a refused write is reported under when the other module names none (null for a
     *               read, whose 409 and 422 are the other service failing)
     * @param fields the request fields the other module refused
     */
    private static RuntimeException refused(String service, String permission, String field, int status, Object code,
            List<String> fields) {
        if (status == HttpStatus.FORBIDDEN.value()) {
            return denied(permission);
        }
        if (status == HttpStatus.NOT_IMPLEMENTED.value()) {
            return new NotServedException(service);
        }
        if (status == HttpStatus.CONFLICT.value() && field != null) {
            return new DuplicateResourceException("The " + service + " service refused the record: it conflicts with "
                    + "one that exists (" + permission + ").");
        }
        if (status == HttpStatus.UNPROCESSABLE_CONTENT.value() && field != null) {
            Set<String> refusedFields = new LinkedHashSet<>(fields);
            if (refusedFields.isEmpty()) {
                refusedFields.add(field);
            }
            return new ValidationFailedException(refusedFields.stream()
                    .map(name -> new FieldError(name, "was refused by the " + service + " service", "REFUSED"))
                    .toList());
        }
        LOG.warn("the {} service answered {} ({}) to enrolment ({})", service, status, code, permission);
        return new UnavailableException("The " + service + " service could not answer.");
    }
}
