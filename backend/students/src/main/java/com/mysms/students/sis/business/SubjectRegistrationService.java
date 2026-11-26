package com.mysms.students.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
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
import com.mysms.core.web.Patches;
import com.mysms.students.sis.api.model.RejectSubjectRegistrationRequest;
import com.mysms.students.sis.api.model.ResetSubjectRegistrationRequest;
import com.mysms.students.sis.api.model.SubjectRegistration;
import com.mysms.students.sis.api.model.SubjectRegistrationLine;
import com.mysms.students.sis.api.model.SubjectRegistrationLineUpdate;
import com.mysms.students.sis.api.model.SubjectRegistrationUpdate;
import com.mysms.students.sis.api.model.SubmitSubjectRegistrationRequest;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.config.SisEventsConfig;
import com.mysms.students.sis.domain.SubjectRegistrationEntity;
import com.mysms.students.sis.domain.SubjectRegistrationLineEntity;
import com.mysms.students.sis.events.SisEvents;
import com.mysms.students.sis.mapper.SubjectRegistrationLineMapper;
import com.mysms.students.sis.mapper.SubjectRegistrationMapper;
import com.mysms.students.sis.repository.SubjectRegistrationLineRepository;
import com.mysms.students.sis.repository.SubjectRegistrationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.subject_registration: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the SubjectRegistration aggregate: SubjectRegistrationLine.
 */
@Service
public class SubjectRegistrationService extends AppTransactionManager {

    public static final String AGGREGATE = SubjectRegistrationEntity.TABLE;
    static final String PERMISSION_LIST = "sis.subject_registration.read";
    static final String PERMISSION_GET = "sis.subject_registration.read";
    static final String PERMISSION_CREATE = "sis.subject_registration.create";
    static final String PERMISSION_UPDATE = "sis.subject_registration.update";
    static final String PERMISSION_DELETE = "sis.subject_registration.delete";
    static final String PERMISSION_SUBMIT_SUBJECT_REGISTRATION = "sis.subject_registration.submit_subject_registration";
    static final String PERMISSION_REJECT_SUBJECT_REGISTRATION = "sis.subject_registration.reject_subject_registration";
    static final String PERMISSION_RESET_SUBJECT_REGISTRATION = "sis.subject_registration.reset_subject_registration";

    public static final String SUBJECT_REGISTRATION_LINE_AGGREGATE = SubjectRegistrationLineEntity.TABLE;
    static final String SUBJECT_REGISTRATION_LINE_PERMISSION_LIST = "sis.subject_registration_line.read";
    static final String SUBJECT_REGISTRATION_LINE_PERMISSION_GET = "sis.subject_registration_line.read";
    static final String SUBJECT_REGISTRATION_LINE_PERMISSION_CREATE = "sis.subject_registration_line.create";
    static final String SUBJECT_REGISTRATION_LINE_PERMISSION_UPDATE = "sis.subject_registration_line.update";
    static final String SUBJECT_REGISTRATION_LINE_PERMISSION_DELETE = "sis.subject_registration_line.delete";

    private final SubjectRegistrationRepository repository;
    private final SubjectRegistrationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final SubjectRegistrationLineRepository subjectRegistrationLineRepository;
    private final SubjectRegistrationLineMapper subjectRegistrationLineMapper;

    public SubjectRegistrationService(SubjectRegistrationRepository repository,
            SubjectRegistrationMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            SubjectRegistrationLineRepository subjectRegistrationLineRepository,
            SubjectRegistrationLineMapper subjectRegistrationLineMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.subjectRegistrationLineRepository = subjectRegistrationLineRepository;
        this.subjectRegistrationLineMapper = subjectRegistrationLineMapper;
    }

    public SubjectRegistration getSubjectRegistration(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<SubjectRegistration> listSubjectRegistrations(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SubjectRegistrationRepository.SORT_FIELDS,
                        SubjectRegistrationRepository.DEFAULT_ORDER,
                SubjectRegistrationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SubjectRegistrationRepository.FILTER_FIELDS)
                .and(SubjectRegistrationRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<SubjectRegistrationEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SubjectRegistration createSubjectRegistration(SubjectRegistration request, boolean validateOnly) {
        validator.validate(request);
        SubjectRegistrationEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            SubjectRegistrationEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public SubjectRegistration updateSubjectRegistration(UUID id, String ifMatch, SubjectRegistrationUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            SubjectRegistrationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSubjectRegistration(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            SubjectRegistrationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedBySubjectRegistrationLineSubjectRegistrationId(id)) {
                throw new InvalidStateException(
                        "The subject registration is still referenced by sis.subject_registration_line rows; remove "
                                + "those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /**
     * sis.SubmitSubjectRegistration: Submit for approval. Allowed while status is draft, for a caller who can see and
     * write the row; otherwise 409 and nothing changes.
     */
    public SubjectRegistration submitSubjectRegistration(UUID id, SubmitSubjectRegistrationRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SUBMIT_SUBJECT_REGISTRATION);
        return doInWriteTransaction(() -> {
            SubjectRegistrationEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The subject registration cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.submitSubjectRegistration(id, entity.getRowVersion(), now, Actor.current()
                    .orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "submit", List.of("status"), null);
            outbox.publish(AGGREGATE + ".submit", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * sis.RejectSubjectRegistration: Reject. Allowed while status is submitted, for a caller who can see and write the
     * row; otherwise 409 and nothing changes.
     */
    public SubjectRegistration rejectSubjectRegistration(UUID id, RejectSubjectRegistrationRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_SUBJECT_REGISTRATION);
        return doInWriteTransaction(() -> {
            SubjectRegistrationEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("submitted").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The subject registration cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectSubjectRegistration(id, entity.getRowVersion(), now, Actor.current()
                    .orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reject", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * sis.ResetSubjectRegistration: Back to draft. Allowed while status is rejected, for a caller who can see and write
     * the row; otherwise 409 and nothing changes.
     */
    public SubjectRegistration resetSubjectRegistration(UUID id, ResetSubjectRegistrationRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_SUBJECT_REGISTRATION);
        return doInWriteTransaction(() -> {
            SubjectRegistrationEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("rejected").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The subject registration cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetSubjectRegistration(id, entity.getRowVersion(), now, Actor.current()
                    .orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SubjectRegistrationEntity loadVisible(DataScope scope, UUID id) {
        SubjectRegistrationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the sis.student it belongs to). */
    private Optional<UUID> institutionOf(SubjectRegistrationEntity entity) {
        return repository.institutionOf(entity.getStudentId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(SubjectRegistrationEntity entity, SubjectRegistrationUpdate changes) {
        Patches patch = new Patches();
        patch.apply("studentId", changes.getStudentId(), entity::setStudentId);
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("batchId", changes.getBatchId(), entity::setBatchId);
        patch.apply("referenceNo", changes.getReferenceNo(), entity::setReferenceNo);
        patch.apply("minUnitLoad", changes.getMinUnitLoad(), v -> entity.setMinUnitLoad(WireValues.decimal(v)));
        patch.apply("maxUnitLoad", changes.getMaxUnitLoad(), v -> entity.setMaxUnitLoad(WireValues.decimal(v)));
        patch.apply("readByStudent", changes.getReadByStudent(), entity::setReadByStudent);
        patch.apply("readByGuardian", changes.getReadByGuardian(), entity::setReadByGuardian);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(SubjectRegistrationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getProgramEnrollmentId() == null) {
            errors.add(new FieldError("programEnrollmentId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("minUnitLoad", WireValues.decimal(entity.getMinUnitLoad()), 6, 2, errors);
        WireValues.checkDecimal("maxUnitLoad", WireValues.decimal(entity.getMaxUnitLoad()), 6, 2, errors);
        if (!creating && entity.getReadByStudent() == null) {
            errors.add(new FieldError("readByStudent", "must not be null", "NotNull"));
        }
        if (!creating && entity.getReadByGuardian() == null) {
            errors.add(new FieldError("readByGuardian", "must not be null", "NotNull"));
        }
        if (entity.getProgramEnrollmentId() != null
                && !repository.existsProgramEnrollmentId(entity.getProgramEnrollmentId())) {
            errors.add(new FieldError("programEnrollmentId", "program enrollment does not exist", "NOT_FOUND"));
        }
        if (entity.getStudentId() != null && !repository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(SubjectRegistrationEntity entity) {
        if (repository.takenProgramEnrollmentIdAcademicYearIdAcademicTermIdSlot(entity)) {
            throw new DuplicateResourceException(
                    "A subject registration with the same program enrollment id and academic year id and academic "
                            + "term id already exists.");
        }
    }

    // ---- SubjectRegistrationLine: a record of the SubjectRegistration aggregate ----

    public SubjectRegistrationLine getSubjectRegistrationLine(UUID subjectRegistrationId, UUID id) {
        return doInReadTransaction(() -> {
            return subjectRegistrationLineMapper.toDto(loadVisibleSubjectRegistrationLine(DataScope.current(
                    SUBJECT_REGISTRATION_LINE_PERMISSION_GET), subjectRegistrationId, id));
        });
    }

    public Page<SubjectRegistrationLine> listSubjectRegistrationLines(UUID subjectRegistrationId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SubjectRegistrationLineRepository.SORT_FIELDS,
                        SubjectRegistrationLineRepository.DEFAULT_ORDER,
                SubjectRegistrationLineRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SubjectRegistrationLineRepository.FILTER_FIELDS)
                .and(SubjectRegistrationLineRepository.ofParent(subjectRegistrationId))
                        .and(SubjectRegistrationLineRepository.scope(DataScope.current(
                                SUBJECT_REGISTRATION_LINE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + subjectRegistrationId, clock);
        return doInReadTransaction(() -> {
            Page<SubjectRegistrationLineEntity> rows = subjectRegistrationLineRepository.list(page, where);
            return rows.map(subjectRegistrationLineMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SubjectRegistrationLine createSubjectRegistrationLine(UUID subjectRegistrationId,
            SubjectRegistrationLine request, boolean validateOnly) {
        validator.validate(request);
        SubjectRegistrationLineEntity entity = subjectRegistrationLineMapper.toEntity(request);
        entity.setSubjectRegistrationId(subjectRegistrationId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(SUBJECT_REGISTRATION_LINE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!subjectRegistrationLineRepository.existsSubjectRegistrationId(entity.getSubjectRegistrationId())) {
                throw new ResourceNotFoundException("sis.subject_registration", entity.getSubjectRegistrationId());
            }
            if (!scope.canRead(institutionOfSubjectRegistrationLine(entity))) {
                throw new ResourceNotFoundException("sis.subject_registration", entity.getSubjectRegistrationId());
            }
            assertValidSubjectRegistrationLine(entity, true);
            scope.requireWritable(institutionOfSubjectRegistrationLine(entity));
            assertUniqueSubjectRegistrationLine(entity);
            if (validateOnly) {
                return subjectRegistrationLineMapper.toDto(entity);
            }
            SubjectRegistrationLineEntity stored = subjectRegistrationLineRepository.insert(entity);
            audit.recordCreate(SUBJECT_REGISTRATION_LINE_AGGREGATE, stored.getId());
            outbox.publish(SUBJECT_REGISTRATION_LINE_AGGREGATE + SisEvents.CREATED, SUBJECT_REGISTRATION_LINE_AGGREGATE,
                    stored.getId(), SisEvents.payload(stored.getId()));
            return subjectRegistrationLineMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public SubjectRegistrationLine updateSubjectRegistrationLine(UUID subjectRegistrationId, UUID id, String ifMatch,
            SubjectRegistrationLineUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SUBJECT_REGISTRATION_LINE_PERMISSION_UPDATE);
            SubjectRegistrationLineEntity entity = loadVisibleSubjectRegistrationLine(scope, subjectRegistrationId, id);
            scope.requireWritable(institutionOfSubjectRegistrationLine(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applySubjectRegistrationLine(entity, changes);
            validator.validate(subjectRegistrationLineMapper.toDto(entity));
            assertValidSubjectRegistrationLine(entity, false);
            scope.requireWritable(institutionOfSubjectRegistrationLine(entity));
            assertUniqueSubjectRegistrationLine(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return subjectRegistrationLineMapper.toDto(entity);
            }
            if (subjectRegistrationLineRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(SUBJECT_REGISTRATION_LINE_AGGREGATE, id, fields);
            outbox.publish(SUBJECT_REGISTRATION_LINE_AGGREGATE + SisEvents.UPDATED, SUBJECT_REGISTRATION_LINE_AGGREGATE, id,
                    SisEvents.payload(id));
            return subjectRegistrationLineMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSubjectRegistrationLine(UUID subjectRegistrationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SUBJECT_REGISTRATION_LINE_PERMISSION_DELETE);
            SubjectRegistrationLineEntity entity = loadVisibleSubjectRegistrationLine(scope, subjectRegistrationId, id);
            scope.requireWritable(institutionOfSubjectRegistrationLine(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (subjectRegistrationLineRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(SUBJECT_REGISTRATION_LINE_AGGREGATE, id);
            outbox.publish(SUBJECT_REGISTRATION_LINE_AGGREGATE + SisEvents.DELETED, SUBJECT_REGISTRATION_LINE_AGGREGATE, id,
                    SisEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SubjectRegistrationLineEntity loadVisibleSubjectRegistrationLine(DataScope scope, UUID subjectRegistrationId,
            UUID id) {
        SubjectRegistrationLineEntity entity = subjectRegistrationLineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT_REGISTRATION_LINE_AGGREGATE, id));
        if (!scope.canRead(institutionOfSubjectRegistrationLine(entity))
                || !subjectRegistrationId.equals(entity.getSubjectRegistrationId())) {
            throw new ResourceNotFoundException(SUBJECT_REGISTRATION_LINE_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the sis.subject_registration → sis.student it belongs to).
     */
    private Optional<UUID> institutionOfSubjectRegistrationLine(SubjectRegistrationLineEntity entity) {
        return subjectRegistrationLineRepository.institutionOf(entity.getSubjectRegistrationId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applySubjectRegistrationLine(SubjectRegistrationLineEntity entity,
            SubjectRegistrationLineUpdate changes) {
        Patches patch = new Patches();
        patch.apply("subjectId", changes.getSubjectId(), entity::setSubjectId);
        patch.apply("selectionType", changes.getSelectionType(), v -> entity.setSelectionType(v == null ? null : v.getValue()));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidSubjectRegistrationLine(SubjectRegistrationLineEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseSubjectId() == null) {
            errors.add(new FieldError("courseSubjectId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectRegistrationId() == null) {
            errors.add(new FieldError("subjectRegistrationId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getSelectionType() == null) {
            errors.add(new FieldError("selectionType", "must not be null", "NotNull"));
        }
        if (entity.getSubjectRegistrationId() != null
                && !subjectRegistrationLineRepository.existsSubjectRegistrationId(entity.getSubjectRegistrationId())) {
            errors.add(new FieldError("subjectRegistrationId", "subject registration does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueSubjectRegistrationLine(SubjectRegistrationLineEntity entity) {
        if (subjectRegistrationLineRepository.takenSubjectRegistrationIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A subject registration line with the same subject registration id and subject id already exists.");
        }
    }
}
