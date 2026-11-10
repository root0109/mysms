package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.CancelImportExportJobRequest;
import com.mysms.lms.api.model.ImportExportJob;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ImportExportJobEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ImportExportJobMapper;
import com.mysms.lms.repository.ImportExportJobRepository;
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
 * Business service of lms.import_export_job (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class ImportExportJobService extends AppTransactionManager {

    public static final String AGGREGATE = ImportExportJobEntity.TABLE;
    static final String PERMISSION_LIST = "lms.import_export_job.read";
    static final String PERMISSION_GET = "lms.import_export_job.read";
    static final String PERMISSION_CREATE = "lms.import_export_job.create";
    static final String PERMISSION_CANCEL_IMPORT_EXPORT_JOB = "lms.import_export_job.cancel_import_export_job";


    private final ImportExportJobRepository repository;
    private final ImportExportJobMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ImportExportJobService(ImportExportJobRepository repository, ImportExportJobMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public ImportExportJob getImportExportJob(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ImportExportJob> listImportExportJobs(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ImportExportJobRepository.SORT_FIELDS, ImportExportJobRepository.DEFAULT_ORDER,
                ImportExportJobRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ImportExportJobRepository.FILTER_FIELDS)
                .and(ImportExportJobRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ImportExportJobEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ImportExportJob createImportExportJob(ImportExportJob request, boolean validateOnly) {
        validator.validate(request);
        ImportExportJobEntity entity = mapper.toEntity(request);
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
            ImportExportJobEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * lms.CancelImportExportJob: Cancel a pending job. The row must be visible and writable for the caller, and the
     * step is allowed while status is pending; in any other state
     * the answer is 409 and nothing changes.
     */
    public ImportExportJob cancelImportExportJob(UUID id, CancelImportExportJobRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_IMPORT_EXPORT_JOB);
        return doInWriteTransaction(() -> {
            ImportExportJobEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("pending").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The import export job cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelImportExportJob(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), null);
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ImportExportJobEntity loadVisible(DataScope scope, UUID id) {
        ImportExportJobEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(ImportExportJobEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ImportExportJobEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getDirection() == null) {
            errors.add(new FieldError("direction", "must not be null", "NotNull"));
        }
        if (entity.getPackageFormat() == null) {
            errors.add(new FieldError("packageFormat", "must not be null", "NotNull"));
        }
        if (entity.getScope() == null) {
            errors.add(new FieldError("scope", "must not be null", "NotNull"));
        }
        if (entity.getRequestedBy() == null) {
            errors.add(new FieldError("requestedBy", "must not be null", "NotNull"));
        }
        if (entity.getRequestedOn() == null) {
            errors.add(new FieldError("requestedOn", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getTargetChapterId() != null && !repository.existsTargetChapterId(entity.getTargetChapterId())) {
            errors.add(new FieldError("targetChapterId", "chapter does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ImportExportJobEntity entity) {

    }
}
