package com.mysms.lms.business;

import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.OptionalInt;
import java.util.Objects;
import java.util.HashSet;
import com.mysms.lms.repository.ContentItemRepository;
import com.mysms.lms.domain.ContentItemEntity;
import com.mysms.lms.api.model.SetReleaseRulesEnabledRequest;
import com.mysms.lms.api.model.ReorderReleaseRulesRequest;
import com.mysms.lms.api.model.ReleaseRuleList;
import com.mysms.lms.api.model.CopyReleaseRuleRequest;
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
import com.mysms.lms.api.model.ReleaseRule;
import com.mysms.lms.api.model.ReleaseRuleCondition;
import com.mysms.lms.api.model.ReleaseRuleConditionUpdate;
import com.mysms.lms.api.model.ReleaseRuleTarget;
import com.mysms.lms.api.model.ReleaseRuleTargetUpdate;
import com.mysms.lms.api.model.ReleaseRuleUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ReleaseRuleConditionEntity;
import com.mysms.lms.domain.ReleaseRuleEntity;
import com.mysms.lms.domain.ReleaseRuleTargetEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ReleaseRuleConditionMapper;
import com.mysms.lms.mapper.ReleaseRuleMapper;
import com.mysms.lms.mapper.ReleaseRuleTargetMapper;
import com.mysms.lms.repository.ReleaseRuleConditionRepository;
import com.mysms.lms.repository.ReleaseRuleRepository;
import com.mysms.lms.repository.ReleaseRuleTargetRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.release_rule and of the records that exist only as part of a ReleaseRule
 * (ReleaseRuleCondition, ReleaseRuleTarget): audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ReleaseRuleService extends AppTransactionManager {

    public static final String AGGREGATE = ReleaseRuleEntity.TABLE;
    static final String PERMISSION_LIST = "lms.release_rule.read";
    static final String PERMISSION_GET = "lms.release_rule.read";
    static final String PERMISSION_CREATE = "lms.release_rule.create";
    static final String PERMISSION_UPDATE = "lms.release_rule.update";
    static final String PERMISSION_DELETE = "lms.release_rule.delete";
    static final String PERMISSION_ENABLE = "lms.release_rule.enable_release_rule";
    static final String PERMISSION_DISABLE = "lms.release_rule.disable_release_rule";
    static final String PERMISSION_COPY = "lms.release_rule.copy_release_rule";
    static final String PERMISSION_REORDER = "lms.content_item.reorder_release_rules";
    static final String PERMISSION_SET_ENABLED = "lms.content_item.set_release_rules_enabled";
    /** A rule is copied to a handful of items at a time. */
    static final int MAX_COPIES = 50;
    /** The contract's ReleaseRuleUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("courseId", "cohortId", "sourceContentItemId", "title", "stopIfTrue", "isDisabled",
                    "reassignSource",
            "sortOrder", "copiedFromId");

    public static final String RELEASE_RULE_CONDITION_AGGREGATE = ReleaseRuleConditionEntity.TABLE;
    static final String RELEASE_RULE_CONDITION_PERMISSION_LIST = "lms.release_rule_condition.read";
    static final String RELEASE_RULE_CONDITION_PERMISSION_GET = "lms.release_rule_condition.read";
    static final String RELEASE_RULE_CONDITION_PERMISSION_CREATE = "lms.release_rule_condition.create";
    static final String RELEASE_RULE_CONDITION_PERMISSION_UPDATE = "lms.release_rule_condition.update";
    static final String RELEASE_RULE_CONDITION_PERMISSION_DELETE = "lms.release_rule_condition.delete";
    /** The contract's ReleaseRuleConditionUpdate fields, in apply order. */
    static final List<String> RELEASE_RULE_CONDITION_WRITABLE = List.of("conditionType", "operator", "value", "value2",
            "sortOrder");

    public static final String RELEASE_RULE_TARGET_AGGREGATE = ReleaseRuleTargetEntity.TABLE;
    static final String RELEASE_RULE_TARGET_PERMISSION_LIST = "lms.release_rule_target.read";
    static final String RELEASE_RULE_TARGET_PERMISSION_GET = "lms.release_rule_target.read";
    static final String RELEASE_RULE_TARGET_PERMISSION_CREATE = "lms.release_rule_target.create";
    static final String RELEASE_RULE_TARGET_PERMISSION_UPDATE = "lms.release_rule_target.update";
    static final String RELEASE_RULE_TARGET_PERMISSION_DELETE = "lms.release_rule_target.delete";
    /** The contract's ReleaseRuleTargetUpdate fields, in apply order. */
    static final List<String> RELEASE_RULE_TARGET_WRITABLE = List.of("contentItemId", "chapterId");

    private final ReleaseRuleRepository repository;
    private final ReleaseRuleMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ReleaseRuleConditionRepository releaseRuleConditionRepository;
    private final ReleaseRuleConditionMapper releaseRuleConditionMapper;
    private final ReleaseRuleTargetRepository releaseRuleTargetRepository;
    private final ReleaseRuleTargetMapper releaseRuleTargetMapper;
    private final ContentItemRepository items;

    public ReleaseRuleService(ReleaseRuleRepository repository,
            ReleaseRuleMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ReleaseRuleConditionRepository releaseRuleConditionRepository,
            ReleaseRuleConditionMapper releaseRuleConditionMapper,
            ReleaseRuleTargetRepository releaseRuleTargetRepository,
            ReleaseRuleTargetMapper releaseRuleTargetMapper,
            ContentItemRepository items) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.releaseRuleConditionRepository = releaseRuleConditionRepository;
        this.releaseRuleConditionMapper = releaseRuleConditionMapper;
        this.releaseRuleTargetRepository = releaseRuleTargetRepository;
        this.releaseRuleTargetMapper = releaseRuleTargetMapper;
        this.items = items;
    }

    public ReleaseRule getReleaseRule(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ReleaseRule> listReleaseRules(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ReleaseRuleRepository.SORT_FIELDS, ReleaseRuleRepository.DEFAULT_ORDER,
                ReleaseRuleRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReleaseRuleRepository.FILTER_FIELDS)
                .and(ReleaseRuleRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ReleaseRuleEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ReleaseRule createReleaseRule(ReleaseRule request, boolean validateOnly) {
        validator.validate(request);
        ReleaseRuleEntity entity = mapper.toEntity(request);
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
            ReleaseRuleEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ReleaseRule updateReleaseRule(UUID id, String ifMatch, ReleaseRuleUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ReleaseRuleEntity entity = loadVisible(scope, id);
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
    public void deleteReleaseRule(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ReleaseRuleEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByReleaseRuleCopiedFromId(id)) {
                throw new InvalidStateException(
                        "The release rule is still referenced by lms.release_rule rows; remove those first.");
            }
            if (repository.referencedByReleaseRuleConditionReleaseRuleId(id)) {
                throw new InvalidStateException(
                        "The release rule is still referenced by lms.release_rule_condition rows; remove those first.");
            }
            if (repository.referencedByReleaseRuleTargetReleaseRuleId(id)) {
                throw new InvalidStateException(
                        "The release rule is still referenced by lms.release_rule_target rows; remove those first.");
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
    /** lms.EnableReleaseRule: a disabled rule takes part in releasing again (409 when it is enabled). */
    public ReleaseRule enableReleaseRule(UUID id, String etag) {
        return setEnabled(id, etag, true, PERMISSION_ENABLE);
    }

    /** lms.DisableReleaseRule: an enabled rule stops holding items back (409 when it is disabled). */
    public ReleaseRule disableReleaseRule(UUID id, String etag) {
        return setEnabled(id, etag, false, PERMISSION_DISABLE);
    }

    /** The guarded step on is_disabled: scope, writable, body etag, guard, guarded UPDATE. */
    private ReleaseRule setEnabled(UUID id, String etag, boolean enabled, String permission) {
        DataScope scope = DataScope.current(permission);
        OptionalInt expected = Etags.bodyVersion(etag);
        return doInWriteTransaction(() -> {
            ReleaseRuleEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (Boolean.TRUE.equals(entity.getIsDisabled()) != enabled) {
                throw new InvalidStateException("The release rule cannot be changed this way in its current state.");
            }
            if (repository.setEnabled(id, enabled, entity.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            String step = enabled ? "enable" : "disable";
            audit.recordTransition(AGGREGATE, id, step, List.of("isDisabled"), null);
            outbox.publish(AGGREGATE + "." + step, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.CopyReleaseRule: copies the rule — its conditions and targets — to each listed item of the same course or
     * cohort as its new source (at most 50; an item elsewhere is 422). Each copy remembers the rule it came from and
     * goes last in its source's order.
     */
    public ReleaseRuleList copyReleaseRule(UUID id, CopyReleaseRuleRequest request) {
        validator.validate(request);
        List<UUID> sources = request.getSourceContentItemIds().stream().distinct().toList();
        if (sources.isEmpty() || sources.size() > MAX_COPIES) {
            throw new ValidationFailedException(List.of(new FieldError("sourceContentItemIds", "name 1 to 50 items",
                    "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_COPY);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ReleaseRuleEntity rule = loadVisible(scope, id);
            scope.requireWritable(institutionOf(rule));
            List<ReleaseRuleConditionEntity> conditions = releaseRuleConditionRepository.ofRules(List.of(id));
            List<ReleaseRuleTargetEntity> targets = releaseRuleTargetRepository.ofRule(id);
            List<ReleaseRule> copies = new ArrayList<>();
            for (UUID source : sources) {
                ContentItemEntity item = items.findById(source)
                        .filter(i -> Objects.equals(i.getCourseId(), rule.getCourseId()) && rule.getCohortId() == null
                                || rule.getCohortId() != null && rule.getCohortId().equals(i.getCohortId()))
                        .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError(
                                "sourceContentItemIds", "items must belong to the rule's course or cohort",
                                "NOT_FOUND"))));
                ReleaseRuleEntity copy = ReleaseRuleEntity.builder().tenantId(tenant).courseId(rule.getCourseId())
                        .cohortId(rule.getCohortId()).sourceContentItemId(item.getId()).title(rule.getTitle())
                        .stopIfTrue(rule.getStopIfTrue()).isDisabled(rule.getIsDisabled())
                        .reassignSource(rule.getReassignSource()).sortOrder(repository.ofSource(item.getId()).size())
                        .copiedFromId(rule.getId()).build();
                copy.onCreatingData(Actor.current(), clock);
                ReleaseRuleEntity stored = repository.insert(copy);
                for (ReleaseRuleConditionEntity condition : conditions) {
                    ReleaseRuleConditionEntity c = ReleaseRuleConditionEntity.builder().tenantId(tenant)
                            .releaseRuleId(stored.getId()).conditionType(condition.getConditionType())
                            .operator(condition.getOperator()).value(condition.getValue()).value2(condition.getValue2())
                            .sortOrder(condition.getSortOrder()).build();
                    c.onCreatingData(Actor.current(), clock);
                    releaseRuleConditionRepository.insert(c);
                }
                for (ReleaseRuleTargetEntity target : targets) {
                    ReleaseRuleTargetEntity t = ReleaseRuleTargetEntity.builder().tenantId(tenant)
                            .releaseRuleId(stored.getId()).contentItemId(target.getContentItemId())
                            .chapterId(target.getChapterId()).build();
                    t.onCreatingData(Actor.current(), clock);
                    releaseRuleTargetRepository.insert(t);
                }
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(),
                        LmsEvents.payload(stored.getId()));
                copies.add(mapper.toDto(stored));
            }
            return new ReleaseRuleList(copies);
        });
    }

    /**
     * lms.ReorderReleaseRules: the rules whose source is the item, in the order given (each named exactly once — 422
     * otherwise); they are evaluated in this order.
     */
    public ReleaseRuleList reorderReleaseRules(UUID itemId, ReorderReleaseRulesRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_REORDER);
        return doInWriteTransaction(() -> {
            ContentItemEntity item = writableItem(scope, itemId);
            List<UUID> current = repository.ofSource(item.getId()).stream().map(ReleaseRuleEntity::getId).toList();
            List<UUID> wanted = request.getReleaseRuleIds();
            if (wanted.size() != current.size() || !new HashSet<>(wanted).equals(new HashSet<>(current))) {
                throw new ValidationFailedException(List.of(new FieldError("releaseRuleIds",
                        "must list each rule of the item exactly once", "Invalid")));
            }
            repository.place(wanted, now(), Actor.current().orElse(null));
            audit.recordTransition("lms.content_item", itemId, "reorder_release_rules", List.of("releaseRules"), null);
            return new ReleaseRuleList(repository.ofSource(itemId).stream().map(mapper::toDto).toList());
        });
    }

    /** lms.SetReleaseRulesEnabled: enables or disables every rule whose source is the item, at once. */
    public ReleaseRuleList setReleaseRulesEnabled(UUID itemId, SetReleaseRulesEnabledRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_SET_ENABLED);
        return doInWriteTransaction(() -> {
            ContentItemEntity item = writableItem(scope, itemId);
            repository.setEnabledForSource(item.getId(), request.getEnabled(), now(), Actor.current().orElse(null));
            audit.recordTransition("lms.content_item", itemId, request.getEnabled() ? "enable_release_rules"
                    : "disable_release_rules", List.of("releaseRules"), null);
            return new ReleaseRuleList(repository.ofSource(itemId).stream().map(mapper::toDto).toList());
        });
    }

    /** The item, visible (404) and writable (403) for the caller: its course's institution decides. */
    private ContentItemEntity writableItem(DataScope scope, UUID itemId) {
        ContentItemEntity item = items.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("lms.content_item", itemId));
        Optional<UUID> institution = items.institutionOf(item.getCourseId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException("lms.content_item", itemId);
        }
        scope.requireWritable(institution);
        return item;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private ReleaseRuleEntity loadVisible(DataScope scope, UUID id) {
        ReleaseRuleEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(ReleaseRuleEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected ReleaseRuleUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ReleaseRuleEntity entity, ReleaseRuleUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "sourceContentItemId" -> entity.setSourceContentItemId(changes.getSourceContentItemId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "stopIfTrue" -> entity.setStopIfTrue(changes.getStopIfTrue());
                case "isDisabled" -> entity.setIsDisabled(changes.getIsDisabled());
                case "reassignSource" -> entity.setReassignSource(changes.getReassignSource());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "copiedFromId" -> entity.setCopiedFromId(changes.getCopiedFromId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ReleaseRuleEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getSourceContentItemId() == null) {
            errors.add(new FieldError("sourceContentItemId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStopIfTrue() == null) {
            errors.add(new FieldError("stopIfTrue", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsDisabled() == null) {
            errors.add(new FieldError("isDisabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getReassignSource() == null) {
            errors.add(new FieldError("reassignSource", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCopiedFromId() != null && !repository.existsCopiedFromId(entity.getCopiedFromId())) {
            errors.add(new FieldError("copiedFromId", "release rule does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getSourceContentItemId() != null
                && !repository.existsSourceContentItemId(entity.getSourceContentItemId())) {
            errors.add(new FieldError("sourceContentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ReleaseRuleEntity entity) {

    }

    // ---- ReleaseRuleCondition: a record of the ReleaseRule aggregate ----

    public ReleaseRuleCondition getReleaseRuleCondition(UUID releaseRuleId, UUID id) {
        return doInReadTransaction(() -> {
            return releaseRuleConditionMapper.toDto(loadVisibleReleaseRuleCondition(DataScope.current(
                    RELEASE_RULE_CONDITION_PERMISSION_GET), releaseRuleId, id));
        });
    }

    public Page<ReleaseRuleCondition> listReleaseRuleConditions(UUID releaseRuleId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ReleaseRuleConditionRepository.SORT_FIELDS,
                        ReleaseRuleConditionRepository.DEFAULT_ORDER,
                ReleaseRuleConditionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReleaseRuleConditionRepository.FILTER_FIELDS)
                .and(ReleaseRuleConditionRepository.ofParent(releaseRuleId))
                        .and(ReleaseRuleConditionRepository.scope(DataScope.current(RELEASE_RULE_CONDITION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + releaseRuleId, clock);
        return doInReadTransaction(() -> {
            Page<ReleaseRuleConditionEntity> rows = releaseRuleConditionRepository.list(page, where);
            return rows.map(releaseRuleConditionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ReleaseRuleCondition createReleaseRuleCondition(UUID releaseRuleId, ReleaseRuleCondition request,
            boolean validateOnly) {
        validator.validate(request);
        ReleaseRuleConditionEntity entity = releaseRuleConditionMapper.toEntity(request);
        entity.setReleaseRuleId(releaseRuleId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RELEASE_RULE_CONDITION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!releaseRuleConditionRepository.existsReleaseRuleId(entity.getReleaseRuleId())) {
                throw new ResourceNotFoundException("lms.release_rule", entity.getReleaseRuleId());
            }
            if (!scope.canRead(institutionOfReleaseRuleCondition(entity))) {
                throw new ResourceNotFoundException("lms.release_rule", entity.getReleaseRuleId());
            }
            assertValidReleaseRuleCondition(entity, true);
            scope.requireWritable(institutionOfReleaseRuleCondition(entity));
            assertUniqueReleaseRuleCondition(entity);
            if (validateOnly) {
                return releaseRuleConditionMapper.toDto(entity);
            }
            ReleaseRuleConditionEntity stored = releaseRuleConditionRepository.insert(entity);
            audit.recordCreate(RELEASE_RULE_CONDITION_AGGREGATE, stored.getId());
            outbox.publish(RELEASE_RULE_CONDITION_AGGREGATE + LmsEvents.CREATED, RELEASE_RULE_CONDITION_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return releaseRuleConditionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ReleaseRuleCondition updateReleaseRuleCondition(UUID releaseRuleId, UUID id, String ifMatch,
            ReleaseRuleConditionUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, RELEASE_RULE_CONDITION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELEASE_RULE_CONDITION_PERMISSION_UPDATE);
            ReleaseRuleConditionEntity entity = loadVisibleReleaseRuleCondition(scope, releaseRuleId, id);
            scope.requireWritable(institutionOfReleaseRuleCondition(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyReleaseRuleCondition(entity, changes, fields);
            validator.validate(releaseRuleConditionMapper.toDto(entity));
            assertValidReleaseRuleCondition(entity, false);
            scope.requireWritable(institutionOfReleaseRuleCondition(entity));
            assertUniqueReleaseRuleCondition(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return releaseRuleConditionMapper.toDto(entity);
            }
            if (releaseRuleConditionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RELEASE_RULE_CONDITION_AGGREGATE, id, fields);
            outbox.publish(RELEASE_RULE_CONDITION_AGGREGATE + LmsEvents.UPDATED, RELEASE_RULE_CONDITION_AGGREGATE, id,
                    LmsEvents.payload(id));
            return releaseRuleConditionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteReleaseRuleCondition(UUID releaseRuleId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELEASE_RULE_CONDITION_PERMISSION_DELETE);
            ReleaseRuleConditionEntity entity = loadVisibleReleaseRuleCondition(scope, releaseRuleId, id);
            scope.requireWritable(institutionOfReleaseRuleCondition(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (releaseRuleConditionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RELEASE_RULE_CONDITION_AGGREGATE, id);
            outbox.publish(RELEASE_RULE_CONDITION_AGGREGATE + LmsEvents.DELETED, RELEASE_RULE_CONDITION_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ReleaseRuleConditionEntity loadVisibleReleaseRuleCondition(DataScope scope, UUID releaseRuleId, UUID id) {
        ReleaseRuleConditionEntity entity = releaseRuleConditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RELEASE_RULE_CONDITION_AGGREGATE, id));
        if (!scope.canRead(institutionOfReleaseRuleCondition(entity)) || !releaseRuleId.equals(entity.getReleaseRuleId())) {
            throw new ResourceNotFoundException(RELEASE_RULE_CONDITION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.release_rule → lms.course it belongs to). */
    private Optional<UUID> institutionOfReleaseRuleCondition(ReleaseRuleConditionEntity entity) {
        return releaseRuleConditionRepository.institutionOf(entity.getReleaseRuleId());
    }

    /** Applies the selected ReleaseRuleConditionUpdate fields; explicit per field so a mask can clear values. */
    private static void applyReleaseRuleCondition(ReleaseRuleConditionEntity entity, ReleaseRuleConditionUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "conditionType" -> entity.setConditionType(Optional.ofNullable(changes.getConditionType())
                        .map(v -> v.getValue()).orElse(null));
                case "operator" -> entity.setOperator(changes.getOperator());
                case "value" -> entity.setValue(changes.getValue());
                case "value2" -> entity.setValue2(changes.getValue2());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidReleaseRuleCondition(ReleaseRuleConditionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getConditionType() == null) {
            errors.add(new FieldError("conditionType", "must not be null", "NotNull"));
        }
        if (entity.getReleaseRuleId() == null) {
            errors.add(new FieldError("releaseRuleId", "must not be null", "NotNull"));
        }
        if (entity.getOperator() == null) {
            errors.add(new FieldError("operator", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getReleaseRuleId() != null && !releaseRuleConditionRepository.existsReleaseRuleId(entity.getReleaseRuleId())) {
            errors.add(new FieldError("releaseRuleId", "release rule does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueReleaseRuleCondition(ReleaseRuleConditionEntity entity) {

    }

    // ---- ReleaseRuleTarget: a record of the ReleaseRule aggregate ----

    public ReleaseRuleTarget getReleaseRuleTarget(UUID releaseRuleId, UUID id) {
        return doInReadTransaction(() -> {
            return releaseRuleTargetMapper.toDto(loadVisibleReleaseRuleTarget(DataScope.current(
                    RELEASE_RULE_TARGET_PERMISSION_GET), releaseRuleId, id));
        });
    }

    public Page<ReleaseRuleTarget> listReleaseRuleTargets(UUID releaseRuleId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ReleaseRuleTargetRepository.SORT_FIELDS,
                        ReleaseRuleTargetRepository.DEFAULT_ORDER,
                ReleaseRuleTargetRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReleaseRuleTargetRepository.FILTER_FIELDS)
                .and(ReleaseRuleTargetRepository.ofParent(releaseRuleId))
                        .and(ReleaseRuleTargetRepository.scope(DataScope.current(RELEASE_RULE_TARGET_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + releaseRuleId, clock);
        return doInReadTransaction(() -> {
            Page<ReleaseRuleTargetEntity> rows = releaseRuleTargetRepository.list(page, where);
            return rows.map(releaseRuleTargetMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ReleaseRuleTarget createReleaseRuleTarget(UUID releaseRuleId, ReleaseRuleTarget request,
            boolean validateOnly) {
        validator.validate(request);
        ReleaseRuleTargetEntity entity = releaseRuleTargetMapper.toEntity(request);
        entity.setReleaseRuleId(releaseRuleId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RELEASE_RULE_TARGET_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!releaseRuleTargetRepository.existsReleaseRuleId(entity.getReleaseRuleId())) {
                throw new ResourceNotFoundException("lms.release_rule", entity.getReleaseRuleId());
            }
            if (!scope.canRead(institutionOfReleaseRuleTarget(entity))) {
                throw new ResourceNotFoundException("lms.release_rule", entity.getReleaseRuleId());
            }
            assertValidReleaseRuleTarget(entity, true);
            scope.requireWritable(institutionOfReleaseRuleTarget(entity));
            assertUniqueReleaseRuleTarget(entity);
            if (validateOnly) {
                return releaseRuleTargetMapper.toDto(entity);
            }
            ReleaseRuleTargetEntity stored = releaseRuleTargetRepository.insert(entity);
            audit.recordCreate(RELEASE_RULE_TARGET_AGGREGATE, stored.getId());
            outbox.publish(RELEASE_RULE_TARGET_AGGREGATE + LmsEvents.CREATED, RELEASE_RULE_TARGET_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return releaseRuleTargetMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ReleaseRuleTarget updateReleaseRuleTarget(UUID releaseRuleId, UUID id, String ifMatch,
            ReleaseRuleTargetUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, RELEASE_RULE_TARGET_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELEASE_RULE_TARGET_PERMISSION_UPDATE);
            ReleaseRuleTargetEntity entity = loadVisibleReleaseRuleTarget(scope, releaseRuleId, id);
            scope.requireWritable(institutionOfReleaseRuleTarget(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyReleaseRuleTarget(entity, changes, fields);
            validator.validate(releaseRuleTargetMapper.toDto(entity));
            assertValidReleaseRuleTarget(entity, false);
            scope.requireWritable(institutionOfReleaseRuleTarget(entity));
            assertUniqueReleaseRuleTarget(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return releaseRuleTargetMapper.toDto(entity);
            }
            if (releaseRuleTargetRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RELEASE_RULE_TARGET_AGGREGATE, id, fields);
            outbox.publish(RELEASE_RULE_TARGET_AGGREGATE + LmsEvents.UPDATED, RELEASE_RULE_TARGET_AGGREGATE, id,
                    LmsEvents.payload(id));
            return releaseRuleTargetMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteReleaseRuleTarget(UUID releaseRuleId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELEASE_RULE_TARGET_PERMISSION_DELETE);
            ReleaseRuleTargetEntity entity = loadVisibleReleaseRuleTarget(scope, releaseRuleId, id);
            scope.requireWritable(institutionOfReleaseRuleTarget(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (releaseRuleTargetRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RELEASE_RULE_TARGET_AGGREGATE, id);
            outbox.publish(RELEASE_RULE_TARGET_AGGREGATE + LmsEvents.DELETED, RELEASE_RULE_TARGET_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ReleaseRuleTargetEntity loadVisibleReleaseRuleTarget(DataScope scope, UUID releaseRuleId, UUID id) {
        ReleaseRuleTargetEntity entity = releaseRuleTargetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RELEASE_RULE_TARGET_AGGREGATE, id));
        if (!scope.canRead(institutionOfReleaseRuleTarget(entity)) || !releaseRuleId.equals(entity.getReleaseRuleId())) {
            throw new ResourceNotFoundException(RELEASE_RULE_TARGET_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.release_rule → lms.course it belongs to). */
    private Optional<UUID> institutionOfReleaseRuleTarget(ReleaseRuleTargetEntity entity) {
        return releaseRuleTargetRepository.institutionOf(entity.getReleaseRuleId());
    }

    /** Applies the selected ReleaseRuleTargetUpdate fields; explicit per field so a mask can clear values. */
    private static void applyReleaseRuleTarget(ReleaseRuleTargetEntity entity, ReleaseRuleTargetUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "contentItemId" -> entity.setContentItemId(changes.getContentItemId());
                case "chapterId" -> entity.setChapterId(changes.getChapterId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidReleaseRuleTarget(ReleaseRuleTargetEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getReleaseRuleId() == null) {
            errors.add(new FieldError("releaseRuleId", "must not be null", "NotNull"));
        }
        if (entity.getChapterId() != null && !releaseRuleTargetRepository.existsChapterId(entity.getChapterId())) {
            errors.add(new FieldError("chapterId", "chapter does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !releaseRuleTargetRepository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getReleaseRuleId() != null && !releaseRuleTargetRepository.existsReleaseRuleId(entity.getReleaseRuleId())) {
            errors.add(new FieldError("releaseRuleId", "release rule does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueReleaseRuleTarget(ReleaseRuleTargetEntity entity) {

    }
}
