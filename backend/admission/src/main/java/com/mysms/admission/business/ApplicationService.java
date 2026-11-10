package com.mysms.admission.business;

import com.mysms.admission.api.model.Application;
import com.mysms.admission.api.model.ApplicationEmergencyContact;
import com.mysms.admission.api.model.ApplicationEmergencyContactUpdate;
import com.mysms.admission.api.model.ApplicationGuardian;
import com.mysms.admission.api.model.ApplicationGuardianUpdate;
import com.mysms.admission.api.model.ApplicationUpdate;
import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.config.AdmissionEventsConfig;
import com.mysms.admission.domain.ApplicationEmergencyContactEntity;
import com.mysms.admission.domain.ApplicationEntity;
import com.mysms.admission.domain.ApplicationGuardianEntity;
import com.mysms.admission.events.AdmissionEvents;
import com.mysms.admission.mapper.ApplicationEmergencyContactMapper;
import com.mysms.admission.mapper.ApplicationGuardianMapper;
import com.mysms.admission.mapper.ApplicationMapper;
import com.mysms.admission.repository.ApplicationEmergencyContactRepository;
import com.mysms.admission.repository.ApplicationGuardianRepository;
import com.mysms.admission.repository.ApplicationRepository;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of admission.application (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Application
 * are served here too — ApplicationEmergencyContact, ApplicationGuardian.
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
            ApplicationGuardianMapper applicationGuardianMapper) {
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
    }

    /** Refuses to start without a master key: this resource stores secret columns (DEPLOYMENT.md "Secrets"). */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public Application getApplication(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Application> listApplications(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ApplicationRepository.SORT_FIELDS, ApplicationRepository.DEFAULT_ORDER,
                ApplicationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ApplicationRepository.FILTER_FIELDS)
                .and(ApplicationRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ApplicationEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Application createApplication(Application request, boolean validateOnly) {
        validator.validate(request);
        ApplicationEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealNationalId(entity, request.getNationalId());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            ApplicationEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AdmissionEvents.CREATED, AGGREGATE, stored.getId(),
                    AdmissionEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Application updateApplication(UUID id, String ifMatch, ApplicationUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ApplicationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validateExcept(dto(entity), SECRETS);
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return dto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + AdmissionEvents.UPDATED, AGGREGATE, id, AdmissionEvents.payload(id));
            return dto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteApplication(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ApplicationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
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

    /** Stores nationalId encrypted for the row's tenant and column (database/README "Secrets"); never the plaintext. */
    private void sealNationalId(ApplicationEntity entity, String plaintext) {
        entity.setNationalIdEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_NATIONAL_ID_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private Application dto(ApplicationEntity entity) {
        Application dto = mapper.toDto(entity);
        dto.setNationalIdMasked(
                cipher.maskStored(entity.getNationalIdEnc(), entity.getTenantId(), SECRET_NATIONAL_ID_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ApplicationEntity loadVisible(DataScope scope, UUID id) {
        ApplicationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(ApplicationEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected ApplicationUpdate fields; explicit per field so a mask can clear values (no reflection). */
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

    // ---- ApplicationEmergencyContact: a record of the Application aggregate ----

    public ApplicationEmergencyContact getApplicationEmergencyContact(UUID applicationId, UUID id) {
        return doInReadTransaction(() -> {
            return applicationEmergencyContactMapper.toDto(loadVisibleApplicationEmergencyContact(DataScope.current(
                    APPLICATION_EMERGENCY_CONTACT_PERMISSION_GET), applicationId, id));
        });
    }

    public Page<ApplicationEmergencyContact> listApplicationEmergencyContacts(UUID applicationId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ApplicationEmergencyContactRepository.SORT_FIELDS,
                        ApplicationEmergencyContactRepository.DEFAULT_ORDER,
                ApplicationEmergencyContactRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ApplicationEmergencyContactRepository.FILTER_FIELDS)
                .and(ApplicationEmergencyContactRepository.ofParent(applicationId))
                        .and(ApplicationEmergencyContactRepository.scope(DataScope.current(
                                APPLICATION_EMERGENCY_CONTACT_PERMISSION_LIST)));
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
        DataScope scope = DataScope.current(APPLICATION_EMERGENCY_CONTACT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!applicationEmergencyContactRepository.existsApplicationId(entity.getApplicationId())) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            if (!scope.canRead(institutionOfApplicationEmergencyContact(entity))) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            assertValidApplicationEmergencyContact(entity, true);
            scope.requireWritable(institutionOfApplicationEmergencyContact(entity));
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ApplicationEmergencyContact updateApplicationEmergencyContact(UUID applicationId, UUID id, String ifMatch,
            ApplicationEmergencyContactUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, APPLICATION_EMERGENCY_CONTACT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(APPLICATION_EMERGENCY_CONTACT_PERMISSION_UPDATE);
            ApplicationEmergencyContactEntity entity = loadVisibleApplicationEmergencyContact(scope, applicationId, id);
            scope.requireWritable(institutionOfApplicationEmergencyContact(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyApplicationEmergencyContact(entity, changes, fields);
            validator.validate(applicationEmergencyContactMapper.toDto(entity));
            assertValidApplicationEmergencyContact(entity, false);
            scope.requireWritable(institutionOfApplicationEmergencyContact(entity));
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
            DataScope scope = DataScope.current(APPLICATION_EMERGENCY_CONTACT_PERMISSION_DELETE);
            ApplicationEmergencyContactEntity entity = loadVisibleApplicationEmergencyContact(scope, applicationId, id);
            scope.requireWritable(institutionOfApplicationEmergencyContact(entity));
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
    private ApplicationEmergencyContactEntity loadVisibleApplicationEmergencyContact(DataScope scope, UUID applicationId,
            UUID id) {
        ApplicationEmergencyContactEntity entity = applicationEmergencyContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id));
        if (!scope.canRead(institutionOfApplicationEmergencyContact(entity))
                || !applicationId.equals(entity.getApplicationId())) {
            throw new ResourceNotFoundException(APPLICATION_EMERGENCY_CONTACT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the admission.application it belongs to). */
    private Optional<UUID> institutionOfApplicationEmergencyContact(ApplicationEmergencyContactEntity entity) {
        return applicationEmergencyContactRepository.institutionOf(entity.getApplicationId());
    }

    /**
     * Applies the selected ApplicationEmergencyContactUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
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

    // ---- ApplicationGuardian: a record of the Application aggregate ----

    public ApplicationGuardian getApplicationGuardian(UUID applicationId, UUID id) {
        return doInReadTransaction(() -> {
            return dtoApplicationGuardian(loadVisibleApplicationGuardian(DataScope.current(
                    APPLICATION_GUARDIAN_PERMISSION_GET), applicationId, id));
        });
    }

    public Page<ApplicationGuardian> listApplicationGuardians(UUID applicationId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ApplicationGuardianRepository.SORT_FIELDS,
                        ApplicationGuardianRepository.DEFAULT_ORDER,
                ApplicationGuardianRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ApplicationGuardianRepository.FILTER_FIELDS)
                .and(ApplicationGuardianRepository.ofParent(applicationId))
                        .and(ApplicationGuardianRepository.scope(DataScope.current(APPLICATION_GUARDIAN_PERMISSION_LIST)));
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
        DataScope scope = DataScope.current(APPLICATION_GUARDIAN_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!applicationGuardianRepository.existsApplicationId(entity.getApplicationId())) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            if (!scope.canRead(institutionOfApplicationGuardian(entity))) {
                throw new ResourceNotFoundException("admission.application", entity.getApplicationId());
            }
            assertValidApplicationGuardian(entity, true);
            scope.requireWritable(institutionOfApplicationGuardian(entity));
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ApplicationGuardian updateApplicationGuardian(UUID applicationId, UUID id, String ifMatch,
            ApplicationGuardianUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, APPLICATION_GUARDIAN_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(APPLICATION_GUARDIAN_PERMISSION_UPDATE);
            ApplicationGuardianEntity entity = loadVisibleApplicationGuardian(scope, applicationId, id);
            scope.requireWritable(institutionOfApplicationGuardian(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyApplicationGuardian(entity, changes, fields);
            validator.validateExcept(dtoApplicationGuardian(entity), APPLICATION_GUARDIAN_SECRETS);
            assertValidApplicationGuardian(entity, false);
            scope.requireWritable(institutionOfApplicationGuardian(entity));
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
            DataScope scope = DataScope.current(APPLICATION_GUARDIAN_PERMISSION_DELETE);
            ApplicationGuardianEntity entity = loadVisibleApplicationGuardian(scope, applicationId, id);
            scope.requireWritable(institutionOfApplicationGuardian(entity));
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

    /** Stores nationalId encrypted for the row's tenant and column (database/README "Secrets"); never the plaintext. */
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
    private ApplicationGuardianEntity loadVisibleApplicationGuardian(DataScope scope, UUID applicationId, UUID id) {
        ApplicationGuardianEntity entity = applicationGuardianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION_GUARDIAN_AGGREGATE, id));
        if (!scope.canRead(institutionOfApplicationGuardian(entity)) || !applicationId.equals(entity.getApplicationId())) {
            throw new ResourceNotFoundException(APPLICATION_GUARDIAN_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the admission.application it belongs to). */
    private Optional<UUID> institutionOfApplicationGuardian(ApplicationGuardianEntity entity) {
        return applicationGuardianRepository.institutionOf(entity.getApplicationId());
    }

    /**
     * Applies the selected ApplicationGuardianUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
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
