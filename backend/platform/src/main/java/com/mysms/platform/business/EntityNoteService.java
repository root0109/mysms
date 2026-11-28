package com.mysms.platform.business;

import com.mysms.authclient.DataScope;
import com.mysms.platform.api.model.EntityNote;
import com.mysms.platform.api.model.EntityNoteUpdate;
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
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.EntityNoteEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.EntityNoteMapper;
import com.mysms.platform.repository.EntityNoteRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.entity_note: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class EntityNoteService extends AppTransactionManager {

    public static final String AGGREGATE = EntityNoteEntity.TABLE;
    static final String PERMISSION_LIST = "platform.entity_note.read";
    static final String PERMISSION_GET = "platform.entity_note.read";
    static final String PERMISSION_CREATE = "platform.entity_note.create";
    static final String PERMISSION_UPDATE = "platform.entity_note.update";
    static final String PERMISSION_DELETE = "platform.entity_note.delete";


    private final EntityNoteRepository repository;
    private final EntityNoteMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public EntityNoteService(EntityNoteRepository repository, EntityNoteMapper mapper,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public EntityNote getEntityNote(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<EntityNote> listEntityNotes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, EntityNoteRepository.SORT_FIELDS, EntityNoteRepository.DEFAULT_ORDER,
                EntityNoteRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EntityNoteRepository.FILTER_FIELDS)
                .and(EntityNoteRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<EntityNoteEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EntityNote createEntityNote(EntityNote request, boolean validateOnly) {
        validator.validate(request);
        EntityNoteEntity entity = mapper.toEntity(request);
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
            EntityNoteEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                    PlatformEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EntityNote updateEntityNote(UUID id, String ifMatch, EntityNoteUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            EntityNoteEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, id, PlatformEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEntityNote(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            EntityNoteEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, PlatformEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EntityNoteEntity loadVisible(DataScope scope, UUID id) {
        EntityNoteEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(EntityNoteEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(EntityNoteEntity entity, EntityNoteUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("entityType", changes.getEntityType(), entity::setEntityType);
        patch.apply("entityId", changes.getEntityId(), entity::setEntityId);
        patch.apply("noteText", changes.getNoteText(), entity::setNoteText);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(EntityNoteEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getEntityType() == null) {
            errors.add(new FieldError("entityType", "must not be null", "NotNull"));
        }
        if (entity.getEntityId() == null) {
            errors.add(new FieldError("entityId", "must not be null", "NotNull"));
        }
        if (entity.getNoteText() == null) {
            errors.add(new FieldError("noteText", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(EntityNoteEntity entity) {

    }
}
