package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.NoticeBackgroundEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.NoticeBackgroundMapper;
import com.mysms.comms.repository.NoticeBackgroundRepository;
import com.mysms.comms.api.model.NoticeBackground;
import com.mysms.comms.api.model.NoticeBackgroundUpdate;
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
 * Business service of comms.notice_background: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class NoticeBackgroundService extends AppTransactionManager {

    public static final String AGGREGATE = NoticeBackgroundEntity.TABLE;
    static final String PERMISSION_LIST = "comms.notice_background.read";
    static final String PERMISSION_GET = "comms.notice_background.read";
    static final String PERMISSION_CREATE = "comms.notice_background.create";
    static final String PERMISSION_UPDATE = "comms.notice_background.update";
    static final String PERMISSION_DELETE = "comms.notice_background.delete";


    private final NoticeBackgroundRepository repository;
    private final NoticeBackgroundMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public NoticeBackgroundService(NoticeBackgroundRepository repository, NoticeBackgroundMapper mapper,
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

    public NoticeBackground getNoticeBackground(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<NoticeBackground> listNoticeBackgrounds(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NoticeBackgroundRepository.SORT_FIELDS,
                        NoticeBackgroundRepository.DEFAULT_ORDER,
                NoticeBackgroundRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NoticeBackgroundRepository.FILTER_FIELDS)
                .and(NoticeBackgroundRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<NoticeBackgroundEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NoticeBackground createNoticeBackground(NoticeBackground request, boolean validateOnly) {
        validator.validate(request);
        NoticeBackgroundEntity entity = mapper.toEntity(request);
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
            NoticeBackgroundEntity stored = repository.insert(entity);
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
    public NoticeBackground updateNoticeBackground(UUID id, String ifMatch, NoticeBackgroundUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            NoticeBackgroundEntity entity = loadVisible(scope, id);
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
    public void deleteNoticeBackground(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            NoticeBackgroundEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByNoticeBackgroundId(id)) {
                throw new InvalidStateException(
                        "The notice background is still referenced by comms.notice rows; remove those first.");
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
    private NoticeBackgroundEntity loadVisible(DataScope scope, UUID id) {
        NoticeBackgroundEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(NoticeBackgroundEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(NoticeBackgroundEntity entity, NoticeBackgroundUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("usage", changes.getUsage(), v -> entity.setUsage(v == null ? null : v.getValue()));
        patch.apply("imageFileId", changes.getImageFileId(), entity::setImageFileId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(NoticeBackgroundEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getUsage() == null) {
            errors.add(new FieldError("usage", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(NoticeBackgroundEntity entity) {

    }
}
