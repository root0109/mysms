package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.MagazineEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.MagazineMapper;
import com.mysms.comms.repository.MagazineRepository;
import com.mysms.comms.api.model.Magazine;
import com.mysms.comms.api.model.MagazineUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
 * Business service of comms.magazine: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class MagazineService extends AppTransactionManager {

    public static final String AGGREGATE = MagazineEntity.TABLE;
    static final String PERMISSION_LIST = "comms.magazine.read";
    static final String PERMISSION_GET = "comms.magazine.read";
    static final String PERMISSION_CREATE = "comms.magazine.create";
    static final String PERMISSION_UPDATE = "comms.magazine.update";
    static final String PERMISSION_DELETE = "comms.magazine.delete";


    private final MagazineRepository repository;
    private final MagazineMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MagazineService(MagazineRepository repository, MagazineMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Magazine getMagazine(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Magazine> listMagazines(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, MagazineRepository.SORT_FIELDS, MagazineRepository.DEFAULT_ORDER,
                MagazineRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MagazineRepository.FILTER_FIELDS)
                .and(MagazineRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<MagazineEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Magazine createMagazine(Magazine request, boolean validateOnly) {
        validator.validate(request);
        MagazineEntity entity = mapper.toEntity(request);
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
            MagazineEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Magazine updateMagazine(UUID id, String ifMatch, MagazineUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            MagazineEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMagazine(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            MagazineEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MagazineEntity loadVisible(DataScope scope, UUID id) {
        MagazineEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(MagazineEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(MagazineEntity entity, MagazineUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("publicationYear", changes.getPublicationYear(), entity::setPublicationYear);
        patch.apply("coverImageFileId", changes.getCoverImageFileId(), entity::setCoverImageFileId);
        patch.apply("documentFileId", changes.getDocumentFileId(), entity::setDocumentFileId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(MagazineEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(MagazineEntity entity) {

    }
}
