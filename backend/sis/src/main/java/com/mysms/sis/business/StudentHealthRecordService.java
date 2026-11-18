package com.mysms.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.sis.api.model.StudentHealthRecord;
import com.mysms.sis.api.model.StudentHealthRecordUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.StudentHealthRecordEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.StudentHealthRecordMapper;
import com.mysms.sis.repository.StudentHealthRecordRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.student_health_record: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class StudentHealthRecordService extends AppTransactionManager {

    public static final String AGGREGATE = StudentHealthRecordEntity.TABLE;
    static final String PERMISSION_LIST = "sis.student_health_record.read";
    static final String PERMISSION_GET = "sis.student_health_record.read";
    static final String PERMISSION_CREATE = "sis.student_health_record.create";
    static final String PERMISSION_UPDATE = "sis.student_health_record.update";
    static final String PERMISSION_DELETE = "sis.student_health_record.delete";
    /** The contract's StudentHealthRecordUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("studentId", "academicYearId", "heightCm", "weightKg", "medicalConditions", "medicationNeeds",
            "medicationAllergies", "foodAllergies", "otherAllergies", "otherMedicalInfo");


    private final StudentHealthRecordRepository repository;
    private final StudentHealthRecordMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StudentHealthRecordService(StudentHealthRecordRepository repository, StudentHealthRecordMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public StudentHealthRecord getStudentHealthRecord(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StudentHealthRecord> listStudentHealthRecords(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StudentHealthRecordRepository.SORT_FIELDS,
                        StudentHealthRecordRepository.DEFAULT_ORDER,
                StudentHealthRecordRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StudentHealthRecordRepository.FILTER_FIELDS)
                .and(StudentHealthRecordRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StudentHealthRecordEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StudentHealthRecord createStudentHealthRecord(StudentHealthRecord request, boolean validateOnly) {
        validator.validate(request);
        StudentHealthRecordEntity entity = mapper.toEntity(request);
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
            StudentHealthRecordEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StudentHealthRecord updateStudentHealthRecord(UUID id, String ifMatch, StudentHealthRecordUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StudentHealthRecordEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
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
    public void deleteStudentHealthRecord(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StudentHealthRecordEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StudentHealthRecordEntity loadVisible(DataScope scope, UUID id) {
        StudentHealthRecordEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the sis.student it belongs to). */
    private Optional<UUID> institutionOf(StudentHealthRecordEntity entity) {
        return repository.institutionOf(entity.getStudentId());
    }

    /**
     * Applies the selected StudentHealthRecordUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(StudentHealthRecordEntity entity, StudentHealthRecordUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "heightCm" -> entity.setHeightCm(WireValues.decimal(changes.getHeightCm()));
                case "weightKg" -> entity.setWeightKg(WireValues.decimal(changes.getWeightKg()));
                case "medicalConditions" -> entity.setMedicalConditions(changes.getMedicalConditions());
                case "medicationNeeds" -> entity.setMedicationNeeds(changes.getMedicationNeeds());
                case "medicationAllergies" -> entity.setMedicationAllergies(changes.getMedicationAllergies());
                case "foodAllergies" -> entity.setFoodAllergies(changes.getFoodAllergies());
                case "otherAllergies" -> entity.setOtherAllergies(changes.getOtherAllergies());
                case "otherMedicalInfo" -> entity.setOtherMedicalInfo(changes.getOtherMedicalInfo());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StudentHealthRecordEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("heightCm", WireValues.decimal(entity.getHeightCm()), 5, 2, errors);
        WireValues.checkDecimal("weightKg", WireValues.decimal(entity.getWeightKg()), 5, 2, errors);
        if (entity.getStudentId() != null && !repository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StudentHealthRecordEntity entity) {
        if (entity.getAcademicYearId() != null && repository.takenStudentIdAcademicYearId1(entity)) {
            throw new DuplicateResourceException(
                    "A student health record with the same student id and academic year id already exists.");
        }
    }
}
