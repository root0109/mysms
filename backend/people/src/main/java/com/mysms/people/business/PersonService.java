package com.mysms.people.business;

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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.people.api.model.Certification;
import com.mysms.people.api.model.CertificationUpdate;
import com.mysms.people.api.model.EducationHistory;
import com.mysms.people.api.model.EducationHistoryUpdate;
import com.mysms.people.api.model.EmergencyContact;
import com.mysms.people.api.model.EmergencyContactUpdate;
import com.mysms.people.api.model.Person;
import com.mysms.people.api.model.PersonAddress;
import com.mysms.people.api.model.PersonAddressUpdate;
import com.mysms.people.api.model.PersonDocument;
import com.mysms.people.api.model.PersonDocumentUpdate;
import com.mysms.people.api.model.PersonSkill;
import com.mysms.people.api.model.PersonSkillUpdate;
import com.mysms.people.api.model.PersonSocialLink;
import com.mysms.people.api.model.PersonSocialLinkUpdate;
import com.mysms.people.api.model.PersonUpdate;
import com.mysms.people.api.model.PublicProfile;
import com.mysms.people.api.model.PublicProfileUpdate;
import com.mysms.people.api.model.WorkExperience;
import com.mysms.people.api.model.WorkExperienceUpdate;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.config.PeopleEventsConfig;
import com.mysms.people.domain.CertificationEntity;
import com.mysms.people.domain.EducationHistoryEntity;
import com.mysms.people.domain.EmergencyContactEntity;
import com.mysms.people.domain.PersonAddressEntity;
import com.mysms.people.domain.PersonDocumentEntity;
import com.mysms.people.domain.PersonEntity;
import com.mysms.people.domain.PersonSkillEntity;
import com.mysms.people.domain.PersonSocialLinkEntity;
import com.mysms.people.domain.PublicProfileEntity;
import com.mysms.people.domain.WorkExperienceEntity;
import com.mysms.people.events.PeopleEvents;
import com.mysms.people.mapper.CertificationMapper;
import com.mysms.people.mapper.EducationHistoryMapper;
import com.mysms.people.mapper.EmergencyContactMapper;
import com.mysms.people.mapper.PersonAddressMapper;
import com.mysms.people.mapper.PersonDocumentMapper;
import com.mysms.people.mapper.PersonMapper;
import com.mysms.people.mapper.PersonSkillMapper;
import com.mysms.people.mapper.PersonSocialLinkMapper;
import com.mysms.people.mapper.PublicProfileMapper;
import com.mysms.people.mapper.WorkExperienceMapper;
import com.mysms.people.repository.CertificationRepository;
import com.mysms.people.repository.EducationHistoryRepository;
import com.mysms.people.repository.EmergencyContactRepository;
import com.mysms.people.repository.PersonAddressRepository;
import com.mysms.people.repository.PersonDocumentRepository;
import com.mysms.people.repository.PersonRepository;
import com.mysms.people.repository.PersonSkillRepository;
import com.mysms.people.repository.PersonSocialLinkRepository;
import com.mysms.people.repository.PublicProfileRepository;
import com.mysms.people.repository.WorkExperienceRepository;
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
 * Business service of people.person (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Person
 * are served here too — Certification, EducationHistory, EmergencyContact, PersonAddress, PersonDocument,
 * PersonSkill, PersonSocialLink, PublicProfile, WorkExperience.
 */
@Service
public class PersonService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = PersonEntity.TABLE;
    static final String PERMISSION_LIST = "people.person.read";
    static final String PERMISSION_GET = "people.person.read";
    static final String PERMISSION_CREATE = "people.person.create";
    static final String PERMISSION_UPDATE = "people.person.update";
    static final String PERMISSION_DELETE = "people.person.delete";
    /** The contract's PersonUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("honorificId", "firstName", "middleName", "lastName", "displayName", "dateOfBirth", "gender",
                    "bloodGroup",
            "maritalStatus", "birthPlace", "nativePlace", "nationalityCountryId", "motherTongue", "religion",
            "socialCategory", "socialSubCategory", "nationalIdType", "nationalId", "visaInfo", "primaryEmail",
            "primaryPhone", "alternatePhone", "photoFileId", "remarks", "workPhone", "websiteUrl", "aboutMe");

    public static final String CERTIFICATION_AGGREGATE = CertificationEntity.TABLE;
    static final String CERTIFICATION_PERMISSION_LIST = "people.certification.read";
    static final String CERTIFICATION_PERMISSION_GET = "people.certification.read";
    static final String CERTIFICATION_PERMISSION_CREATE = "people.certification.create";
    static final String CERTIFICATION_PERMISSION_UPDATE = "people.certification.update";
    static final String CERTIFICATION_PERMISSION_DELETE = "people.certification.delete";
    /** The contract's CertificationUpdate fields, in apply order. */
    static final List<String> CERTIFICATION_WRITABLE =
            List.of("name", "issuingOrganization", "issueDate", "expires", "expirationDate", "description");

    public static final String EDUCATION_HISTORY_AGGREGATE = EducationHistoryEntity.TABLE;
    static final String EDUCATION_HISTORY_PERMISSION_LIST = "people.education_history.read";
    static final String EDUCATION_HISTORY_PERMISSION_GET = "people.education_history.read";
    static final String EDUCATION_HISTORY_PERMISSION_CREATE = "people.education_history.create";
    static final String EDUCATION_HISTORY_PERMISSION_UPDATE = "people.education_history.update";
    static final String EDUCATION_HISTORY_PERMISSION_DELETE = "people.education_history.delete";
    /** The contract's EducationHistoryUpdate fields, in apply order. */
    static final List<String> EDUCATION_HISTORY_WRITABLE =
            List.of("qualificationId", "institutionName", "location", "degreeType", "major", "gradeType", "grade",
                    "startDate",
            "endDate", "certification", "minor", "studyMode", "sortOrder");

    public static final String EMERGENCY_CONTACT_AGGREGATE = EmergencyContactEntity.TABLE;
    static final String EMERGENCY_CONTACT_PERMISSION_LIST = "people.emergency_contact.read";
    static final String EMERGENCY_CONTACT_PERMISSION_GET = "people.emergency_contact.read";
    static final String EMERGENCY_CONTACT_PERMISSION_CREATE = "people.emergency_contact.create";
    static final String EMERGENCY_CONTACT_PERMISSION_UPDATE = "people.emergency_contact.update";
    static final String EMERGENCY_CONTACT_PERMISSION_DELETE = "people.emergency_contact.delete";
    /** The contract's EmergencyContactUpdate fields, in apply order. */
    static final List<String> EMERGENCY_CONTACT_WRITABLE = List.of("contactPersonId", "contactName", "relationship", "phone",
            "priority");

    public static final String PERSON_ADDRESS_AGGREGATE = PersonAddressEntity.TABLE;
    static final String PERSON_ADDRESS_PERMISSION_LIST = "people.person_address.read";
    static final String PERSON_ADDRESS_PERMISSION_GET = "people.person_address.read";
    static final String PERSON_ADDRESS_PERMISSION_CREATE = "people.person_address.create";
    static final String PERSON_ADDRESS_PERMISSION_UPDATE = "people.person_address.update";
    static final String PERSON_ADDRESS_PERMISSION_DELETE = "people.person_address.delete";
    /** The contract's PersonAddressUpdate fields, in apply order. */
    static final List<String> PERSON_ADDRESS_WRITABLE =
            List.of("addressType", "line1", "line2", "cityId", "cityName", "stateProvinceId", "countryId", "postalCode",
            "isPrimary", "stateName", "sortOrder");

    public static final String PERSON_DOCUMENT_AGGREGATE = PersonDocumentEntity.TABLE;
    static final String PERSON_DOCUMENT_PERMISSION_LIST = "people.person_document.read";
    static final String PERSON_DOCUMENT_PERMISSION_GET = "people.person_document.read";
    static final String PERSON_DOCUMENT_PERMISSION_CREATE = "people.person_document.create";
    static final String PERSON_DOCUMENT_PERMISSION_UPDATE = "people.person_document.update";
    static final String PERSON_DOCUMENT_PERMISSION_DELETE = "people.person_document.delete";
    /** The contract's PersonDocumentUpdate fields, in apply order. */
    static final List<String> PERSON_DOCUMENT_WRITABLE =
            List.of("institutionId", "documentType", "name", "fileId", "documentVersion", "status");

    public static final String PERSON_SKILL_AGGREGATE = PersonSkillEntity.TABLE;
    static final String PERSON_SKILL_PERMISSION_LIST = "people.person_skill.read";
    static final String PERSON_SKILL_PERMISSION_GET = "people.person_skill.read";
    static final String PERSON_SKILL_PERMISSION_CREATE = "people.person_skill.create";
    static final String PERSON_SKILL_PERMISSION_UPDATE = "people.person_skill.update";
    static final String PERSON_SKILL_PERMISSION_DELETE = "people.person_skill.delete";
    /** The contract's PersonSkillUpdate fields, in apply order. */
    static final List<String> PERSON_SKILL_WRITABLE = List.of("skillId");

    public static final String PERSON_SOCIAL_LINK_AGGREGATE = PersonSocialLinkEntity.TABLE;
    static final String PERSON_SOCIAL_LINK_PERMISSION_LIST = "people.person_social_link.read";
    static final String PERSON_SOCIAL_LINK_PERMISSION_GET = "people.person_social_link.read";
    static final String PERSON_SOCIAL_LINK_PERMISSION_CREATE = "people.person_social_link.create";
    static final String PERSON_SOCIAL_LINK_PERMISSION_UPDATE = "people.person_social_link.update";
    static final String PERSON_SOCIAL_LINK_PERMISSION_DELETE = "people.person_social_link.delete";
    /** The contract's PersonSocialLinkUpdate fields, in apply order. */
    static final List<String> PERSON_SOCIAL_LINK_WRITABLE = List.of("network", "handle", "sortOrder");

    public static final String PUBLIC_PROFILE_AGGREGATE = PublicProfileEntity.TABLE;
    static final String PUBLIC_PROFILE_PERMISSION_GET = "people.public_profile.read";
    static final String PUBLIC_PROFILE_PERMISSION_CREATE = "people.public_profile.create";
    static final String PUBLIC_PROFILE_PERMISSION_UPDATE = "people.public_profile.update";
    static final String PUBLIC_PROFILE_PERMISSION_DELETE = "people.public_profile.delete";
    /** The contract's PublicProfileUpdate fields, in apply order. */
    static final List<String> PUBLIC_PROFILE_WRITABLE =
            List.of("headline", "profession", "personaCategory", "coverImageFileId", "city", "countryId", "college",
                    "branch",
            "linkedinUrl", "githubUrl", "twitterUrl", "mediumUrl", "hidePrivateInfo");

    public static final String WORK_EXPERIENCE_AGGREGATE = WorkExperienceEntity.TABLE;
    static final String WORK_EXPERIENCE_PERMISSION_LIST = "people.work_experience.read";
    static final String WORK_EXPERIENCE_PERMISSION_GET = "people.work_experience.read";
    static final String WORK_EXPERIENCE_PERMISSION_CREATE = "people.work_experience.create";
    static final String WORK_EXPERIENCE_PERMISSION_UPDATE = "people.work_experience.update";
    static final String WORK_EXPERIENCE_PERMISSION_DELETE = "people.work_experience.delete";
    /** The contract's WorkExperienceUpdate fields, in apply order. */
    static final List<String> WORK_EXPERIENCE_WRITABLE =
            List.of("experienceType", "title", "company", "location", "fromDate", "toDate", "isCurrent", "description",
            "companyWebsite", "industry", "responsibilities", "sortOrder");

    private final PersonRepository repository;
    private final PersonMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("nationalId");
    private static final String SECRET_NATIONAL_ID_ENC = "people.person.national_id_enc";
    private final FieldCipher cipher;
    private final CertificationRepository certificationRepository;
    private final CertificationMapper certificationMapper;
    private final EducationHistoryRepository educationHistoryRepository;
    private final EducationHistoryMapper educationHistoryMapper;
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyContactMapper emergencyContactMapper;
    private final PersonAddressRepository personAddressRepository;
    private final PersonAddressMapper personAddressMapper;
    private final PersonDocumentRepository personDocumentRepository;
    private final PersonDocumentMapper personDocumentMapper;
    private final PersonSkillRepository personSkillRepository;
    private final PersonSkillMapper personSkillMapper;
    private final PersonSocialLinkRepository personSocialLinkRepository;
    private final PersonSocialLinkMapper personSocialLinkMapper;
    private final PublicProfileRepository publicProfileRepository;
    private final PublicProfileMapper publicProfileMapper;
    private final WorkExperienceRepository workExperienceRepository;
    private final WorkExperienceMapper workExperienceMapper;

    public PersonService(PersonRepository repository,
            PersonMapper mapper,
            @Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PeopleDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PeopleEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PeopleEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            FieldCipher cipher,
            CertificationRepository certificationRepository,
            CertificationMapper certificationMapper,
            EducationHistoryRepository educationHistoryRepository,
            EducationHistoryMapper educationHistoryMapper,
            EmergencyContactRepository emergencyContactRepository,
            EmergencyContactMapper emergencyContactMapper,
            PersonAddressRepository personAddressRepository,
            PersonAddressMapper personAddressMapper,
            PersonDocumentRepository personDocumentRepository,
            PersonDocumentMapper personDocumentMapper,
            PersonSkillRepository personSkillRepository,
            PersonSkillMapper personSkillMapper,
            PersonSocialLinkRepository personSocialLinkRepository,
            PersonSocialLinkMapper personSocialLinkMapper,
            PublicProfileRepository publicProfileRepository,
            PublicProfileMapper publicProfileMapper,
            WorkExperienceRepository workExperienceRepository,
            WorkExperienceMapper workExperienceMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.cipher = cipher;
        this.certificationRepository = certificationRepository;
        this.certificationMapper = certificationMapper;
        this.educationHistoryRepository = educationHistoryRepository;
        this.educationHistoryMapper = educationHistoryMapper;
        this.emergencyContactRepository = emergencyContactRepository;
        this.emergencyContactMapper = emergencyContactMapper;
        this.personAddressRepository = personAddressRepository;
        this.personAddressMapper = personAddressMapper;
        this.personDocumentRepository = personDocumentRepository;
        this.personDocumentMapper = personDocumentMapper;
        this.personSkillRepository = personSkillRepository;
        this.personSkillMapper = personSkillMapper;
        this.personSocialLinkRepository = personSocialLinkRepository;
        this.personSocialLinkMapper = personSocialLinkMapper;
        this.publicProfileRepository = publicProfileRepository;
        this.publicProfileMapper = publicProfileMapper;
        this.workExperienceRepository = workExperienceRepository;
        this.workExperienceMapper = workExperienceMapper;
    }

    /** Refuses to start without a master key: this resource stores secret columns (DEPLOYMENT.md "Secrets"). */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public Person getPerson(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Person> listPersons(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PersonRepository.SORT_FIELDS, PersonRepository.DEFAULT_ORDER,
                PersonRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PersonRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PersonEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Person createPerson(Person request, boolean validateOnly) {
        validator.validate(request);
        PersonEntity entity = mapper.toEntity(request);
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
            PersonEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PeopleEvents.CREATED, AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Person updatePerson(UUID id, String ifMatch, PersonUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PersonEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + PeopleEvents.UPDATED, AGGREGATE, id, PeopleEvents.payload(id));
            return dto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePerson(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PersonEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCertificationPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.certification rows; remove those first.");
            }
            if (repository.referencedByConsentRecordGivenByPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.consent_record rows; remove those first.");
            }
            if (repository.referencedByConsentRecordPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.consent_record rows; remove those first.");
            }
            if (repository.referencedByEducationHistoryPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.education_history rows; remove those first.");
            }
            if (repository.referencedByEmergencyContactContactPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.emergency_contact rows; remove those first.");
            }
            if (repository.referencedByEmergencyContactPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.emergency_contact rows; remove those first.");
            }
            if (repository.referencedByPersonAddressPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.person_address rows; remove those first.");
            }
            if (repository.referencedByPersonDocumentPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.person_document rows; remove those first.");
            }
            if (repository.referencedByPersonSkillPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.person_skill rows; remove those first.");
            }
            if (repository.referencedByPersonSocialLinkPersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.person_social_link rows; remove those first.");
            }
            if (repository.referencedByPublicProfilePersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.public_profile rows; remove those first.");
            }
            if (repository.referencedByWorkExperiencePersonId(id)) {
                throw new InvalidStateException(
                        "The person is still referenced by people.work_experience rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PeopleEvents.DELETED, AGGREGATE, id, PeopleEvents.payload(id));
        });
    }

    /** Stores nationalId encrypted for the row's tenant and column (database/README "Secrets"); never the plaintext. */
    private void sealNationalId(PersonEntity entity, String plaintext) {
        entity.setNationalIdEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_NATIONAL_ID_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private Person dto(PersonEntity entity) {
        Person dto = mapper.toDto(entity);
        dto.setNationalIdMasked(
                cipher.maskStored(entity.getNationalIdEnc(), entity.getTenantId(), SECRET_NATIONAL_ID_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PersonEntity loadVisible(DataScope scope, UUID id) {
        PersonEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(PersonEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected PersonUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private void apply(PersonEntity entity, PersonUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "honorificId" -> entity.setHonorificId(changes.getHonorificId());
                case "firstName" -> entity.setFirstName(changes.getFirstName());
                case "middleName" -> entity.setMiddleName(changes.getMiddleName());
                case "lastName" -> entity.setLastName(changes.getLastName());
                case "displayName" -> entity.setDisplayName(changes.getDisplayName());
                case "dateOfBirth" -> entity.setDateOfBirth(changes.getDateOfBirth());
                case "gender" -> entity.setGender(Optional.ofNullable(changes.getGender()).map(v -> v.getValue())
                        .orElse(null));
                case "bloodGroup" -> entity.setBloodGroup(Optional.ofNullable(changes.getBloodGroup())
                        .map(v -> v.getValue()).orElse(null));
                case "maritalStatus" -> entity.setMaritalStatus(Optional.ofNullable(changes.getMaritalStatus())
                        .map(v -> v.getValue()).orElse(null));
                case "birthPlace" -> entity.setBirthPlace(changes.getBirthPlace());
                case "nativePlace" -> entity.setNativePlace(changes.getNativePlace());
                case "nationalityCountryId" -> entity.setNationalityCountryId(changes.getNationalityCountryId());
                case "motherTongue" -> entity.setMotherTongue(changes.getMotherTongue());
                case "religion" -> entity.setReligion(changes.getReligion());
                case "socialCategory" -> entity.setSocialCategory(changes.getSocialCategory());
                case "socialSubCategory" -> entity.setSocialSubCategory(changes.getSocialSubCategory());
                case "nationalIdType" -> entity.setNationalIdType(changes.getNationalIdType());
                case "nationalId" -> sealNationalId(entity, changes.getNationalId());
                case "visaInfo" -> entity.setVisaInfo(changes.getVisaInfo());
                case "primaryEmail" -> entity.setPrimaryEmail(changes.getPrimaryEmail());
                case "primaryPhone" -> entity.setPrimaryPhone(changes.getPrimaryPhone());
                case "alternatePhone" -> entity.setAlternatePhone(changes.getAlternatePhone());
                case "photoFileId" -> entity.setPhotoFileId(changes.getPhotoFileId());
                case "remarks" -> entity.setRemarks(changes.getRemarks());
                case "workPhone" -> entity.setWorkPhone(changes.getWorkPhone());
                case "websiteUrl" -> entity.setWebsiteUrl(changes.getWebsiteUrl());
                case "aboutMe" -> entity.setAboutMe(changes.getAboutMe());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PersonEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getDisplayName() == null) {
            errors.add(new FieldError("displayName", "must not be null", "NotNull"));
        }
        if (entity.getHonorificId() != null && !repository.existsHonorificId(entity.getHonorificId())) {
            errors.add(new FieldError("honorificId", "honorific does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PersonEntity entity) {

    }

    // ---- Certification: a record of the Person aggregate ----

    public Certification getCertification(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return certificationMapper.toDto(loadVisibleCertification(DataScope.current(CERTIFICATION_PERMISSION_GET),
                    personId, id));
        });
    }

    public Page<Certification> listCertifications(UUID personId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CertificationRepository.SORT_FIELDS, CertificationRepository.DEFAULT_ORDER,
                CertificationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CertificationRepository.FILTER_FIELDS)
                .and(CertificationRepository.ofParent(personId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<CertificationEntity> rows = certificationRepository.list(page, where);
            return rows.map(certificationMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Certification createCertification(UUID personId, Certification request, boolean validateOnly) {
        validator.validate(request);
        CertificationEntity entity = certificationMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CERTIFICATION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!certificationRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidCertification(entity, true);
            scope.requireWritable(institutionOfCertification(entity));
            assertUniqueCertification(entity);
            if (validateOnly) {
                return certificationMapper.toDto(entity);
            }
            CertificationEntity stored = certificationRepository.insert(entity);
            audit.recordCreate(CERTIFICATION_AGGREGATE, stored.getId());
            outbox.publish(CERTIFICATION_AGGREGATE + PeopleEvents.CREATED, CERTIFICATION_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return certificationMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Certification updateCertification(UUID personId, UUID id, String ifMatch, CertificationUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CERTIFICATION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CERTIFICATION_PERMISSION_UPDATE);
            CertificationEntity entity = loadVisibleCertification(scope, personId, id);
            scope.requireWritable(institutionOfCertification(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCertification(entity, changes, fields);
            validator.validate(certificationMapper.toDto(entity));
            assertValidCertification(entity, false);
            scope.requireWritable(institutionOfCertification(entity));
            assertUniqueCertification(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return certificationMapper.toDto(entity);
            }
            if (certificationRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CERTIFICATION_AGGREGATE, id, fields);
            outbox.publish(CERTIFICATION_AGGREGATE + PeopleEvents.UPDATED, CERTIFICATION_AGGREGATE, id, PeopleEvents.payload(id));
            return certificationMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCertification(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CERTIFICATION_PERMISSION_DELETE);
            CertificationEntity entity = loadVisibleCertification(scope, personId, id);
            scope.requireWritable(institutionOfCertification(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (certificationRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CERTIFICATION_AGGREGATE, id);
            outbox.publish(CERTIFICATION_AGGREGATE + PeopleEvents.DELETED, CERTIFICATION_AGGREGATE, id, PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CertificationEntity loadVisibleCertification(DataScope scope, UUID personId, UUID id) {
        CertificationEntity entity = certificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CERTIFICATION_AGGREGATE, id));
        if (!scope.canRead(institutionOfCertification(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(CERTIFICATION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfCertification(CertificationEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected CertificationUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCertification(CertificationEntity entity, CertificationUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "name" -> entity.setName(changes.getName());
                case "issuingOrganization" -> entity.setIssuingOrganization(changes.getIssuingOrganization());
                case "issueDate" -> entity.setIssueDate(changes.getIssueDate());
                case "expires" -> entity.setExpires(changes.getExpires());
                case "expirationDate" -> entity.setExpirationDate(changes.getExpirationDate());
                case "description" -> entity.setDescription(changes.getDescription());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCertification(CertificationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getIssuingOrganization() == null) {
            errors.add(new FieldError("issuingOrganization", "must not be null", "NotNull"));
        }
        if (entity.getIssueDate() == null) {
            errors.add(new FieldError("issueDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getExpires() == null) {
            errors.add(new FieldError("expires", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !certificationRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCertification(CertificationEntity entity) {

    }

    // ---- EducationHistory: a record of the Person aggregate ----

    public EducationHistory getEducationHistory(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return educationHistoryMapper.toDto(loadVisibleEducationHistory(DataScope.current(
                    EDUCATION_HISTORY_PERMISSION_GET), personId, id));
        });
    }

    public Page<EducationHistory> listEducationHistorys(UUID personId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EducationHistoryRepository.SORT_FIELDS,
                        EducationHistoryRepository.DEFAULT_ORDER,
                EducationHistoryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EducationHistoryRepository.FILTER_FIELDS)
                .and(EducationHistoryRepository.ofParent(personId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<EducationHistoryEntity> rows = educationHistoryRepository.list(page, where);
            return rows.map(educationHistoryMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EducationHistory createEducationHistory(UUID personId, EducationHistory request, boolean validateOnly) {
        validator.validate(request);
        EducationHistoryEntity entity = educationHistoryMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EDUCATION_HISTORY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!educationHistoryRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidEducationHistory(entity, true);
            scope.requireWritable(institutionOfEducationHistory(entity));
            assertUniqueEducationHistory(entity);
            if (validateOnly) {
                return educationHistoryMapper.toDto(entity);
            }
            EducationHistoryEntity stored = educationHistoryRepository.insert(entity);
            audit.recordCreate(EDUCATION_HISTORY_AGGREGATE, stored.getId());
            outbox.publish(EDUCATION_HISTORY_AGGREGATE + PeopleEvents.CREATED, EDUCATION_HISTORY_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return educationHistoryMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EducationHistory updateEducationHistory(UUID personId, UUID id, String ifMatch,
            EducationHistoryUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EDUCATION_HISTORY_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EDUCATION_HISTORY_PERMISSION_UPDATE);
            EducationHistoryEntity entity = loadVisibleEducationHistory(scope, personId, id);
            scope.requireWritable(institutionOfEducationHistory(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEducationHistory(entity, changes, fields);
            validator.validate(educationHistoryMapper.toDto(entity));
            assertValidEducationHistory(entity, false);
            scope.requireWritable(institutionOfEducationHistory(entity));
            assertUniqueEducationHistory(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return educationHistoryMapper.toDto(entity);
            }
            if (educationHistoryRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EDUCATION_HISTORY_AGGREGATE, id, fields);
            outbox.publish(EDUCATION_HISTORY_AGGREGATE + PeopleEvents.UPDATED, EDUCATION_HISTORY_AGGREGATE, id,
                    PeopleEvents.payload(id));
            return educationHistoryMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEducationHistory(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EDUCATION_HISTORY_PERMISSION_DELETE);
            EducationHistoryEntity entity = loadVisibleEducationHistory(scope, personId, id);
            scope.requireWritable(institutionOfEducationHistory(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (educationHistoryRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EDUCATION_HISTORY_AGGREGATE, id);
            outbox.publish(EDUCATION_HISTORY_AGGREGATE + PeopleEvents.DELETED, EDUCATION_HISTORY_AGGREGATE, id,
                    PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EducationHistoryEntity loadVisibleEducationHistory(DataScope scope, UUID personId, UUID id) {
        EducationHistoryEntity entity = educationHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EDUCATION_HISTORY_AGGREGATE, id));
        if (!scope.canRead(institutionOfEducationHistory(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(EDUCATION_HISTORY_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfEducationHistory(EducationHistoryEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected EducationHistoryUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyEducationHistory(EducationHistoryEntity entity, EducationHistoryUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "qualificationId" -> entity.setQualificationId(changes.getQualificationId());
                case "institutionName" -> entity.setInstitutionName(changes.getInstitutionName());
                case "location" -> entity.setLocation(changes.getLocation());
                case "degreeType" -> entity.setDegreeType(changes.getDegreeType());
                case "major" -> entity.setMajor(changes.getMajor());
                case "gradeType" -> entity.setGradeType(Optional.ofNullable(changes.getGradeType())
                        .map(v -> v.getValue()).orElse(null));
                case "grade" -> entity.setGrade(changes.getGrade());
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                case "certification" -> entity.setCertification(changes.getCertification());
                case "minor" -> entity.setMinor(changes.getMinor());
                case "studyMode" -> entity.setStudyMode(Optional.ofNullable(changes.getStudyMode())
                        .map(v -> v.getValue()).orElse(null));
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEducationHistory(EducationHistoryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionName() == null) {
            errors.add(new FieldError("institutionName", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !educationHistoryRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (entity.getQualificationId() != null
                && !educationHistoryRepository.existsQualificationId(entity.getQualificationId())) {
            errors.add(new FieldError("qualificationId", "qualification does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEducationHistory(EducationHistoryEntity entity) {

    }

    // ---- EmergencyContact: a record of the Person aggregate ----

    public EmergencyContact getEmergencyContact(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return emergencyContactMapper.toDto(loadVisibleEmergencyContact(DataScope.current(
                    EMERGENCY_CONTACT_PERMISSION_GET), personId, id));
        });
    }

    public Page<EmergencyContact> listEmergencyContacts(UUID personId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EmergencyContactRepository.SORT_FIELDS,
                        EmergencyContactRepository.DEFAULT_ORDER,
                EmergencyContactRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EmergencyContactRepository.FILTER_FIELDS)
                .and(EmergencyContactRepository.ofParent(personId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<EmergencyContactEntity> rows = emergencyContactRepository.list(page, where);
            return rows.map(emergencyContactMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EmergencyContact createEmergencyContact(UUID personId, EmergencyContact request, boolean validateOnly) {
        validator.validate(request);
        EmergencyContactEntity entity = emergencyContactMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EMERGENCY_CONTACT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!emergencyContactRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidEmergencyContact(entity, true);
            scope.requireWritable(institutionOfEmergencyContact(entity));
            assertUniqueEmergencyContact(entity);
            if (validateOnly) {
                return emergencyContactMapper.toDto(entity);
            }
            EmergencyContactEntity stored = emergencyContactRepository.insert(entity);
            audit.recordCreate(EMERGENCY_CONTACT_AGGREGATE, stored.getId());
            outbox.publish(EMERGENCY_CONTACT_AGGREGATE + PeopleEvents.CREATED, EMERGENCY_CONTACT_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return emergencyContactMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EmergencyContact updateEmergencyContact(UUID personId, UUID id, String ifMatch,
            EmergencyContactUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EMERGENCY_CONTACT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EMERGENCY_CONTACT_PERMISSION_UPDATE);
            EmergencyContactEntity entity = loadVisibleEmergencyContact(scope, personId, id);
            scope.requireWritable(institutionOfEmergencyContact(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEmergencyContact(entity, changes, fields);
            validator.validate(emergencyContactMapper.toDto(entity));
            assertValidEmergencyContact(entity, false);
            scope.requireWritable(institutionOfEmergencyContact(entity));
            assertUniqueEmergencyContact(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return emergencyContactMapper.toDto(entity);
            }
            if (emergencyContactRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EMERGENCY_CONTACT_AGGREGATE, id, fields);
            outbox.publish(EMERGENCY_CONTACT_AGGREGATE + PeopleEvents.UPDATED, EMERGENCY_CONTACT_AGGREGATE, id,
                    PeopleEvents.payload(id));
            return emergencyContactMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEmergencyContact(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EMERGENCY_CONTACT_PERMISSION_DELETE);
            EmergencyContactEntity entity = loadVisibleEmergencyContact(scope, personId, id);
            scope.requireWritable(institutionOfEmergencyContact(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (emergencyContactRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EMERGENCY_CONTACT_AGGREGATE, id);
            outbox.publish(EMERGENCY_CONTACT_AGGREGATE + PeopleEvents.DELETED, EMERGENCY_CONTACT_AGGREGATE, id,
                    PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EmergencyContactEntity loadVisibleEmergencyContact(DataScope scope, UUID personId, UUID id) {
        EmergencyContactEntity entity = emergencyContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EMERGENCY_CONTACT_AGGREGATE, id));
        if (!scope.canRead(institutionOfEmergencyContact(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(EMERGENCY_CONTACT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfEmergencyContact(EmergencyContactEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected EmergencyContactUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyEmergencyContact(EmergencyContactEntity entity, EmergencyContactUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "contactPersonId" -> entity.setContactPersonId(changes.getContactPersonId());
                case "contactName" -> entity.setContactName(changes.getContactName());
                case "relationship" -> entity.setRelationship(changes.getRelationship());
                case "phone" -> entity.setPhone(changes.getPhone());
                case "priority" -> entity.setPriority(changes.getPriority());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEmergencyContact(EmergencyContactEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPriority() == null) {
            errors.add(new FieldError("priority", "must not be null", "NotNull"));
        }
        if (entity.getContactPersonId() != null
                && !emergencyContactRepository.existsContactPersonId(entity.getContactPersonId())) {
            errors.add(new FieldError("contactPersonId", "person does not exist", "NOT_FOUND"));
        }
        if (entity.getPersonId() != null && !emergencyContactRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEmergencyContact(EmergencyContactEntity entity) {

    }

    // ---- PersonAddress: a record of the Person aggregate ----

    public PersonAddress getPersonAddress(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return personAddressMapper.toDto(loadVisiblePersonAddress(DataScope.current(PERSON_ADDRESS_PERMISSION_GET),
                    personId, id));
        });
    }

    public Page<PersonAddress> listPersonAddresss(UUID personId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PersonAddressRepository.SORT_FIELDS, PersonAddressRepository.DEFAULT_ORDER,
                PersonAddressRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PersonAddressRepository.FILTER_FIELDS)
                .and(PersonAddressRepository.ofParent(personId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<PersonAddressEntity> rows = personAddressRepository.list(page, where);
            return rows.map(personAddressMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PersonAddress createPersonAddress(UUID personId, PersonAddress request, boolean validateOnly) {
        validator.validate(request);
        PersonAddressEntity entity = personAddressMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERSON_ADDRESS_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!personAddressRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidPersonAddress(entity, true);
            scope.requireWritable(institutionOfPersonAddress(entity));
            assertUniquePersonAddress(entity);
            if (validateOnly) {
                return personAddressMapper.toDto(entity);
            }
            PersonAddressEntity stored = personAddressRepository.insert(entity);
            audit.recordCreate(PERSON_ADDRESS_AGGREGATE, stored.getId());
            outbox.publish(PERSON_ADDRESS_AGGREGATE + PeopleEvents.CREATED, PERSON_ADDRESS_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return personAddressMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PersonAddress updatePersonAddress(UUID personId, UUID id, String ifMatch, PersonAddressUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PERSON_ADDRESS_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_ADDRESS_PERMISSION_UPDATE);
            PersonAddressEntity entity = loadVisiblePersonAddress(scope, personId, id);
            scope.requireWritable(institutionOfPersonAddress(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPersonAddress(entity, changes, fields);
            validator.validate(personAddressMapper.toDto(entity));
            assertValidPersonAddress(entity, false);
            scope.requireWritable(institutionOfPersonAddress(entity));
            assertUniquePersonAddress(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return personAddressMapper.toDto(entity);
            }
            if (personAddressRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PERSON_ADDRESS_AGGREGATE, id, fields);
            outbox.publish(PERSON_ADDRESS_AGGREGATE + PeopleEvents.UPDATED, PERSON_ADDRESS_AGGREGATE, id,
                    PeopleEvents.payload(id));
            return personAddressMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePersonAddress(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_ADDRESS_PERMISSION_DELETE);
            PersonAddressEntity entity = loadVisiblePersonAddress(scope, personId, id);
            scope.requireWritable(institutionOfPersonAddress(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (personAddressRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PERSON_ADDRESS_AGGREGATE, id);
            outbox.publish(PERSON_ADDRESS_AGGREGATE + PeopleEvents.DELETED, PERSON_ADDRESS_AGGREGATE, id,
                    PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PersonAddressEntity loadVisiblePersonAddress(DataScope scope, UUID personId, UUID id) {
        PersonAddressEntity entity = personAddressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PERSON_ADDRESS_AGGREGATE, id));
        if (!scope.canRead(institutionOfPersonAddress(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(PERSON_ADDRESS_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfPersonAddress(PersonAddressEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected PersonAddressUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyPersonAddress(PersonAddressEntity entity, PersonAddressUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "addressType" -> entity.setAddressType(Optional.ofNullable(changes.getAddressType())
                        .map(v -> v.getValue()).orElse(null));
                case "line1" -> entity.setLine1(changes.getLine1());
                case "line2" -> entity.setLine2(changes.getLine2());
                case "cityId" -> entity.setCityId(changes.getCityId());
                case "cityName" -> entity.setCityName(changes.getCityName());
                case "stateProvinceId" -> entity.setStateProvinceId(changes.getStateProvinceId());
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                case "postalCode" -> entity.setPostalCode(changes.getPostalCode());
                case "isPrimary" -> entity.setIsPrimary(changes.getIsPrimary());
                case "stateName" -> entity.setStateName(changes.getStateName());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPersonAddress(PersonAddressEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getAddressType() == null) {
            errors.add(new FieldError("addressType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPrimary() == null) {
            errors.add(new FieldError("isPrimary", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !personAddressRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePersonAddress(PersonAddressEntity entity) {

    }

    // ---- PersonDocument: a record of the Person aggregate ----

    public PersonDocument getPersonDocument(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return personDocumentMapper.toDto(loadVisiblePersonDocument(DataScope.current(PERSON_DOCUMENT_PERMISSION_GET),
                    personId, id));
        });
    }

    public Page<PersonDocument> listPersonDocuments(UUID personId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PersonDocumentRepository.SORT_FIELDS, PersonDocumentRepository.DEFAULT_ORDER,
                PersonDocumentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PersonDocumentRepository.FILTER_FIELDS)
                .and(PersonDocumentRepository.ofParent(personId))
                        .and(PersonDocumentRepository.scope(DataScope.current(PERSON_DOCUMENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<PersonDocumentEntity> rows = personDocumentRepository.list(page, where);
            return rows.map(personDocumentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PersonDocument createPersonDocument(UUID personId, PersonDocument request, boolean validateOnly) {
        validator.validate(request);
        PersonDocumentEntity entity = personDocumentMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERSON_DOCUMENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!personDocumentRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidPersonDocument(entity, true);
            scope.requireWritable(institutionOfPersonDocument(entity));
            assertUniquePersonDocument(entity);
            if (validateOnly) {
                return personDocumentMapper.toDto(entity);
            }
            PersonDocumentEntity stored = personDocumentRepository.insert(entity);
            audit.recordCreate(PERSON_DOCUMENT_AGGREGATE, stored.getId());
            outbox.publish(PERSON_DOCUMENT_AGGREGATE + PeopleEvents.CREATED, PERSON_DOCUMENT_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return personDocumentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PersonDocument updatePersonDocument(UUID personId, UUID id, String ifMatch, PersonDocumentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PERSON_DOCUMENT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_DOCUMENT_PERMISSION_UPDATE);
            PersonDocumentEntity entity = loadVisiblePersonDocument(scope, personId, id);
            scope.requireWritable(institutionOfPersonDocument(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPersonDocument(entity, changes, fields);
            validator.validate(personDocumentMapper.toDto(entity));
            assertValidPersonDocument(entity, false);
            scope.requireWritable(institutionOfPersonDocument(entity));
            assertUniquePersonDocument(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return personDocumentMapper.toDto(entity);
            }
            if (personDocumentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PERSON_DOCUMENT_AGGREGATE, id, fields);
            outbox.publish(PERSON_DOCUMENT_AGGREGATE + PeopleEvents.UPDATED, PERSON_DOCUMENT_AGGREGATE, id,
                    PeopleEvents.payload(id));
            return personDocumentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePersonDocument(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_DOCUMENT_PERMISSION_DELETE);
            PersonDocumentEntity entity = loadVisiblePersonDocument(scope, personId, id);
            scope.requireWritable(institutionOfPersonDocument(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (personDocumentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PERSON_DOCUMENT_AGGREGATE, id);
            outbox.publish(PERSON_DOCUMENT_AGGREGATE + PeopleEvents.DELETED, PERSON_DOCUMENT_AGGREGATE, id,
                    PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PersonDocumentEntity loadVisiblePersonDocument(DataScope scope, UUID personId, UUID id) {
        PersonDocumentEntity entity = personDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PERSON_DOCUMENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfPersonDocument(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(PERSON_DOCUMENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOfPersonDocument(PersonDocumentEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected PersonDocumentUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyPersonDocument(PersonDocumentEntity entity, PersonDocumentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "documentType" -> entity.setDocumentType(Optional.ofNullable(changes.getDocumentType())
                        .map(v -> v.getValue()).orElse(null));
                case "name" -> entity.setName(changes.getName());
                case "fileId" -> entity.setFileId(changes.getFileId());
                case "documentVersion" -> entity.setDocumentVersion(changes.getDocumentVersion());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPersonDocument(PersonDocumentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getDocumentType() == null) {
            errors.add(new FieldError("documentType", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getFileId() == null) {
            errors.add(new FieldError("fileId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDocumentVersion() == null) {
            errors.add(new FieldError("documentVersion", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !personDocumentRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePersonDocument(PersonDocumentEntity entity) {
        if (personDocumentRepository.takenPersonIdNameDocumentVersion1(entity)) {
            throw new DuplicateResourceException(
                    "A person document with the same person id and name and document version already exists.");
        }
    }

    // ---- PersonSkill: a record of the Person aggregate ----

    public PersonSkill getPersonSkill(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return personSkillMapper.toDto(loadVisiblePersonSkill(DataScope.current(PERSON_SKILL_PERMISSION_GET), personId, id));
        });
    }

    public Page<PersonSkill> listPersonSkills(UUID personId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PersonSkillRepository.SORT_FIELDS, PersonSkillRepository.DEFAULT_ORDER,
                PersonSkillRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PersonSkillRepository.FILTER_FIELDS)
                .and(PersonSkillRepository.ofParent(personId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<PersonSkillEntity> rows = personSkillRepository.list(page, where);
            return rows.map(personSkillMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PersonSkill createPersonSkill(UUID personId, PersonSkill request, boolean validateOnly) {
        validator.validate(request);
        PersonSkillEntity entity = personSkillMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERSON_SKILL_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!personSkillRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidPersonSkill(entity, true);
            scope.requireWritable(institutionOfPersonSkill(entity));
            assertUniquePersonSkill(entity);
            if (validateOnly) {
                return personSkillMapper.toDto(entity);
            }
            PersonSkillEntity stored = personSkillRepository.insert(entity);
            audit.recordCreate(PERSON_SKILL_AGGREGATE, stored.getId());
            outbox.publish(PERSON_SKILL_AGGREGATE + PeopleEvents.CREATED, PERSON_SKILL_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return personSkillMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PersonSkill updatePersonSkill(UUID personId, UUID id, String ifMatch, PersonSkillUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PERSON_SKILL_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_SKILL_PERMISSION_UPDATE);
            PersonSkillEntity entity = loadVisiblePersonSkill(scope, personId, id);
            scope.requireWritable(institutionOfPersonSkill(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPersonSkill(entity, changes, fields);
            validator.validate(personSkillMapper.toDto(entity));
            assertValidPersonSkill(entity, false);
            scope.requireWritable(institutionOfPersonSkill(entity));
            assertUniquePersonSkill(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return personSkillMapper.toDto(entity);
            }
            if (personSkillRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PERSON_SKILL_AGGREGATE, id, fields);
            outbox.publish(PERSON_SKILL_AGGREGATE + PeopleEvents.UPDATED, PERSON_SKILL_AGGREGATE, id, PeopleEvents.payload(id));
            return personSkillMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePersonSkill(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_SKILL_PERMISSION_DELETE);
            PersonSkillEntity entity = loadVisiblePersonSkill(scope, personId, id);
            scope.requireWritable(institutionOfPersonSkill(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (personSkillRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PERSON_SKILL_AGGREGATE, id);
            outbox.publish(PERSON_SKILL_AGGREGATE + PeopleEvents.DELETED, PERSON_SKILL_AGGREGATE, id, PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PersonSkillEntity loadVisiblePersonSkill(DataScope scope, UUID personId, UUID id) {
        PersonSkillEntity entity = personSkillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PERSON_SKILL_AGGREGATE, id));
        if (!scope.canRead(institutionOfPersonSkill(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(PERSON_SKILL_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfPersonSkill(PersonSkillEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected PersonSkillUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyPersonSkill(PersonSkillEntity entity, PersonSkillUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "skillId" -> entity.setSkillId(changes.getSkillId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPersonSkill(PersonSkillEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getSkillId() == null) {
            errors.add(new FieldError("skillId", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !personSkillRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (entity.getSkillId() != null && !personSkillRepository.existsSkillId(entity.getSkillId())) {
            errors.add(new FieldError("skillId", "skill does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePersonSkill(PersonSkillEntity entity) {
        if (personSkillRepository.takenPersonIdSkillId1(entity)) {
            throw new DuplicateResourceException("A person skill with the same person id and skill id already exists.");
        }
    }

    // ---- PersonSocialLink: a record of the Person aggregate ----

    public PersonSocialLink getPersonSocialLink(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return personSocialLinkMapper.toDto(loadVisiblePersonSocialLink(DataScope.current(
                    PERSON_SOCIAL_LINK_PERMISSION_GET), personId, id));
        });
    }

    public Page<PersonSocialLink> listPersonSocialLinks(UUID personId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PersonSocialLinkRepository.SORT_FIELDS,
                        PersonSocialLinkRepository.DEFAULT_ORDER,
                PersonSocialLinkRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PersonSocialLinkRepository.FILTER_FIELDS)
                .and(PersonSocialLinkRepository.ofParent(personId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<PersonSocialLinkEntity> rows = personSocialLinkRepository.list(page, where);
            return rows.map(personSocialLinkMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PersonSocialLink createPersonSocialLink(UUID personId, PersonSocialLink request, boolean validateOnly) {
        validator.validate(request);
        PersonSocialLinkEntity entity = personSocialLinkMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERSON_SOCIAL_LINK_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!personSocialLinkRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidPersonSocialLink(entity, true);
            scope.requireWritable(institutionOfPersonSocialLink(entity));
            assertUniquePersonSocialLink(entity);
            if (validateOnly) {
                return personSocialLinkMapper.toDto(entity);
            }
            PersonSocialLinkEntity stored = personSocialLinkRepository.insert(entity);
            audit.recordCreate(PERSON_SOCIAL_LINK_AGGREGATE, stored.getId());
            outbox.publish(PERSON_SOCIAL_LINK_AGGREGATE + PeopleEvents.CREATED, PERSON_SOCIAL_LINK_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return personSocialLinkMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PersonSocialLink updatePersonSocialLink(UUID personId, UUID id, String ifMatch,
            PersonSocialLinkUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PERSON_SOCIAL_LINK_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_SOCIAL_LINK_PERMISSION_UPDATE);
            PersonSocialLinkEntity entity = loadVisiblePersonSocialLink(scope, personId, id);
            scope.requireWritable(institutionOfPersonSocialLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPersonSocialLink(entity, changes, fields);
            validator.validate(personSocialLinkMapper.toDto(entity));
            assertValidPersonSocialLink(entity, false);
            scope.requireWritable(institutionOfPersonSocialLink(entity));
            assertUniquePersonSocialLink(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return personSocialLinkMapper.toDto(entity);
            }
            if (personSocialLinkRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PERSON_SOCIAL_LINK_AGGREGATE, id, fields);
            outbox.publish(PERSON_SOCIAL_LINK_AGGREGATE + PeopleEvents.UPDATED, PERSON_SOCIAL_LINK_AGGREGATE, id,
                    PeopleEvents.payload(id));
            return personSocialLinkMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePersonSocialLink(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERSON_SOCIAL_LINK_PERMISSION_DELETE);
            PersonSocialLinkEntity entity = loadVisiblePersonSocialLink(scope, personId, id);
            scope.requireWritable(institutionOfPersonSocialLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (personSocialLinkRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PERSON_SOCIAL_LINK_AGGREGATE, id);
            outbox.publish(PERSON_SOCIAL_LINK_AGGREGATE + PeopleEvents.DELETED, PERSON_SOCIAL_LINK_AGGREGATE, id,
                    PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PersonSocialLinkEntity loadVisiblePersonSocialLink(DataScope scope, UUID personId, UUID id) {
        PersonSocialLinkEntity entity = personSocialLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PERSON_SOCIAL_LINK_AGGREGATE, id));
        if (!scope.canRead(institutionOfPersonSocialLink(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(PERSON_SOCIAL_LINK_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfPersonSocialLink(PersonSocialLinkEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected PersonSocialLinkUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyPersonSocialLink(PersonSocialLinkEntity entity, PersonSocialLinkUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "network" -> entity.setNetwork(Optional.ofNullable(changes.getNetwork()).map(v -> v.getValue())
                        .orElse(null));
                case "handle" -> entity.setHandle(changes.getHandle());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPersonSocialLink(PersonSocialLinkEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getNetwork() == null) {
            errors.add(new FieldError("network", "must not be null", "NotNull"));
        }
        if (entity.getHandle() == null) {
            errors.add(new FieldError("handle", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !personSocialLinkRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePersonSocialLink(PersonSocialLinkEntity entity) {
        if (personSocialLinkRepository.takenPersonIdNetworkHandle1(entity)) {
            throw new DuplicateResourceException(
                    "A person social link with the same person id and network and handle already exists.");
        }
    }

    // ---- PublicProfile: a record of the Person aggregate ----

    public PublicProfile getPublicProfile(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return publicProfileMapper.toDto(loadVisiblePublicProfile(DataScope.current(PUBLIC_PROFILE_PERMISSION_GET),
                    personId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PublicProfile createPublicProfile(UUID personId, PublicProfile request, boolean validateOnly) {
        validator.validate(request);
        PublicProfileEntity entity = publicProfileMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PUBLIC_PROFILE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!publicProfileRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidPublicProfile(entity, true);
            scope.requireWritable(institutionOfPublicProfile(entity));
            assertUniquePublicProfile(entity);
            if (validateOnly) {
                return publicProfileMapper.toDto(entity);
            }
            PublicProfileEntity stored = publicProfileRepository.insert(entity);
            audit.recordCreate(PUBLIC_PROFILE_AGGREGATE, stored.getId());
            outbox.publish(PUBLIC_PROFILE_AGGREGATE + PeopleEvents.CREATED, PUBLIC_PROFILE_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return publicProfileMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PublicProfile updatePublicProfile(UUID personId, UUID id, String ifMatch, PublicProfileUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PUBLIC_PROFILE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PUBLIC_PROFILE_PERMISSION_UPDATE);
            PublicProfileEntity entity = loadVisiblePublicProfile(scope, personId, id);
            scope.requireWritable(institutionOfPublicProfile(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPublicProfile(entity, changes, fields);
            validator.validate(publicProfileMapper.toDto(entity));
            assertValidPublicProfile(entity, false);
            scope.requireWritable(institutionOfPublicProfile(entity));
            assertUniquePublicProfile(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return publicProfileMapper.toDto(entity);
            }
            if (publicProfileRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PUBLIC_PROFILE_AGGREGATE, id, fields);
            outbox.publish(PUBLIC_PROFILE_AGGREGATE + PeopleEvents.UPDATED, PUBLIC_PROFILE_AGGREGATE, id,
                    PeopleEvents.payload(id));
            return publicProfileMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePublicProfile(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PUBLIC_PROFILE_PERMISSION_DELETE);
            PublicProfileEntity entity = loadVisiblePublicProfile(scope, personId, id);
            scope.requireWritable(institutionOfPublicProfile(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (publicProfileRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PUBLIC_PROFILE_AGGREGATE, id);
            outbox.publish(PUBLIC_PROFILE_AGGREGATE + PeopleEvents.DELETED, PUBLIC_PROFILE_AGGREGATE, id,
                    PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PublicProfileEntity loadVisiblePublicProfile(DataScope scope, UUID personId, UUID id) {
        PublicProfileEntity entity = publicProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PUBLIC_PROFILE_AGGREGATE, id));
        if (!scope.canRead(institutionOfPublicProfile(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(PUBLIC_PROFILE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfPublicProfile(PublicProfileEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected PublicProfileUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyPublicProfile(PublicProfileEntity entity, PublicProfileUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "headline" -> entity.setHeadline(changes.getHeadline());
                case "profession" -> entity.setProfession(changes.getProfession());
                case "personaCategory" -> entity.setPersonaCategory(Optional.ofNullable(changes.getPersonaCategory())
                        .map(v -> v.getValue()).orElse(null));
                case "coverImageFileId" -> entity.setCoverImageFileId(changes.getCoverImageFileId());
                case "city" -> entity.setCity(changes.getCity());
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                case "college" -> entity.setCollege(changes.getCollege());
                case "branch" -> entity.setBranch(changes.getBranch());
                case "linkedinUrl" -> entity.setLinkedinUrl(changes.getLinkedinUrl());
                case "githubUrl" -> entity.setGithubUrl(changes.getGithubUrl());
                case "twitterUrl" -> entity.setTwitterUrl(changes.getTwitterUrl());
                case "mediumUrl" -> entity.setMediumUrl(changes.getMediumUrl());
                case "hidePrivateInfo" -> entity.setHidePrivateInfo(changes.getHidePrivateInfo());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPublicProfile(PublicProfileEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getHidePrivateInfo() == null) {
            errors.add(new FieldError("hidePrivateInfo", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !publicProfileRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePublicProfile(PublicProfileEntity entity) {
        if (publicProfileRepository.takenPersonIdId(entity)) {
            throw new DuplicateResourceException("A public profile with the same person id already exists.");
        }
    }

    // ---- WorkExperience: a record of the Person aggregate ----

    public WorkExperience getWorkExperience(UUID personId, UUID id) {
        return doInReadTransaction(() -> {
            return workExperienceMapper.toDto(loadVisibleWorkExperience(DataScope.current(WORK_EXPERIENCE_PERMISSION_GET),
                    personId, id));
        });
    }

    public Page<WorkExperience> listWorkExperiences(UUID personId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, WorkExperienceRepository.SORT_FIELDS, WorkExperienceRepository.DEFAULT_ORDER,
                WorkExperienceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, WorkExperienceRepository.FILTER_FIELDS)
                .and(WorkExperienceRepository.ofParent(personId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + personId, clock);
        return doInReadTransaction(() -> {
            Page<WorkExperienceEntity> rows = workExperienceRepository.list(page, where);
            return rows.map(workExperienceMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public WorkExperience createWorkExperience(UUID personId, WorkExperience request, boolean validateOnly) {
        validator.validate(request);
        WorkExperienceEntity entity = workExperienceMapper.toEntity(request);
        entity.setPersonId(personId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(WORK_EXPERIENCE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!workExperienceRepository.existsPersonId(entity.getPersonId())) {
                throw new ResourceNotFoundException("people.person", entity.getPersonId());
            }
            assertValidWorkExperience(entity, true);
            scope.requireWritable(institutionOfWorkExperience(entity));
            assertUniqueWorkExperience(entity);
            if (validateOnly) {
                return workExperienceMapper.toDto(entity);
            }
            WorkExperienceEntity stored = workExperienceRepository.insert(entity);
            audit.recordCreate(WORK_EXPERIENCE_AGGREGATE, stored.getId());
            outbox.publish(WORK_EXPERIENCE_AGGREGATE + PeopleEvents.CREATED, WORK_EXPERIENCE_AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return workExperienceMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public WorkExperience updateWorkExperience(UUID personId, UUID id, String ifMatch, WorkExperienceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WORK_EXPERIENCE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(WORK_EXPERIENCE_PERMISSION_UPDATE);
            WorkExperienceEntity entity = loadVisibleWorkExperience(scope, personId, id);
            scope.requireWritable(institutionOfWorkExperience(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyWorkExperience(entity, changes, fields);
            validator.validate(workExperienceMapper.toDto(entity));
            assertValidWorkExperience(entity, false);
            scope.requireWritable(institutionOfWorkExperience(entity));
            assertUniqueWorkExperience(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return workExperienceMapper.toDto(entity);
            }
            if (workExperienceRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(WORK_EXPERIENCE_AGGREGATE, id, fields);
            outbox.publish(WORK_EXPERIENCE_AGGREGATE + PeopleEvents.UPDATED, WORK_EXPERIENCE_AGGREGATE, id,
                    PeopleEvents.payload(id));
            return workExperienceMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteWorkExperience(UUID personId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(WORK_EXPERIENCE_PERMISSION_DELETE);
            WorkExperienceEntity entity = loadVisibleWorkExperience(scope, personId, id);
            scope.requireWritable(institutionOfWorkExperience(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (workExperienceRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(WORK_EXPERIENCE_AGGREGATE, id);
            outbox.publish(WORK_EXPERIENCE_AGGREGATE + PeopleEvents.DELETED, WORK_EXPERIENCE_AGGREGATE, id,
                    PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private WorkExperienceEntity loadVisibleWorkExperience(DataScope scope, UUID personId, UUID id) {
        WorkExperienceEntity entity = workExperienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(WORK_EXPERIENCE_AGGREGATE, id));
        if (!scope.canRead(institutionOfWorkExperience(entity)) || !personId.equals(entity.getPersonId())) {
            throw new ResourceNotFoundException(WORK_EXPERIENCE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfWorkExperience(WorkExperienceEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected WorkExperienceUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyWorkExperience(WorkExperienceEntity entity, WorkExperienceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "experienceType" -> entity.setExperienceType(Optional.ofNullable(changes.getExperienceType())
                        .map(v -> v.getValue()).orElse(null));
                case "title" -> entity.setTitle(changes.getTitle());
                case "company" -> entity.setCompany(changes.getCompany());
                case "location" -> entity.setLocation(changes.getLocation());
                case "fromDate" -> entity.setFromDate(changes.getFromDate());
                case "toDate" -> entity.setToDate(changes.getToDate());
                case "isCurrent" -> entity.setIsCurrent(changes.getIsCurrent());
                case "description" -> entity.setDescription(changes.getDescription());
                case "companyWebsite" -> entity.setCompanyWebsite(changes.getCompanyWebsite());
                case "industry" -> entity.setIndustry(changes.getIndustry());
                case "responsibilities" -> entity.setResponsibilities(changes.getResponsibilities());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidWorkExperience(WorkExperienceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getExperienceType() == null) {
            errors.add(new FieldError("experienceType", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getCompany() == null) {
            errors.add(new FieldError("company", "must not be null", "NotNull"));
        }
        if (entity.getLocation() == null) {
            errors.add(new FieldError("location", "must not be null", "NotNull"));
        }
        if (entity.getFromDate() == null) {
            errors.add(new FieldError("fromDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsCurrent() == null) {
            errors.add(new FieldError("isCurrent", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() != null && !workExperienceRepository.existsPersonId(entity.getPersonId())) {
            errors.add(new FieldError("personId", "person does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueWorkExperience(WorkExperienceEntity entity) {

    }
}
