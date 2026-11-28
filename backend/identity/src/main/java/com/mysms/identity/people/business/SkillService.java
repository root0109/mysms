package com.mysms.identity.people.business;

import com.mysms.authclient.DataScope;
import com.mysms.identity.people.api.model.Skill;
import com.mysms.identity.people.api.model.SkillUpdate;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.identity.people.config.PeopleDatabaseConfig;
import com.mysms.identity.people.config.PeopleEventsConfig;
import com.mysms.identity.people.domain.SkillEntity;
import com.mysms.identity.people.events.PeopleEvents;
import com.mysms.identity.people.mapper.SkillMapper;
import com.mysms.identity.people.repository.SkillRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of people.skill: audit and domain event in the same transaction as every change; rows outside the
 * caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class SkillService extends AppTransactionManager {

    public static final String AGGREGATE = SkillEntity.TABLE;
    static final String PERMISSION_LIST = "people.skill.read";
    static final String PERMISSION_GET = "people.skill.read";
    static final String PERMISSION_CREATE = "people.skill.create";
    static final String PERMISSION_UPDATE = "people.skill.update";
    static final String PERMISSION_DELETE = "people.skill.delete";


    private final SkillRepository repository;
    private final SkillMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public SkillService(SkillRepository repository, SkillMapper mapper,
            @Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PeopleDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PeopleEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PeopleEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Skill getSkill(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Skill> listSkills(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, SkillRepository.SORT_FIELDS, SkillRepository.DEFAULT_ORDER,
                SkillRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SkillRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<SkillEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Skill createSkill(Skill request, boolean validateOnly) {
        validator.validate(request);
        SkillEntity entity = mapper.toEntity(request);
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
            SkillEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PeopleEvents.CREATED, AGGREGATE, stored.getId(),
                    PeopleEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Skill updateSkill(UUID id, String ifMatch, SkillUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            SkillEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + PeopleEvents.UPDATED, AGGREGATE, id, PeopleEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSkill(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            SkillEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByPersonSkillSkillId(id)) {
                throw new InvalidStateException(
                        "The skill is still referenced by people.person_skill rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PeopleEvents.DELETED, AGGREGATE, id, PeopleEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SkillEntity loadVisible(DataScope scope, UUID id) {
        SkillEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(SkillEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(SkillEntity entity, SkillUpdate changes) {
        Patches patch = new Patches();
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("name", changes.getName(), entity::setName);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(SkillEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(SkillEntity entity) {
        if (repository.takenName1(entity)) {
            throw new DuplicateResourceException("A skill with the same name already exists.");
        }
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A skill with the same code already exists.");
        }
    }
}
