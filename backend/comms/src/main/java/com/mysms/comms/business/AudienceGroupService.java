package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.AudienceGroup;
import com.mysms.comms.api.model.AudienceGroupMember;
import com.mysms.comms.api.model.AudienceGroupMemberUpdate;
import com.mysms.comms.api.model.AudienceGroupUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.AudienceGroupEntity;
import com.mysms.comms.domain.AudienceGroupMemberEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.AudienceGroupMapper;
import com.mysms.comms.mapper.AudienceGroupMemberMapper;
import com.mysms.comms.repository.AudienceGroupMemberRepository;
import com.mysms.comms.repository.AudienceGroupRepository;
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
import com.mysms.core.web.UpdateMask;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.audience_group (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a AudienceGroup
 * are served here too — AudienceGroupMember.
 */
@Service
public class AudienceGroupService extends AppTransactionManager {

    public static final String AGGREGATE = AudienceGroupEntity.TABLE;
    static final String PERMISSION_LIST = "comms.audience_group.read";
    static final String PERMISSION_GET = "comms.audience_group.read";
    static final String PERMISSION_CREATE = "comms.audience_group.create";
    static final String PERMISSION_UPDATE = "comms.audience_group.update";
    static final String PERMISSION_DELETE = "comms.audience_group.delete";
    /** The contract's AudienceGroupUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("institutionId", "classSectionId", "name", "groupScope", "isActive");

    public static final String AUDIENCE_GROUP_MEMBER_AGGREGATE = AudienceGroupMemberEntity.TABLE;
    static final String AUDIENCE_GROUP_MEMBER_PERMISSION_LIST = "comms.audience_group_member.read";
    static final String AUDIENCE_GROUP_MEMBER_PERMISSION_GET = "comms.audience_group_member.read";
    static final String AUDIENCE_GROUP_MEMBER_PERMISSION_CREATE = "comms.audience_group_member.create";
    static final String AUDIENCE_GROUP_MEMBER_PERMISSION_UPDATE = "comms.audience_group_member.update";
    static final String AUDIENCE_GROUP_MEMBER_PERMISSION_DELETE = "comms.audience_group_member.delete";
    /** The contract's AudienceGroupMemberUpdate fields, in apply order. */
    static final List<String> AUDIENCE_GROUP_MEMBER_WRITABLE = List.of("userId", "memberType");

    private final AudienceGroupRepository repository;
    private final AudienceGroupMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final AudienceGroupMemberRepository audienceGroupMemberRepository;
    private final AudienceGroupMemberMapper audienceGroupMemberMapper;

    public AudienceGroupService(AudienceGroupRepository repository,
            AudienceGroupMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            AudienceGroupMemberRepository audienceGroupMemberRepository,
            AudienceGroupMemberMapper audienceGroupMemberMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.audienceGroupMemberRepository = audienceGroupMemberRepository;
        this.audienceGroupMemberMapper = audienceGroupMemberMapper;
    }

    public AudienceGroup getAudienceGroup(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AudienceGroup> listAudienceGroups(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AudienceGroupRepository.SORT_FIELDS, AudienceGroupRepository.DEFAULT_ORDER,
                AudienceGroupRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AudienceGroupRepository.FILTER_FIELDS)
                .and(AudienceGroupRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AudienceGroupEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AudienceGroup createAudienceGroup(AudienceGroup request, boolean validateOnly) {
        validator.validate(request);
        AudienceGroupEntity entity = mapper.toEntity(request);
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
            AudienceGroupEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AudienceGroup updateAudienceGroup(UUID id, String ifMatch, AudienceGroupUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AudienceGroupEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAudienceGroup(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AudienceGroupEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAudienceGroupMemberGroupId(id)) {
                throw new InvalidStateException(
                        "The audience group is still referenced by comms.audience_group_member rows; remove those "
                                + "first.");
            }
            if (repository.referencedByTaskAssigneeGroupId(id)) {
                throw new InvalidStateException(
                        "The audience group is still referenced by comms.task_assignee rows; remove those first.");
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
    private AudienceGroupEntity loadVisible(DataScope scope, UUID id) {
        AudienceGroupEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(AudienceGroupEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected AudienceGroupUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(AudienceGroupEntity entity, AudienceGroupUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "name" -> entity.setName(changes.getName());
                case "groupScope" -> entity.setGroupScope(Optional.ofNullable(changes.getGroupScope())
                        .map(v -> v.getValue()).orElse(null));
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AudienceGroupEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getGroupScope() == null) {
            errors.add(new FieldError("groupScope", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AudienceGroupEntity entity) {

    }

    // ---- AudienceGroupMember: a record of the AudienceGroup aggregate ----

    public AudienceGroupMember getAudienceGroupMember(UUID groupId, UUID id) {
        return doInReadTransaction(() -> {
            return audienceGroupMemberMapper.toDto(loadVisibleAudienceGroupMember(DataScope.current(
                    AUDIENCE_GROUP_MEMBER_PERMISSION_GET), groupId, id));
        });
    }

    public Page<AudienceGroupMember> listAudienceGroupMembers(UUID groupId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AudienceGroupMemberRepository.SORT_FIELDS,
                        AudienceGroupMemberRepository.DEFAULT_ORDER,
                AudienceGroupMemberRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AudienceGroupMemberRepository.FILTER_FIELDS)
                .and(AudienceGroupMemberRepository.ofParent(groupId))
                        .and(AudienceGroupMemberRepository.scope(DataScope.current(AUDIENCE_GROUP_MEMBER_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + groupId, clock);
        return doInReadTransaction(() -> {
            Page<AudienceGroupMemberEntity> rows = audienceGroupMemberRepository.list(page, where);
            return rows.map(audienceGroupMemberMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AudienceGroupMember createAudienceGroupMember(UUID groupId, AudienceGroupMember request,
            boolean validateOnly) {
        validator.validate(request);
        AudienceGroupMemberEntity entity = audienceGroupMemberMapper.toEntity(request);
        entity.setGroupId(groupId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(AUDIENCE_GROUP_MEMBER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!audienceGroupMemberRepository.existsGroupId(entity.getGroupId())) {
                throw new ResourceNotFoundException("comms.audience_group", entity.getGroupId());
            }
            if (!scope.canRead(institutionOfAudienceGroupMember(entity))) {
                throw new ResourceNotFoundException("comms.audience_group", entity.getGroupId());
            }
            assertValidAudienceGroupMember(entity, true);
            scope.requireWritable(institutionOfAudienceGroupMember(entity));
            assertUniqueAudienceGroupMember(entity);
            if (validateOnly) {
                return audienceGroupMemberMapper.toDto(entity);
            }
            AudienceGroupMemberEntity stored = audienceGroupMemberRepository.insert(entity);
            audit.recordCreate(AUDIENCE_GROUP_MEMBER_AGGREGATE, stored.getId());
            outbox.publish(AUDIENCE_GROUP_MEMBER_AGGREGATE + CommsEvents.CREATED, AUDIENCE_GROUP_MEMBER_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return audienceGroupMemberMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AudienceGroupMember updateAudienceGroupMember(UUID groupId, UUID id, String ifMatch,
            AudienceGroupMemberUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, AUDIENCE_GROUP_MEMBER_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(AUDIENCE_GROUP_MEMBER_PERMISSION_UPDATE);
            AudienceGroupMemberEntity entity = loadVisibleAudienceGroupMember(scope, groupId, id);
            scope.requireWritable(institutionOfAudienceGroupMember(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAudienceGroupMember(entity, changes, fields);
            validator.validate(audienceGroupMemberMapper.toDto(entity));
            assertValidAudienceGroupMember(entity, false);
            scope.requireWritable(institutionOfAudienceGroupMember(entity));
            assertUniqueAudienceGroupMember(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return audienceGroupMemberMapper.toDto(entity);
            }
            if (audienceGroupMemberRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AUDIENCE_GROUP_MEMBER_AGGREGATE, id, fields);
            outbox.publish(AUDIENCE_GROUP_MEMBER_AGGREGATE + CommsEvents.UPDATED, AUDIENCE_GROUP_MEMBER_AGGREGATE, id,
                    CommsEvents.payload(id));
            return audienceGroupMemberMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAudienceGroupMember(UUID groupId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(AUDIENCE_GROUP_MEMBER_PERMISSION_DELETE);
            AudienceGroupMemberEntity entity = loadVisibleAudienceGroupMember(scope, groupId, id);
            scope.requireWritable(institutionOfAudienceGroupMember(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (audienceGroupMemberRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AUDIENCE_GROUP_MEMBER_AGGREGATE, id);
            outbox.publish(AUDIENCE_GROUP_MEMBER_AGGREGATE + CommsEvents.DELETED, AUDIENCE_GROUP_MEMBER_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AudienceGroupMemberEntity loadVisibleAudienceGroupMember(DataScope scope, UUID groupId, UUID id) {
        AudienceGroupMemberEntity entity = audienceGroupMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AUDIENCE_GROUP_MEMBER_AGGREGATE, id));
        if (!scope.canRead(institutionOfAudienceGroupMember(entity)) || !groupId.equals(entity.getGroupId())) {
            throw new ResourceNotFoundException(AUDIENCE_GROUP_MEMBER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the comms.audience_group it belongs to). */
    private Optional<UUID> institutionOfAudienceGroupMember(AudienceGroupMemberEntity entity) {
        return audienceGroupMemberRepository.institutionOf(entity.getGroupId());
    }

    /**
     * Applies the selected AudienceGroupMemberUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyAudienceGroupMember(AudienceGroupMemberEntity entity, AudienceGroupMemberUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "memberType" -> entity.setMemberType(Optional.ofNullable(changes.getMemberType())
                        .map(v -> v.getValue()).orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAudienceGroupMember(AudienceGroupMemberEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getGroupId() == null) {
            errors.add(new FieldError("groupId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getMemberType() == null) {
            errors.add(new FieldError("memberType", "must not be null", "NotNull"));
        }
        if (entity.getGroupId() != null && !audienceGroupMemberRepository.existsGroupId(entity.getGroupId())) {
            errors.add(new FieldError("groupId", "audience group does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAudienceGroupMember(AudienceGroupMemberEntity entity) {
        if (audienceGroupMemberRepository.takenGroupIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A audience group member with the same group id and user id already exists.");
        }
    }
}
