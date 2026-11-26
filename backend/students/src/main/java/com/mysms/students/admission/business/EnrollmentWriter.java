package com.mysms.students.admission.business;

import com.mysms.students.admission.domain.ApplicationEntity;
import com.mysms.students.admission.domain.ApplicationGuardianEntity;
import com.mysms.students.admission.integration.PeopleGrpcClient;
import com.mysms.students.admission.integration.SisGrpcClient;
import com.mysms.common.v1.PageableRequest;
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
import com.mysms.sis.v1.CreateGuardianRequest;
import com.mysms.sis.v1.CreatePortalAccountRequest;
import com.mysms.sis.v1.CreateProgramEnrollmentRequest;
import com.mysms.sis.v1.CreateStudentEnrollmentRequest;
import com.mysms.sis.v1.CreateStudentGuardianRequest;
import com.mysms.sis.v1.CreateStudentPortalAccountRequest;
import com.mysms.sis.v1.CreateStudentRequest;
import com.mysms.sis.v1.GetStudentRequest;
import com.mysms.sis.v1.Guardian;
import com.mysms.sis.v1.ListGuardiansRequest;
import com.mysms.sis.v1.ListProgramEnrollmentsRequest;
import com.mysms.sis.v1.ListRelationshipTypesRequest;
import com.mysms.sis.v1.ListStudentEnrollmentsRequest;
import com.mysms.sis.v1.ListStudentGuardiansRequest;
import com.mysms.sis.v1.ListStudentsRequest;
import com.mysms.sis.v1.ProgramEnrollment;
import com.mysms.sis.v1.RelationshipType;
import com.mysms.sis.v1.Student;
import com.mysms.sis.v1.StudentEnrollment;
import com.mysms.sis.v1.StudentGuardian;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * Adapter over the people and sis gRPC clients for enrolment: reads and writes the applicant's person, the student, the
 * placement, the guardians and their links, and the portal accounts. Every call carries the caller's own identity, so
 * only someone who may create those records enrols. <p>Every create carries a {@code request_id} derived from the
 * application and the step, so a repeated enrolment gets the first record back. Answers map as: 404 on a read is "not
 * there"; 403, 409 and 422 keep their meaning; anything else, or an unreachable service, is 503. Their messages never
 * reach the client.
 */
@Component
public class EnrollmentWriter {

    /** The most rows one of the small lists (a student's guardians, placements, relationship types) is read with. */
    static final int PAGE_SIZE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(EnrollmentWriter.class);
    private static final String NEGATIVE = "-";

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

    private final PeopleGrpcClient people;
    private final SisGrpcClient sis;

    public EnrollmentWriter(PeopleGrpcClient people, SisGrpcClient sis) {
        this.people = people;
        this.sis = sis;
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
        return uuid(created("people", "people.person.create", "application", () -> people.createPerson(
                CreatePersonRequest.newBuilder().setPerson(person).setRequestId(key.toString()).build())
                .getPerson().getId()));
    }

    /** The student, as far as the caller can see it. */
    public Optional<StudentRef> findStudent(UUID studentId) {
        return read("sis", "sis.student.read", () -> sis.getStudent(GetStudentRequest.newBuilder()
                .setId(studentId.toString()).build()).getStudent()).map(EnrollmentWriter::ref);
    }

    /** The student a person already is in the institution, if any (one person is one student per institution). */
    public Optional<StudentRef> findStudentOfPerson(UUID personId, UUID institutionId) {
        String filter = "personId = \"" + personId + "\" AND institutionId = \"" + institutionId + "\"";
        List<Student> found = listed("sis", "sis.student.read", () -> sis.listStudents(ListStudentsRequest.newBuilder()
                .setFilter(filter).setPageable(page(1)).build()).getItemsList());
        return found.stream().findFirst().map(EnrollmentWriter::ref);
    }

    /** Creates the student of the institution for the person; the application number becomes the admission number. */
    public StudentRef createStudent(UUID personId, ApplicationEntity application, LocalDate admissionDate, UUID key) {
        Student.Builder student = Student.newBuilder().setPersonId(personId.toString())
                .setInstitutionId(application.getInstitutionId().toString());
        set(application.getApplicationNumber(), student::setAdmissionNumber);
        set(admissionDate, date -> student.setAdmissionDate(date.toString()));
        return ref(created("sis", "sis.student.create", "application", () -> sis.createStudent(CreateStudentRequest
                .newBuilder().setStudent(student).setRequestId(key.toString()).build()).getStudent()));
    }

    /** Whether the student already has a class placement in the academic year. */
    public boolean placedInYear(UUID studentId, UUID academicYearId) {
        String filter = "studentId = \"" + studentId + "\" AND academicYearId = \"" + academicYearId + "\"";
        return !listed("sis", "sis.student_enrollment.read", () -> sis.listStudentEnrollments(
                ListStudentEnrollmentsRequest.newBuilder().setFilter(filter).setPageable(page(1)).build())
                .getItemsList()).isEmpty();
    }

    /** Places the student in a class section for the academic year (K-12). */
    public void placeInClass(UUID studentId, UUID academicYearId, UUID classSectionId, String rollNumber,
            LocalDate enrolledOn, UUID key) {
        StudentEnrollment.Builder placement = StudentEnrollment.newBuilder().setStudentId(studentId.toString())
                .setAcademicYearId(academicYearId.toString());
        set(classSectionId, id -> placement.setClassSectionId(id.toString()));
        set(rollNumber, placement::setRollNumber);
        set(enrolledOn, date -> placement.setEnrolledOn(date.toString()));
        created("sis", "sis.student_enrollment.create", "classSectionId", () -> sis.createStudentEnrollment(
                CreateStudentEnrollmentRequest.newBuilder().setStudentEnrollment(placement)
                        .setRequestId(key.toString()).build()));
    }

    /** Whether the student is already enrolled in the course. */
    public boolean enrolledInCourse(UUID studentId, UUID courseId) {
        String filter = "studentId = \"" + studentId + "\" AND courseId = \"" + courseId + "\"";
        return !listed("sis", "sis.program_enrollment.read", () -> sis.listProgramEnrollments(
                ListProgramEnrollmentsRequest.newBuilder().setFilter(filter).setPageable(page(1)).build())
                .getItemsList()).isEmpty();
    }

    /** Enrols the student in a course and batch (higher education). */
    public void enrollInCourse(UUID studentId, UUID courseId, UUID batchId, UUID academicYearId, String rollNumber,
            UUID key) {
        ProgramEnrollment.Builder placement = ProgramEnrollment.newBuilder().setStudentId(studentId.toString())
                .setCourseId(courseId.toString());
        set(batchId, id -> placement.setBatchId(id.toString()));
        set(academicYearId, id -> placement.setAcademicYearId(id.toString()));
        set(rollNumber, placement::setRollNumber);
        created("sis", "sis.program_enrollment.create", "batchId", () -> sis.createProgramEnrollment(
                CreateProgramEnrollmentRequest.newBuilder().setProgramEnrollment(placement)
                        .setRequestId(key.toString()).build()));
    }

    /** The guardians linked to the student. */
    public List<UUID> guardianIds(UUID studentId) {
        List<StudentGuardian> links = listed("sis", "sis.student_guardian.read", () -> sis.listStudentGuardians(
                ListStudentGuardiansRequest.newBuilder().setStudentId(studentId.toString())
                        .setPageable(page(PAGE_SIZE)).build()).getItemsList());
        return links.stream().map(StudentGuardian::getGuardianId).filter(id -> !id.isEmpty())
                .map(EnrollmentWriter::uuid).toList();
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
        List<Guardian> rows = listed("sis", "sis.guardian.read", () -> sis.listGuardians(ListGuardiansRequest
                .newBuilder().setFilter(anyOf(linked)).setPageable(page(PAGE_SIZE)).build()).getItemsList());
        List<UUID> persons = rows.stream().map(Guardian::getPersonId).filter(id -> !id.isEmpty())
                .map(EnrollmentWriter::uuid).toList();
        if (persons.isEmpty()) {
            return Set.of();
        }
        List<Person> named = listed("people", "people.person.read", () -> people.listPersons(ListPersonsRequest
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
            List<Guardian> found = listed("sis", "sis.guardian.read", () -> sis.listGuardians(ListGuardiansRequest
                    .newBuilder().setFilter(filter).setPageable(page(1)).build()).getItemsList());
            Optional<UUID> guardian = found.stream().map(Guardian::getId).filter(id -> !id.isEmpty())
                    .map(EnrollmentWriter::uuid).findFirst();
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
        List<RelationshipType> types = listed("sis", "sis.relationship_type.read", () -> sis.listRelationshipTypes(
                ListRelationshipTypesRequest.newBuilder().setPageable(page(PAGE_SIZE)).build()).getItemsList());
        Map<String, UUID> byName = new HashMap<>();
        for (RelationshipType type : types) {
            if (type.getId().isEmpty()) {
                continue;
            }
            UUID typeId = uuid(type.getId());
            if (!type.getName().isEmpty()) {
                byName.putIfAbsent(nameKey(type.getName()), typeId);
            }
            if (type.hasCode()) {
                byName.put(nameKey(type.getCode()), typeId);
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
        String personId = created("people", "people.person.create", "guardians", () -> people.createPerson(
                CreatePersonRequest.newBuilder().setPerson(person).setRequestId(personKey.toString()).build())
                .getPerson().getId());
        Guardian.Builder guardian = Guardian.newBuilder().setPersonId(personId);
        set(source.getOccupation(), guardian::setSubOccupation);
        set(source.getDesignation(), guardian::setDesignation);
        set(source.getOrganisation(), guardian::setOrganizationName);
        set(WireValues.decimal(source.getAnnualIncome()), guardian::setAnnualIncome);
        set(source.getQualificationId(), id -> guardian.setQualificationId(id.toString()));
        set(source.getMobile(), guardian::setMobile);
        set(source.getEmail(), guardian::setEmail);
        return uuid(created("sis", "sis.guardian.create", "guardians", () -> sis.createGuardian(CreateGuardianRequest
                .newBuilder().setGuardian(guardian).setRequestId(guardianKey.toString()).build()).getGuardian()
                .getId()));
    }

    /** Links a guardian to the student, as the given relationship and, for the first, as the first contact. */
    public void linkGuardian(UUID studentId, UUID guardianId, UUID relationshipTypeId, boolean primaryContact,
            UUID key) {
        StudentGuardian.Builder link = StudentGuardian.newBuilder().setGuardianId(guardianId.toString())
                .setIsPrimaryContact(primaryContact);
        set(relationshipTypeId, id -> link.setRelationshipTypeId(id.toString()));
        created("sis", "sis.student_guardian.create", "guardians", () -> sis.createStudentGuardian(
                CreateStudentGuardianRequest.newBuilder().setStudentId(studentId.toString()).setStudentGuardian(link)
                        .setRequestId(key.toString()).build()));
    }

    /** Creates the student's login account (no invitation: message delivery is not built, and sis refuses one). */
    public void createStudentPortalAccount(UUID studentId, UUID key) {
        created("sis", "sis.student.create_student_portal_account", "createPortalAccounts",
                () -> sis.createStudentPortalAccount(CreateStudentPortalAccountRequest.newBuilder()
                        .setStudentId(studentId.toString()).setSendInvite(false).setRequestId(key.toString()).build()));
    }

    /** Creates a guardian's login account (no invitation, as for the student). */
    public void createGuardianPortalAccount(UUID guardianId, UUID key) {
        created("sis", "sis.guardian.create_portal_account", "createPortalAccounts",
                () -> sis.createPortalAccount(CreatePortalAccountRequest.newBuilder()
                        .setGuardianId(guardianId.toString()).setSendInvite(false).setRequestId(key.toString()).build()));
    }

    private static StudentRef ref(Student student) {
        return new StudentRef(uuid(student.getId()), student.getInstitutionId().isEmpty() ? null
                : uuid(student.getInstitutionId()), student.getPersonId().isEmpty() ? null : uuid(student.getPersonId()));
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

    /** A read by id: the record, or empty when the other module answers 404. */
    private static <T> Optional<T> read(String service, String permission, Supplier<T> request) {
        try {
            return Optional.of(request.get());
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw refused(service, permission, null, e);
        }
    }

    /** A list read: its rows. */
    private static <T> T listed(String service, String permission, Supplier<T> request) {
        return answered(service, permission, null, request);
    }

    /** A create: what the other module stored. */
    private static <T> T created(String service, String permission, String field, Supplier<T> request) {
        return answered(service, permission, field, request);
    }

    private static <T> T answered(String service, String permission, String field, Supplier<T> request) {
        try {
            return request.get();
        } catch (IntegrationGrpcClientException e) {
            throw refused(service, permission, field, e);
        }
    }

    /**
     * The other module's refusal as this operation's error.
     *
     * @param field the request field a refused write is reported under when the other module names none (null for a
     *              read, whose 409 and 422 are the other service failing)
     */
    private static RuntimeException refused(String service, String permission, String field,
            IntegrationGrpcClientException e) {
        int status = e.httpStatus();
        if (status == HttpStatus.FORBIDDEN.value()) {
            return new PermissionDeniedException("Enrolling an applicant also needs " + permission + ".");
        }
        if (status == HttpStatus.NOT_IMPLEMENTED.value()) {
            return new NotServedException(service);
        }
        if (status == HttpStatus.CONFLICT.value() && field != null) {
            return new DuplicateResourceException("The " + service + " service refused the record: it conflicts with "
                    + "one that exists (" + permission + ").");
        }
        if (status == HttpStatus.UNPROCESSABLE_CONTENT.value() && field != null) {
            Set<String> fields = new LinkedHashSet<>();
            e.calleeErrors().forEach(error -> fields.add(error.field()));
            if (fields.isEmpty()) {
                fields.add(field);
            }
            return new ValidationFailedException(fields.stream()
                    .map(name -> new FieldError(name, "was refused by the " + service + " service", "REFUSED"))
                    .toList());
        }
        LOG.warn("the {} service answered {} ({}) to enrolment ({})", service, status, e.statusCode(), permission);
        return new UnavailableException("The " + service + " service could not answer.");
    }
}
