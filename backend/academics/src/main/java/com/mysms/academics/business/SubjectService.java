package com.mysms.academics.business;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.SubjectEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.SubjectMapper;
import com.mysms.academics.repository.SubjectRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.api.model.Subject;
import com.mysms.academics.api.model.SubjectUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of academics.subject: audit and domain event in the same transaction as every change; rows outside
 * the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class SubjectService extends AppTransactionManager {

    public static final String AGGREGATE = SubjectEntity.TABLE;
    static final String PERMISSION_LIST = "academics.subject.read";
    static final String PERMISSION_GET = "academics.subject.read";
    static final String PERMISSION_CREATE = "academics.subject.create";
    static final String PERMISSION_UPDATE = "academics.subject.update";
    static final String PERMISSION_DELETE = "academics.subject.delete";


    private final SubjectRepository repository;
    private final SubjectMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public SubjectService(SubjectRepository repository, SubjectMapper mapper,
            @Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AcademicsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Subject getSubject(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Subject> listSubjects(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, SubjectRepository.SORT_FIELDS, SubjectRepository.DEFAULT_ORDER,
                SubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SubjectRepository.FILTER_FIELDS)
                .and(SubjectRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<SubjectEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Subject createSubject(Subject request, boolean validateOnly) {
        validator.validate(request);
        SubjectEntity entity = mapper.toEntity(request);
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
            SubjectEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Subject updateSubject(UUID id, String ifMatch, SubjectUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            SubjectEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + AcademicsEvents.UPDATED, AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSubject(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            SubjectEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByClassSubjectSubjectId(id)) {
                throw new InvalidStateException(
                        "The subject is still referenced by academics.class_subject rows; remove those first.");
            }
            if (repository.referencedByTeacherSubjectSubjectId(id)) {
                throw new InvalidStateException(
                        "The subject is still referenced by academics.teacher_subject rows; remove those first.");
            }
            if (repository.referencedByCourseSubjectSubjectId(id)) {
                throw new InvalidStateException(
                        "The subject is still referenced by academics.course_subject rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AcademicsEvents.DELETED, AGGREGATE, id, AcademicsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SubjectEntity loadVisible(DataScope scope, UUID id) {
        SubjectEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(SubjectEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(SubjectEntity entity, SubjectUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("departmentId", changes.getDepartmentId(), entity::setDepartmentId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("deliveryType", changes.getDeliveryType(), v -> entity.setDeliveryType(v == null ? null : v.getValue()));
        patch.apply("defaultCategory", changes.getDefaultCategory(),
                v -> entity.setDefaultCategory(v == null ? null : v.getValue()));
        patch.apply("gradeWeightage", changes.getGradeWeightage(), v -> entity.setGradeWeightage(WireValues.decimal(v)));
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(SubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDeliveryType() == null) {
            errors.add(new FieldError("deliveryType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDefaultCategory() == null) {
            errors.add(new FieldError("defaultCategory", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("gradeWeightage", WireValues.decimal(entity.getGradeWeightage()), 6, 2, errors);
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(SubjectEntity entity) {

    }
}
