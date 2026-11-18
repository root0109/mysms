package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.ExternalToolLink;
import com.mysms.lms.api.model.ExternalToolLinkUpdate;
import com.mysms.lms.api.model.ExternalToolResource;
import com.mysms.lms.api.model.ExternalToolResourceUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ExternalToolLinkEntity;
import com.mysms.lms.domain.ExternalToolResourceEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ExternalToolLinkMapper;
import com.mysms.lms.mapper.ExternalToolResourceMapper;
import com.mysms.lms.repository.ExternalToolLinkRepository;
import com.mysms.lms.repository.ExternalToolResourceRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.external_tool_resource and of the records that exist only as part of a ExternalToolResource
 * (ExternalToolLink): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ExternalToolResourceService extends AppTransactionManager {

    public static final String AGGREGATE = ExternalToolResourceEntity.TABLE;
    static final String PERMISSION_LIST = "lms.external_tool_resource.read";
    static final String PERMISSION_GET = "lms.external_tool_resource.read";
    static final String PERMISSION_CREATE = "lms.external_tool_resource.create";
    static final String PERMISSION_UPDATE = "lms.external_tool_resource.update";
    static final String PERMISSION_DELETE = "lms.external_tool_resource.delete";
    /** The contract's ExternalToolResourceUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("institutionId", "title", "description");

    public static final String EXTERNAL_TOOL_LINK_AGGREGATE = ExternalToolLinkEntity.TABLE;
    static final String EXTERNAL_TOOL_LINK_PERMISSION_LIST = "lms.external_tool_link.read";
    static final String EXTERNAL_TOOL_LINK_PERMISSION_GET = "lms.external_tool_link.read";
    static final String EXTERNAL_TOOL_LINK_PERMISSION_CREATE = "lms.external_tool_link.create";
    static final String EXTERNAL_TOOL_LINK_PERMISSION_UPDATE = "lms.external_tool_link.update";
    static final String EXTERNAL_TOOL_LINK_PERMISSION_DELETE = "lms.external_tool_link.delete";
    /** The contract's ExternalToolLinkUpdate fields, in apply order. */
    static final List<String> EXTERNAL_TOOL_LINK_WRITABLE = List.of("externalToolId", "linkRef", "linkTitle");

    private final ExternalToolResourceRepository repository;
    private final ExternalToolResourceMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ExternalToolLinkRepository externalToolLinkRepository;
    private final ExternalToolLinkMapper externalToolLinkMapper;

    public ExternalToolResourceService(ExternalToolResourceRepository repository,
            ExternalToolResourceMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ExternalToolLinkRepository externalToolLinkRepository,
            ExternalToolLinkMapper externalToolLinkMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.externalToolLinkRepository = externalToolLinkRepository;
        this.externalToolLinkMapper = externalToolLinkMapper;
    }

    public ExternalToolResource getExternalToolResource(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ExternalToolResource> listExternalToolResources(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExternalToolResourceRepository.SORT_FIELDS,
                        ExternalToolResourceRepository.DEFAULT_ORDER,
                ExternalToolResourceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExternalToolResourceRepository.FILTER_FIELDS)
                .and(ExternalToolResourceRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ExternalToolResourceEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExternalToolResource createExternalToolResource(ExternalToolResource request, boolean validateOnly) {
        validator.validate(request);
        ExternalToolResourceEntity entity = mapper.toEntity(request);
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
            ExternalToolResourceEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ExternalToolResource updateExternalToolResource(UUID id, String ifMatch, ExternalToolResourceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ExternalToolResourceEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExternalToolResource(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ExternalToolResourceEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByContentItemExternalToolResourceId(id)) {
                throw new InvalidStateException(
                        "The external tool resource is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByExternalToolLinkExternalToolResourceId(id)) {
                throw new InvalidStateException(
                        "The external tool resource is still referenced by lms.external_tool_link rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExternalToolResourceEntity loadVisible(DataScope scope, UUID id) {
        ExternalToolResourceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(ExternalToolResourceEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected ExternalToolResourceUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ExternalToolResourceEntity entity, ExternalToolResourceUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ExternalToolResourceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ExternalToolResourceEntity entity) {

    }

    // ---- ExternalToolLink: a record of the ExternalToolResource aggregate ----

    public ExternalToolLink getExternalToolLink(UUID externalToolResourceId, UUID id) {
        return doInReadTransaction(() -> {
            return externalToolLinkMapper.toDto(loadVisibleExternalToolLink(DataScope.current(
                    EXTERNAL_TOOL_LINK_PERMISSION_GET), externalToolResourceId, id));
        });
    }

    public Page<ExternalToolLink> listExternalToolLinks(UUID externalToolResourceId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExternalToolLinkRepository.SORT_FIELDS,
                        ExternalToolLinkRepository.DEFAULT_ORDER,
                ExternalToolLinkRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExternalToolLinkRepository.FILTER_FIELDS)
                .and(ExternalToolLinkRepository.ofParent(externalToolResourceId))
                        .and(ExternalToolLinkRepository.scope(DataScope.current(EXTERNAL_TOOL_LINK_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + externalToolResourceId, clock);
        return doInReadTransaction(() -> {
            Page<ExternalToolLinkEntity> rows = externalToolLinkRepository.list(page, where);
            return rows.map(externalToolLinkMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExternalToolLink createExternalToolLink(UUID externalToolResourceId, ExternalToolLink request,
            boolean validateOnly) {
        validator.validate(request);
        ExternalToolLinkEntity entity = externalToolLinkMapper.toEntity(request);
        entity.setExternalToolResourceId(externalToolResourceId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EXTERNAL_TOOL_LINK_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!externalToolLinkRepository.existsExternalToolResourceId(entity.getExternalToolResourceId())) {
                throw new ResourceNotFoundException("lms.external_tool_resource", entity.getExternalToolResourceId());
            }
            if (!scope.canRead(institutionOfExternalToolLink(entity))) {
                throw new ResourceNotFoundException("lms.external_tool_resource", entity.getExternalToolResourceId());
            }
            assertValidExternalToolLink(entity, true);
            scope.requireWritable(institutionOfExternalToolLink(entity));
            assertUniqueExternalToolLink(entity);
            if (validateOnly) {
                return externalToolLinkMapper.toDto(entity);
            }
            ExternalToolLinkEntity stored = externalToolLinkRepository.insert(entity);
            audit.recordCreate(EXTERNAL_TOOL_LINK_AGGREGATE, stored.getId());
            outbox.publish(EXTERNAL_TOOL_LINK_AGGREGATE + LmsEvents.CREATED, EXTERNAL_TOOL_LINK_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return externalToolLinkMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ExternalToolLink updateExternalToolLink(UUID externalToolResourceId, UUID id, String ifMatch,
            ExternalToolLinkUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EXTERNAL_TOOL_LINK_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_LINK_PERMISSION_UPDATE);
            ExternalToolLinkEntity entity = loadVisibleExternalToolLink(scope, externalToolResourceId, id);
            scope.requireWritable(institutionOfExternalToolLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyExternalToolLink(entity, changes, fields);
            validator.validate(externalToolLinkMapper.toDto(entity));
            assertValidExternalToolLink(entity, false);
            scope.requireWritable(institutionOfExternalToolLink(entity));
            assertUniqueExternalToolLink(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return externalToolLinkMapper.toDto(entity);
            }
            if (externalToolLinkRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EXTERNAL_TOOL_LINK_AGGREGATE, id, fields);
            outbox.publish(EXTERNAL_TOOL_LINK_AGGREGATE + LmsEvents.UPDATED, EXTERNAL_TOOL_LINK_AGGREGATE, id,
                    LmsEvents.payload(id));
            return externalToolLinkMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExternalToolLink(UUID externalToolResourceId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_LINK_PERMISSION_DELETE);
            ExternalToolLinkEntity entity = loadVisibleExternalToolLink(scope, externalToolResourceId, id);
            scope.requireWritable(institutionOfExternalToolLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (externalToolLinkRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EXTERNAL_TOOL_LINK_AGGREGATE, id);
            outbox.publish(EXTERNAL_TOOL_LINK_AGGREGATE + LmsEvents.DELETED, EXTERNAL_TOOL_LINK_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExternalToolLinkEntity loadVisibleExternalToolLink(DataScope scope, UUID externalToolResourceId, UUID id) {
        ExternalToolLinkEntity entity = externalToolLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXTERNAL_TOOL_LINK_AGGREGATE, id));
        if (!scope.canRead(institutionOfExternalToolLink(entity))
                || !externalToolResourceId.equals(entity.getExternalToolResourceId())) {
            throw new ResourceNotFoundException(EXTERNAL_TOOL_LINK_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.external_tool_resource the row belongs to. */
    private Optional<UUID> institutionOfExternalToolLink(ExternalToolLinkEntity entity) {
        return externalToolLinkRepository.institutionOf(entity.getExternalToolResourceId());
    }

    /** Applies the selected ExternalToolLinkUpdate fields; explicit per field so a mask can clear values. */
    private static void applyExternalToolLink(ExternalToolLinkEntity entity, ExternalToolLinkUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "externalToolId" -> entity.setExternalToolId(changes.getExternalToolId());
                case "linkRef" -> entity.setLinkRef(changes.getLinkRef());
                case "linkTitle" -> entity.setLinkTitle(changes.getLinkTitle());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExternalToolLink(ExternalToolLinkEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExternalToolResourceId() == null) {
            errors.add(new FieldError("externalToolResourceId", "must not be null", "NotNull"));
        }
        if (entity.getLinkRef() == null) {
            errors.add(new FieldError("linkRef", "must not be null", "NotNull"));
        }
        if (entity.getExternalToolId() != null && !externalToolLinkRepository.existsExternalToolId(entity.getExternalToolId())) {
            errors.add(new FieldError("externalToolId", "external tool does not exist", "NOT_FOUND"));
        }
        if (entity.getExternalToolResourceId() != null
                && !externalToolLinkRepository.existsExternalToolResourceId(entity.getExternalToolResourceId())) {
            errors.add(new FieldError("externalToolResourceId", "external tool resource does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExternalToolLink(ExternalToolLinkEntity entity) {

    }
}
