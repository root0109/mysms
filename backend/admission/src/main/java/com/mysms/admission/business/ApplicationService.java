package com.mysms.admission.business;

import com.mysms.admission.api.model.Application;
import com.mysms.admission.api.model.ApplicationEmergencyContact;
import com.mysms.admission.api.model.ApplicationEmergencyContactUpdate;
import com.mysms.admission.api.model.ApplicationGuardian;
import com.mysms.admission.api.model.ApplicationGuardianUpdate;
import com.mysms.admission.api.model.ApplicationUpdate;
import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.config.AdmissionEventsConfig;
import com.mysms.admission.domain.AdmissionCycleEntity;
import com.mysms.admission.domain.ApplicationEmergencyContactEntity;
import com.mysms.admission.domain.ApplicationEntity;
import com.mysms.admission.domain.ApplicationGuardianEntity;
import com.mysms.admission.events.AdmissionEvents;
import com.mysms.admission.mapper.ApplicationEmergencyContactMapper;
import com.mysms.admission.mapper.ApplicationGuardianMapper;
import com.mysms.admission.mapper.ApplicationMapper;
import com.mysms.admission.repository.AdmissionCycleRepository;
import com.mysms.admission.repository.ApplicationEmergencyContactRepository;
import com.mysms.admission.repository.ApplicationGuardianRepository;
import com.mysms.admission.repository.ApplicationRepository;
import com.mysms.admission.repository.NumberSequenceRepository;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of admission.application and of the records that exist only as part of a Application
 * (ApplicationEmergencyContact, ApplicationGuardian. <p>Who reaches an application is {@link ApplicationAccess}: the
 * office of its institution, and the applicant who started it in an admission cycle. The steps of its lifecycle are
 * {@link ApplicationWorkflowService}; turning an admitted applicant into a student is {@link
 * ApplicationEnrollmentService}): audit and domain event commit in the same transaction as every change. Rows outside
 * the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ApplicationService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = ApplicationEntity.TABLE;
    static final String PERMISSION_LIST = "admission.application.read";
    static final String PERMISSION_GET = "admission.application.read";
    static final String PERMISSION_CREATE = "admission.application.create";
    static final String PERMISSION_UPDATE = "admission.application.update";
    static final String PERMISSION_DELETE = "admission.application.delete";
    /** The contract's ApplicationUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "admissionCycleId", "applicationNumber", "applicationDate", "admissionDate",
            "academicYearId", "gradeLevelId", "sectionId", "programId", "courseId", "batchId", "applicantPersonId",
            "studentId", "isExistingStudent", "honorificId", "fullName", "firstName", "middleName", "lastName",
            "dateOfBirth", "gender", "birthPlace", "nationality", "religion", "socialCategory", "motherTongue",
            "identificationMarks", "nationalId", "bloodGroup", "heightCm", "weightKg", "photoFileId", "email", "phone",
            "mobile", "addressLine1", "addressLine2", "cityName", "postalCode", "stateProvinceId", "countryId",
            "permanentAddress", "communicationAddress", "previousInstitution", "previousCourse", "previousResult",
            "previousBoard", "boardRegistrationNumber", "reasonForLeaving", "languageChoice", "streamChoice",
            "hasSiblings", "hasMedicalHistory", "medicalDetails", "hasExtracurricular", "extracurricularDetails",
            "transportMode", "transportDetails", "familyBusiness", "familyIncome", "applicationFee", "discountPercent",
            "feePlanId", "feeStartDate", "paymentDueDate", "paymentStatus", "remarks", "paymentStatusNote");

    /**
     * What the admissions office decides, and an applicant therefore never sets: where the applicant is placed, who
     * they become, and everything about the fee.
     */
    private static final Map<String, Function<ApplicationEntity, Object>> OFFICE_FIELDS = officeFields();
    /** On an update an applicant also cannot move the application to another school, cycle or number. */
    private static final Set<String> OFFICE_FIELDS_OF_AN_UPDATE = officeFieldsOfAnUpdate();

    /** The states in which enrolment owns the person and the student of an application. */
    private static final Set<String> ENROLLING = Set.of("admitted", "done");
    /** What enrolment writes, and the standard update therefore leaves alone from admission on. */
    private static final Set<String> ENROLMENT_FIELDS = Set.of("applicantPersonId", "studentId");

    /** A filter term on one of the office's notes. */
    private static final Pattern OFFICE_NOTES = Pattern.compile("\\b(" + String.join("|",
            ApplicationRepository.OFFICE_NOTE_FIELDS) + ")\\b");
    /** The number series of application numbers in admission.number_sequence (per institution and year). */
    static final String NUMBER_SEQUENCE = "application";
    /** How many numbers of the series a self-service create tries before it gives up (office numbers in the way). */
    private static final int MAX_NUMBER_ATTEMPTS = 20;

    public static final String APPLICATION_EMERGENCY_CONTACT_AGGREGATE = ApplicationEmergencyContactEntity.TABLE;
    static final String APPLICATION_EMERGENCY_CONTACT_PERMISSION_LIST = "admission.application_emergency_contact.read";
    static final String APPLICATION_EMERGENCY_CONTACT_PERMISSION_GET = "admission.application_emergency_contact.read";
    static final String APPLICATION_EMERGENCY_CONTACT_PERMISSION_CREATE = "admission.application_emergency_contact.create";
    static final String APPLICATION_EMERGENCY_CONTACT_PERMISSION_UPDATE = "admission.application_emergency_contact.update";
    static final String APPLICATION_EMERGENCY_CONTACT_PERMISSION_DELETE = "admission.application_emergency_contact.delete";
    /** The contract's ApplicationEmergencyContactUpdate fields, in apply order. */
    static final List<String> APPLICATION_EMERGENCY_CONTACT_WRITABLE = List.of("priority", "contactNumber", "relationship");

    public static final String APPLICATION_GUARDIAN_AGGREGATE = ApplicationGuardianEntity.TABLE;
    static final String APPLICATION_GUARDIAN_PERMISSION_LIST = "admission.application_guardian.read";
    static final String APPLICATION_GUARDIAN_PERMISSION_GET = "admission.application_guardian.read";
    static final String APPLICATION_GUARDIAN_PERMISSION_CREATE = "admission.application_guardian.create";
    static final String APPLICATION_GUARDIAN_PERMISSION_UPDATE = "admission.application_guardian.update";
    static final String APPLICATION_GUARDIAN_PERMISSION_DELETE = "admission.application_guardian.delete";
    /** The contract's ApplicationGuardianUpdate fields, in apply order. */
    static final List<String> APPLICATION_GUARDIAN_WRITABLE =
            List.of("relationship", "name", "qualificationId", "designation", "occupation", "organisation",
                    "annualIncome",
            "mobile", "email", "nationalId", "photoFileId");

    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> APPLICATION_GUARDIAN_SECRETS = Set.of("nationalId");
    private static final String APPLICATION_GUARDIAN_SECRET_NATIONAL_ID_ENC = "admission.application_guardian.national_id_enc";

    private final ApplicationRepository repository;
    private final ApplicationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("nationalId");
    private static final String SECRET_NATIONAL_ID_ENC = "admission.application.national_id_enc";
    private final FieldCipher cipher;
    private final ApplicationEmergencyContactRepository applicationEmergencyContactRepository;
    private final ApplicationEmergencyContactMapper applicationEmergencyContactMapper;
    private final ApplicationGuardianRepository applicationGuardianRepository;
    private final ApplicationGuardianMapper applicationGuardianMapper;
    private final AdmissionCycleRepository admissionCycleRepository;
    private final NumberSequenceRepository numbers;

    public ApplicationService(ApplicationRepository repository,
            ApplicationMapper mapper,
            @Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AdmissionDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AdmissionEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AdmissionEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            FieldCipher cipher,
            ApplicationEmergencyContactRepository applicationEmergencyContactRepository,
            ApplicationEmergencyContactMapper applicationEmergencyContactMapper,
            ApplicationGuardianRepository applicationGuardianRepository,
            ApplicationGuardianMapper applicationGuardianMapper,
            AdmissionCycleRepository admissionCycleRepository,
            NumberSequenceRepository numbers) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.cipher = cipher;
        this.applicationEmergencyContactRepository = applicationEmergencyContactRepository;
        this.applicationEmergencyContactMapper = applicationEmergencyContactMapper;
        this.applicationGuardianRepository = applicationGuardianRepository;
        this.applicationGuardianMapper = applicationGuardianMapper;
        this.admissionCycleRepository = admissionCycleRepository;
        this.numbers = numbers;
    }

    /** Refuses to start without a master key: this resource stores secret columns. */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public Application getApplication(UUID id) {
        ApplicationAccess access = ApplicationAccess.current(PERMISSION_GET);
        return doInReadTransaction(() -> {
            return dtoFor(access, loadVisible(access, id));
        });
    }

    public Page<Application> listApplications(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ApplicationRepository.SORT_FIELDS, ApplicationRepository.DEFAULT_ORDER,
                ApplicationRepository.ID_COLUMN);
        ApplicationAccess access = ApplicationAccess.current(PERMISSION_LIST);
        SqlFragment where;
        if (access.applicantOnly()) {
            // the office's notes are no filter field for an applicant: 400 INVALID_FILTER, like any unknown field
            where = FilterParser.parse(filter, ApplicationRepository.APPLICANT_FILTER_FIELDS).and(access.listFilter());
        } else {
            // a filter on the office's notes reads only the office's rows, never the caller's own applications elsewhere
            where = FilterParser.parse(filter, ApplicationRepository.FILTER_FIELDS)
                    .and(mentionsOfficeNotes(filter) ? access.officeFilter() : access.listFilter());
        }
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ApplicationEntity> rows = repository.list(page, where);
            return rows.map(row -> dtoFor(access, row));
        });
    }

    /** Whether a filter names one of the office's notes (a quoted value that merely contains the word counts too). */
    private static boolean mentionsOfficeNotes(String filter) {
        return filter != null && OFFICE_NOTES.matcher(filter).find();
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Application createApplication(Application request, boolean validateOnly) {
        validator.validate(request);
        ApplicationEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealNationalId(entity, request.getNationalId());
        ApplicationAccess access = ApplicationAccess.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            Optional<AdmissionCycleEntity> selfService = requireCreatable(access, entity);
            if (selfService.isPresent() && validateOnly) {
                return dtoFor(access, entity);
            }
            selfService.ifPresent(cycle -> number(entity, cycle));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            ApplicationEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AdmissionEvents.CREATED, AGGREGATE, stored.getId(),
                    AdmissionEvents.payload(stored.getId()));
            return dtoFor(access, stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Application updateApplication(UUID id, String ifMatch, ApplicationUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            ApplicationAccess access = ApplicationAccess.current(PERMISSION_UPDATE);
            ApplicationEntity entity = loadVisible(access, id);
            boolean applicant = access.selfService(entity);
            access.requireWritable(entity);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (applicant) {
                requireApplicantFields(fields.stream().filter(OFFICE_FIELDS_OF_AN_UPDATE::contains).toList());
            }
            if (ENROLLING.contains(entity.getStatus()) && fields.stream().anyMatch(ENROLMENT_FIELDS::contains)) {
                throw new InvalidStateException("The person and the student of an admitted application are kept by "
                        + "its enrolment.");
            }
            apply(entity, changes, fields);
            validator.validateExcept(dto(entity), SECRETS);
            assertValid(entity, false);
            if (!applicant) {
                access.scope().requireWritable(institutionOf(entity));
            }
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return dtoFor(access, entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + AdmissionEvents.UPDATED, AGGREGATE, id, AdmissionEvents.payload(id));
            return dtoFor(access, entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteApplication(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            ApplicationAccess access = ApplicationAccess.current(PERMISSION_DELETE);
            ApplicationEntity entity = loadVisible(access, id);
            access.requireWritable(entity);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (entity.getStudentId() != null && ENROLLING.contains(entity.getStatus())) {
                throw new InvalidStateException("An application that has its student is kept.");
            }
            if (repository.referencedByApplicationEmergencyContactApplicationId(id)) {
                throw new InvalidStateException(
                        "The application is still referenced by admission.application_emergency_contact rows; remove "
                                + "those first.");
            }
            if (repository.referencedByApplicationGuardianApplicationId(id)) {
                throw new InvalidStateException(
                        "The application is still referenced by admission.application_guardian rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AdmissionEvents.DELETED, AGGREGATE, id, AdmissionEvents.payload(id));
        });
    }

    private static Map<String, Function<ApplicationEntity, Object>> officeFields() {
        Map<String, Function<ApplicationEntity, Object>> fields = new LinkedHashMap<>();
        fields.put("applicationDate", ApplicationEntity::getApplicationDate);
        fields.put("admissionDate", ApplicationEntity::getAdmissionDate);
        fields.put("sectionId", ApplicationEntity::getSectionId);
        fields.put("batchId", ApplicationEntity::getBatchId);
        fields.put("applicantPersonId", ApplicationEntity::getApplicantPersonId);
        fields.put("studentId", ApplicationEntity::getStudentId);
        fields.put("isExistingStudent", ApplicationEntity::getIsExistingStudent);
        fields.put("applicationFee", ApplicationEntity::getApplicationFee);
        fields.put("discountPercent", ApplicationEntity::getDiscountPercent);
        fields.put("feePlanId", ApplicationEntity::getFeePlanId);
        fields.put("feeStartDate", ApplicationEntity::getFeeStartDate);
        fields.put("paymentDueDate", ApplicationEntity::getPaymentDueDate);
        fields.put("paymentStatus", ApplicationEntity::getPaymentStatus);
        fields.put("remarks", ApplicationEntity::getRemarks);
        fields.put("paymentStatusNote", ApplicationEntity::getPaymentStatusNote);
        return Collections.unmodifiableMap(fields);
    }

    private static Set<String> officeFieldsOfAnUpdate() {
        Set<String> fields = new HashSet<>(OFFICE_FIELDS.keySet());
        fields.addAll(List.of("institutionId", "admissionCycleId", "applicationNumber"));
        return Set.copyOf(fields);
    }

    /** Stores nationalId encrypted for the row's tenant and column; never the plaintext. */
    private void sealNationalId(ApplicationEntity entity, String plaintext) {
        entity.setNationalIdEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_NATIONAL_ID_ENC));
    }

    /**
     * The response as the caller may see it: an applicant reaching their own application never sees the office's
     * notes on it (remarks, payment status note).
     */
    Application dtoFor(ApplicationAccess access, ApplicationEntity entity) {
        Application dto = dto(entity);
        if (access.selfService(entity)) {
            dto.setRemarks(null);
            dto.setPaymentStatusNote(null);
        }
        return dto;
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    Application dto(ApplicationEntity entity) {
        Application dto = mapper.toDto(entity);
        dto.setNationalIdMasked(
                cipher.maskStored(entity.getNationalIdEnc(), entity.getTenantId(), SECRET_NATIONAL_ID_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: a row outside the caller's reach is 404. */
    private ApplicationEntity loadVisible(ApplicationAccess access, UUID id) {
        ApplicationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!access.reads(entity)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /**
     * Who may create the application: the office of its institution — or, from outside the institution, an
     * applicant, in a cycle of that institution that is open for applications and with the applicant's own fields
     * only (403 anywhere else, as for any institution outside the caller's scope).
     *
     * @return the cycle of an applicant's own application (self-service); empty for the office
     */
    private Optional<AdmissionCycleEntity> requireCreatable(ApplicationAccess access, ApplicationEntity entity) {
        DataScope scope = access.scope();
        if (scope.canRead(institutionOf(entity))) {
            scope.requireWritable(institutionOf(entity));
            return Optional.empty();
        }
        Optional<AdmissionCycleEntity> openCycleOfTheInstitution = Optional.ofNullable(entity.getAdmissionCycleId())
                .flatMap(admissionCycleRepository::findById)
                .filter(cycle -> AdmissionCycleService.OPEN.equals(cycle.getStatus()))
                .filter(cycle -> cycle.getInstitutionId().equals(entity.getInstitutionId()));
        if (openCycleOfTheInstitution.isEmpty()) {
            scope.requireWritable(institutionOf(entity));
        }
        requireApplicantFields(OFFICE_FIELDS.entrySet().stream().filter(field -> field.getValue().apply(entity) != null)
                .map(Map.Entry::getKey).toList());
        return openCycleOfTheInstitution;
    }

    /**
     * The application number of an applicant's own application: the next of the institution's application series
     * for the year the cycle starts (admission.number_sequence, locked for the rest of the transaction), written
     * {@code APP-<year>-000001}. Whatever number the applicant sent is replaced: the number becomes the student's
     * admission number, so it is the school's to give. A number the office already gave by hand is skipped.
     */
    private void number(ApplicationEntity entity, AdmissionCycleEntity cycle) {
        String year = String.valueOf(cycle.getStartDate().getYear());
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        UUID actor = Actor.current().orElse(null);
        for (int attempt = 0; attempt < MAX_NUMBER_ATTEMPTS; attempt++) {
            entity.setApplicationNumber(numbers.next(UuidV7.next(clock), entity.getInstitutionId(),
                    NUMBER_SEQUENCE, year, "APP-" + year + "-", now, actor));
            if (!repository.takenInstitutionIdApplicationNumber1(entity)) {
                return;
            }
        }
        throw new ConflictException("NUMBER_SEQUENCE_EXHAUSTED",
                "The next application numbers of this institution are already in use; ask the office.");
    }

    /** An applicant never sets what the admissions office decides (422 naming each such field). */
    private static void requireApplicantFields(List<String> officeFields) {
        if (!officeFields.isEmpty()) {
            throw new ValidationFailedException(officeFields.stream()
                    .map(field -> new FieldError(field, "is set by the admissions office", "READ_ONLY")).toList());
        }
    }

    /** Whether the caller reaches the application a guardian or emergency contact belongs to. */
    private boolean readsParent(ApplicationAccess access, UUID applicationId) {
        return repository.findById(applicationId).filter(access::reads).isPresent();
    }

    /** Refuses a change to a guardian or emergency contact of an application the caller may not change. */
    private void requireParentWritable(ApplicationAccess access, UUID applicationId) {
        access.requireWritable(repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, applicationId)));
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(ApplicationEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected ApplicationUpdate fields; explicit per field so a mask can clear values. */
    private void apply(ApplicationEntity entity, ApplicationUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "admissionCycleId" -> entity.setAdmissionCycleId(changes.getAdmissionCycleId());
                case "applicationNumber" -> entity.setApplicationNumber(changes.getApplicationNumber());
                case "applicationDate" -> entity.setApplicationDate(changes.getApplicationDate());
                case "admissionDate" -> entity.setAdmissionDate(changes.getAdmissionDate());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "gradeLevelId" -> entity.setGradeLevelId(changes.getGradeLevelId());
                case "sectionId" -> entity.setSectionId(changes.getSectionId());
                case "programId" -> entity.setProgramId(changes.getProgramId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "batchId" -> entity.setBatchId(changes.getBatchId());
                case "applicantPersonId" -> entity.setApplicantPersonId(changes.getApplicantPersonId());
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "isExistingStudent" -> entity.setIsExistingStudent(changes.getIsExistingStudent());
                case "honorificId" -> entity.setHonorificId(changes.getHonorificId());
                case "fullName" -> entity.setFullName(changes.getFullName());
                case "firstName" -> entity.setFirstName(changes.getFirstName());
                case "middleName" -> entity.setMiddleName(changes.getMiddleName());
                case "lastName" -> entity.setLastName(changes.getLastName());
                case "dateOfBirth" -> entity.setDateOfBirth(changes.getDateOfBirth());
                case "gender" -> entity.setGender(Optional.ofNullable(changes.getGender()).map(v -> v.getValue())
                        .orElse(null));
                case "birthPlace" -> entity.setBirthPlace(changes.getBirthPlace());
                case "nationality" -> entity.setNationality(changes.getNationality());
                case "religion" -> entity.setReligion(changes.getReligion());
                case "socialCategory" -> entity.setSocialCategory(changes.getSocialCategory());
                case "motherTongue" -> entity.setMotherTongue(changes.getMotherTongue());
                case "identificationMarks" -> entity.setIdentificationMarks(changes.getIdentificationMarks());
                case "nationalId" -> sealNationalId(entity, changes.getNationalId());
                case "bloodGroup" -> entity.setBloodGroup(changes.getBloodGroup());
                case "heightCm" -> entity.setHeightCm(WireValues.decimal(changes.getHeightCm()));
                case "weightKg" -> entity.setWeightKg(WireValues.decimal(changes.getWeightKg()));
                case "photoFileId" -> entity.setPhotoFileId(changes.getPhotoFileId());
                case "email" -> entity.setEmail(changes.getEmail());
                case "phone" -> entity.setPhone(changes.getPhone());
                case "mobile" -> entity.setMobile(changes.getMobile());
                case "addressLine1" -> entity.setAddressLine1(changes.getAddressLine1());
                case "addressLine2" -> entity.setAddressLine2(changes.getAddressLine2());
                case "cityName" -> entity.setCityName(changes.getCityName());
                case "postalCode" -> entity.setPostalCode(changes.getPostalCode());
                case "stateProvinceId" -> entity.setStateProvinceId(changes.getStateProvinceId());
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                case "permanentAddress" -> entity.setPermanentAddress(changes.getPermanentAddress());
                case "communicationAddress" -> entity.setCommunicationAddress(changes.getCommunicationAddress());
                case "previousInstitution" -> entity.setPreviousInstitution(changes.getPreviousInstitution());
                case "previousCourse" -> entity.setPreviousCourse(changes.getPreviousCourse());
                case "previousResult" -> entity.setPreviousResult(changes.getPreviousResult());
                case "previousBoard" -> entity.setPreviousBoard(Optional.ofNullable(changes.getPreviousBoard())
                        .map(v -> v.getValue()).orElse(null));
                case "boardRegistrationNumber" -> entity.setBoardRegistrationNumber(
                        changes.getBoardRegistrationNumber());
                case "reasonForLeaving" -> entity.setReasonForLeaving(changes.getReasonForLeaving());
                case "languageChoice" -> entity.setLanguageChoice(changes.getLanguageChoice());
                case "streamChoice" -> entity.setStreamChoice(changes.getStreamChoice());
                case "hasSiblings" -> entity.setHasSiblings(changes.getHasSiblings());
                case "hasMedicalHistory" -> entity.setHasMedicalHistory(changes.getHasMedicalHistory());
                case "medicalDetails" -> entity.setMedicalDetails(changes.getMedicalDetails());
                case "hasExtracurricular" -> entity.setHasExtracurricular(changes.getHasExtracurricular());
                case "extracurricularDetails" -> entity.setExtracurricularDetails(changes.getExtracurricularDetails());
                case "transportMode" -> entity.setTransportMode(Optional.ofNullable(changes.getTransportMode())
                        .map(v -> v.getValue()).orElse(null));
                case "transportDetails" -> entity.setTransportDetails(changes.getTransportDetails());
                case "familyBusiness" -> entity.setFamilyBusiness(changes.getFamilyBusiness());
                case "familyIncome" -> entity.setFamilyIncome(WireValues.decimal(changes.getFamilyIncome()));
                case "applicationFee" -> entity.setApplicationFee(WireValues.decimal(changes.getApplicationFee()));
                case "discountPercent" -> entity.setDiscountPercent(WireValues.decimal(changes.getDiscountPercent()));
                case "feePlanId" -> entity.setFeePlanId(changes.getFeePlanId());
                case "feeStartDate" -> entity.setFeeStartDate(changes.getFeeStartDate());
                case "paymentDueDate" -> entity.setPaymentDueDate(changes.getPaymentDueDate());
                case "paymentStatus" -> entity.setPaymentStatus(Optional.ofNullable(changes.getPaymentStatus())
                        .map(v -> v.getValue()).orElse(null));
                case "remarks" -> entity.setRemarks(changes.getRemarks());
                case "paymentStatusNote" -> entity.setPaymentStatusNote(changes.getPaymentStatusNote());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ApplicationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getApplicationNumber() == null) {
            errors.add(new FieldError("applicationNumber", "must not be null", "NotNull"));
        }
        if (!creating && entity.getApplicationDate() == null) {
            errors.add(new FieldError("applicationDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsExistingStudent() == null) {
            errors.add(new FieldError("isExistingStudent", "must not be null", "NotNull"));
        }
        if (entity.getFullName() == null) {
            errors.add(new FieldError("fullName", "must not be null", "NotNull"));
        }
        if (entity.getDateOfBirth() == null) {
            errors.add(new FieldError("dateOfBirth", "must not be null", "NotNull"));
        }
        if (entity.getGender() == null) {
            errors.add(new FieldError("gender", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("heightCm", WireValues.decimal(entity.getHeightCm()), 5, 2, errors);
        WireValues.checkDecimal("weightKg", WireValues.decimal(entity.getWeightKg()), 5, 2, errors);
        WireValues.checkDecimal("familyIncome", WireValues.decimal(entity.getFamilyIncome()), 14, 2, errors);
        WireValues.checkDecimal("applicationFee", WireValues.decimal(entity.getApplicationFee()), 14, 2, errors);
        WireValues.checkDecimal("discountPercent", WireValues.decimal(entity.getDiscountPercent()), 5, 2, errors);
        if (!creating && entity.getPaymentStatus() == null) {
            errors.add(new FieldError("paymentStatus", "must not be null", "NotNull"));
        }
        if (entity.getAdmissionCycleId() != null && !repository.existsAdmissionCycleId(entity.getAdmissionCycleId())) {
            errors.add(new FieldError("admissionCycleId", "admission cycle does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ApplicationEntity entity) {
        if (repository.takenInstitutionIdApplicationNumber1(entity)) {
            throw new DuplicateResourceException(
                    "A application with the same institution id and application number already exists.");
        }
    }

    // ---- ApplicationEmergencyContact ----

    public ApplicationEmergencyContact getApplicationEmergencyContact(UUID applicationId, UUID id) {
        return doInReadTransaction(() -> {
            return applicationEmergencyContactMapper.toDto(loadVisibleApplicationEmergencyContact(ApplicationAccess.current(
                    APPLICATION_EMERGENCY_CONTACT_PERMISSION_GET), applicationId, id));
        });
    }

    public Page<ApplicationEmergencyContact> listApplicationEmergencyContacts(UUID applicationId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        ApplicationAccess access = ApplicationAccess.current(APPLICATION_EMERGENCY_CONTACT_PERMISSION_LIST);
        SortSpec sort =
                SortSpec.parse(sortBy, ApplicationEmergencyContactRepository.SORT_FIELDS,
                        ApplicationEmergencyContactRepository.DEFAULT_ORDER,
                ApplicationEmergencyContactRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ApplicationEmergencyContactRepository.FILTER_FIELDS)
                .and(ApplicationEmergencyContactRepository.ofParent(applicationId))
                        .and(ApplicationEmergencyContactRepository.reach(access.scope(), access.user()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + applicationId, clock);
        return doInReadTransaction(() -> {
            Page<ApplicationEmergencyContactEntity> rows = applicationEmergencyContactRepository.list(page, where);
            return rows.map(applicationEmergencyContactMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ApplicationEmergencyContact createApplicationEmergencyContact(UUID applicationId,
            ApplicationEmergencyContact request, boolean validateOnly) {
        validator.validate(request);
        ApplicationEmergencyContactEntity entity = applicationEmergencyContactMapper.toEntity(request);
        entity.setApplicationId(applicationId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        ApplicationAccess access = ApplicationAccess.current(APPLICATION_EMERGENCY_CONTACT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!applicationEmergencyContactRepository.existsApplicationId(entity.getApplicationId())) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            if (!readsParent(access, entity.getApplicationId())) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            assertValidApplicationEmergencyContact(entity, true);
            requireParentWritable(access, entity.getApplicationId());
            assertUniqueApplicationEmergencyContact(entity);
            if (validateOnly) {
                return applicationEmergencyContactMapper.toDto(entity);
            }
            ApplicationEmergencyContactEntity stored = applicationEmergencyContactRepository.insert(entity);
            audit.recordCreate(APPLICATION_EMERGENCY_CONTACT_AGGREGATE, stored.getId());
            outbox.publish(APPLICATION_EMERGENCY_CONTACT_AGGREGATE + AdmissionEvents.CREATED,
                    APPLICATION_EMERGENCY_CONTACT_AGGREGATE, stored.getId(),
                    AdmissionEvents.payload(stored.getId()));
            return applicationEmergencyContactMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ApplicationEmergencyContact updateApplicationEmergencyContact(UUID applicationId, UUID id, String ifMatch,
            ApplicationEmergencyContactUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, APPLICATION_EMERGENCY_CONTACT_WRITABLE);
        return doInWriteTransaction(() -> {
            ApplicationAccess access = ApplicationAccess.current(APPLICATION_EMERGENCY_CONTACT_PERMISSION_UPDATE);
            ApplicationEmergencyContactEntity entity = loadVisibleApplicationEmergencyContact(access, applicationId, id);
            requireParentWritable(access, entity.getApplicationId());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyApplicationEmergencyContact(entity, changes, fields);
            validator.validate(applicationEmergencyContactMapper.toDto(entity));
            assertValidApplicationEmergencyContact(entity, false);
            requireParentWritable(access, entity.getApplicationId());
            assertUniqueApplicationEmergencyContact(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return applicationEmergencyContactMapper.toDto(entity);
            }
            if (applicationEmergencyContactRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id, fields);
            outbox.publish(APPLICATION_EMERGENCY_CONTACT_AGGREGATE + AdmissionEvents.UPDATED,
                    APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id, AdmissionEvents.payload(id));
            return applicationEmergencyContactMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteApplicationEmergencyContact(UUID applicationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            ApplicationAccess access = ApplicationAccess.current(APPLICATION_EMERGENCY_CONTACT_PERMISSION_DELETE);
            ApplicationEmergencyContactEntity entity = loadVisibleApplicationEmergencyContact(access, applicationId, id);
            requireParentWritable(access, entity.getApplicationId());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (applicationEmergencyContactRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id);
            outbox.publish(APPLICATION_EMERGENCY_CONTACT_AGGREGATE + AdmissionEvents.DELETED,
                    APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id, AdmissionEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ApplicationEmergencyContactEntity loadVisibleApplicationEmergencyContact(ApplicationAccess access, UUID applicationId,
            UUID id) {
        ApplicationEmergencyContactEntity entity = applicationEmergencyContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id));
        if (!readsParent(access, entity.getApplicationId())
                || !applicationId.equals(entity.getApplicationId())) {
            throw new ResourceNotFoundException(APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id);
        }
        return entity;
    }

    /** Applies the selected ApplicationEmergencyContactUpdate fields; explicit per field so a mask can clear values. */
    private static void applyApplicationEmergencyContact(ApplicationEmergencyContactEntity entity,
            ApplicationEmergencyContactUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "priority" -> entity.setPriority(changes.getPriority());
                case "contactNumber" -> entity.setContactNumber(changes.getContactNumber());
                case "relationship" -> entity.setRelationship(changes.getRelationship());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidApplicationEmergencyContact(ApplicationEmergencyContactEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getApplicationId() == null) {
            errors.add(new FieldError("applicationId", "must not be null", "NotNull"));
        }
        if (entity.getPriority() == null) {
            errors.add(new FieldError("priority", "must not be null", "NotNull"));
        }
        if (entity.getContactNumber() == null) {
            errors.add(new FieldError("contactNumber", "must not be null", "NotNull"));
        }
        if (entity.getRelationship() == null) {
            errors.add(new FieldError("relationship", "must not be null", "NotNull"));
        }
        if (entity.getApplicationId() != null
                && !applicationEmergencyContactRepository.existsApplicationId(entity.getApplicationId())) {
            errors.add(new FieldError("applicationId", "application does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueApplicationEmergencyContact(ApplicationEmergencyContactEntity entity) {
        if (applicationEmergencyContactRepository.takenApplicationIdPriority1(entity)) {
            throw new DuplicateResourceException(
                    "A application emergency contact with the same application id and priority already exists.");
        }
    }

    // ---- ApplicationGuardian ----

    public ApplicationGuardian getApplicationGuardian(UUID applicationId, UUID id) {
        return doInReadTransaction(() -> {
            return dtoApplicationGuardian(loadVisibleApplicationGuardian(ApplicationAccess.current(
                    APPLICATION_GUARDIAN_PERMISSION_GET), applicationId, id));
        });
    }

    public Page<ApplicationGuardian> listApplicationGuardians(UUID applicationId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        ApplicationAccess access = ApplicationAccess.current(APPLICATION_GUARDIAN_PERMISSION_LIST);
        SortSpec sort =
                SortSpec.parse(sortBy, ApplicationGuardianRepository.SORT_FIELDS,
                        ApplicationGuardianRepository.DEFAULT_ORDER,
                ApplicationGuardianRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ApplicationGuardianRepository.FILTER_FIELDS)
                .and(ApplicationGuardianRepository.ofParent(applicationId))
                        .and(ApplicationGuardianRepository.reach(access.scope(), access.user()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + applicationId, clock);
        return doInReadTransaction(() -> {
            Page<ApplicationGuardianEntity> rows = applicationGuardianRepository.list(page, where);
            return rows.map(this::dtoApplicationGuardian);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ApplicationGuardian createApplicationGuardian(UUID applicationId, ApplicationGuardian request,
            boolean validateOnly) {
        validator.validate(request);
        ApplicationGuardianEntity entity = applicationGuardianMapper.toEntity(request);
        entity.setApplicationId(applicationId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealNationalIdApplicationGuardian(entity, request.getNationalId());
        ApplicationAccess access = ApplicationAccess.current(APPLICATION_GUARDIAN_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!applicationGuardianRepository.existsApplicationId(entity.getApplicationId())) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            if (!readsParent(access, entity.getApplicationId())) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            assertValidApplicationGuardian(entity, true);
            requireParentWritable(access, entity.getApplicationId());
            assertUniqueApplicationGuardian(entity);
            if (validateOnly) {
                return dtoApplicationGuardian(entity);
            }
            ApplicationGuardianEntity stored = applicationGuardianRepository.insert(entity);
            audit.recordCreate(APPLICATION_GUARDIAN_AGGREGATE, stored.getId());
            outbox.publish(APPLICATION_GUARDIAN_AGGREGATE + AdmissionEvents.CREATED, APPLICATION_GUARDIAN_AGGREGATE,
                    stored.getId(),
                    AdmissionEvents.payload(stored.getId()));
            return dtoApplicationGuardian(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ApplicationGuardian updateApplicationGuardian(UUID applicationId, UUID id, String ifMatch,
            ApplicationGuardianUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, APPLICATION_GUARDIAN_WRITABLE);
        return doInWriteTransaction(() -> {
            ApplicationAccess access = ApplicationAccess.current(APPLICATION_GUARDIAN_PERMISSION_UPDATE);
            ApplicationGuardianEntity entity = loadVisibleApplicationGuardian(access, applicationId, id);
            requireParentWritable(access, entity.getApplicationId());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyApplicationGuardian(entity, changes, fields);
            validator.validateExcept(dtoApplicationGuardian(entity), APPLICATION_GUARDIAN_SECRETS);
            assertValidApplicationGuardian(entity, false);
            requireParentWritable(access, entity.getApplicationId());
            assertUniqueApplicationGuardian(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return dtoApplicationGuardian(entity);
            }
            if (applicationGuardianRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(APPLICATION_GUARDIAN_AGGREGATE, id, fields);
            outbox.publish(APPLICATION_GUARDIAN_AGGREGATE + AdmissionEvents.UPDATED, APPLICATION_GUARDIAN_AGGREGATE, id,
                    AdmissionEvents.payload(id));
            return dtoApplicationGuardian(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteApplicationGuardian(UUID applicationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            ApplicationAccess access = ApplicationAccess.current(APPLICATION_GUARDIAN_PERMISSION_DELETE);
            ApplicationGuardianEntity entity = loadVisibleApplicationGuardian(access, applicationId, id);
            requireParentWritable(access, entity.getApplicationId());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (applicationGuardianRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(APPLICATION_GUARDIAN_AGGREGATE, id);
            outbox.publish(APPLICATION_GUARDIAN_AGGREGATE + AdmissionEvents.DELETED, APPLICATION_GUARDIAN_AGGREGATE, id,
                    AdmissionEvents.payload(id));
        });
    }

    /** Stores nationalId encrypted for the row's tenant and column; never the plaintext. */
    private void sealNationalIdApplicationGuardian(ApplicationGuardianEntity entity, String plaintext) {
        entity.setNationalIdEnc(cipher.encrypt(plaintext, entity.getTenantId(), APPLICATION_GUARDIAN_SECRET_NATIONAL_ID_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private ApplicationGuardian dtoApplicationGuardian(ApplicationGuardianEntity entity) {
        ApplicationGuardian dto = applicationGuardianMapper.toDto(entity);
        dto.setNationalIdMasked(
                cipher.maskStored(entity.getNationalIdEnc(), entity.getTenantId(), APPLICATION_GUARDIAN_SECRET_NATIONAL_ID_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ApplicationGuardianEntity loadVisibleApplicationGuardian(ApplicationAccess access, UUID applicationId, UUID id) {
        ApplicationGuardianEntity entity = applicationGuardianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION_GUARDIAN_AGGREGATE, id));
        if (!readsParent(access, entity.getApplicationId()) || !applicationId.equals(entity.getApplicationId())) {
            throw new ResourceNotFoundException(APPLICATION_GUARDIAN_AGGREGATE, id);
        }
        return entity;
    }

    /** Applies the selected ApplicationGuardianUpdate fields; explicit per field so a mask can clear values. */
    private void applyApplicationGuardian(ApplicationGuardianEntity entity, ApplicationGuardianUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "relationship" -> entity.setRelationship(Optional.ofNullable(changes.getRelationship())
                        .map(v -> v.getValue()).orElse(null));
                case "name" -> entity.setName(changes.getName());
                case "qualificationId" -> entity.setQualificationId(changes.getQualificationId());
                case "designation" -> entity.setDesignation(changes.getDesignation());
                case "occupation" -> entity.setOccupation(changes.getOccupation());
                case "organisation" -> entity.setOrganisation(changes.getOrganisation());
                case "annualIncome" -> entity.setAnnualIncome(WireValues.decimal(changes.getAnnualIncome()));
                case "mobile" -> entity.setMobile(changes.getMobile());
                case "email" -> entity.setEmail(changes.getEmail());
                case "nationalId" -> sealNationalIdApplicationGuardian(entity, changes.getNationalId());
                case "photoFileId" -> entity.setPhotoFileId(changes.getPhotoFileId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidApplicationGuardian(ApplicationGuardianEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getApplicationId() == null) {
            errors.add(new FieldError("applicationId", "must not be null", "NotNull"));
        }
        if (entity.getRelationship() == null) {
            errors.add(new FieldError("relationship", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("annualIncome", WireValues.decimal(entity.getAnnualIncome()), 14, 2, errors);
        if (entity.getApplicationId() != null && !applicationGuardianRepository.existsApplicationId(entity.getApplicationId())) {
            errors.add(new FieldError("applicationId", "application does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueApplicationGuardian(ApplicationGuardianEntity entity) {
        if (applicationGuardianRepository.takenApplicationIdRelationship1(entity)) {
            throw new DuplicateResourceException(
                    "A application guardian with the same application id and relationship already exists.");
        }
    }
}
