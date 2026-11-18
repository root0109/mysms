package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.Badge;
import com.mysms.lms.api.model.BadgeUpdate;
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
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.BadgeEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.BadgeMapper;
import com.mysms.lms.repository.BadgeRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.core.error.ConflictException;
import com.mysms.lms.api.model.BadgeAward;
import com.mysms.lms.domain.BadgeAwardEntity;
import com.mysms.lms.mapper.BadgeAwardMapper;
import com.mysms.lms.repository.BadgeAwardRepository;
import com.mysms.lms.repository.CertificationRepository;
import java.time.LocalDate;

/**
 * Business service of lms.badge: audit and domain event commit in the same transaction as every change. Rows outside
 * the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class BadgeService extends AppTransactionManager {

    public static final String AGGREGATE = BadgeEntity.TABLE;
    static final String PERMISSION_LIST = "lms.badge.read";
    static final String PERMISSION_GET = "lms.badge.read";
    static final String PERMISSION_CREATE = "lms.badge.create";
    static final String PERMISSION_UPDATE = "lms.badge.update";
    static final String PERMISSION_DELETE = "lms.badge.delete";
    /** The contract's BadgeUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("title", "description", "imageFileId", "targetEntity", "triggerEvent", "conditionExpr",
                    "fieldToCheck",
            "userField", "grantOnlyOnce", "isEnabled");


    private final BadgeRepository repository;
    private final BadgeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final BadgeAwardRepository awards;
    private final BadgeAwardMapper awardMapper;
    private final CertificationRepository certification;

    public BadgeService(BadgeRepository repository, BadgeMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, BadgeAwardRepository awards, BadgeAwardMapper awardMapper,
            CertificationRepository certification) {

        super(database, transactions);
        this.awards = awards;
        this.awardMapper = awardMapper;
        this.certification = certification;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Badge getBadge(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Badge> listBadges(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, BadgeRepository.SORT_FIELDS, BadgeRepository.DEFAULT_ORDER,
                BadgeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, BadgeRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<BadgeEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Badge createBadge(Badge request, boolean validateOnly) {
        validator.validate(request);
        BadgeEntity entity = mapper.toEntity(request);
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
            BadgeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Badge updateBadge(UUID id, String ifMatch, BadgeUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            BadgeEntity entity = loadVisible(scope, id);
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
    public void deleteBadge(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            BadgeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByBadgeAwardBadgeId(id)) {
                throw new InvalidStateException(
                        "The badge is still referenced by lms.badge_award rows; remove those first.");
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
    private BadgeEntity loadVisible(DataScope scope, UUID id) {
        BadgeEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(BadgeEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected BadgeUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(BadgeEntity entity, BadgeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "imageFileId" -> entity.setImageFileId(changes.getImageFileId());
                case "targetEntity" -> entity.setTargetEntity(changes.getTargetEntity());
                case "triggerEvent" -> entity.setTriggerEvent(Optional.ofNullable(changes.getTriggerEvent())
                        .map(v -> v.getValue()).orElse(null));
                case "conditionExpr" -> entity.setConditionExpr(changes.getConditionExpr());
                case "fieldToCheck" -> entity.setFieldToCheck(changes.getFieldToCheck());
                case "userField" -> entity.setUserField(changes.getUserField());
                case "grantOnlyOnce" -> entity.setGrantOnlyOnce(changes.getGrantOnlyOnce());
                case "isEnabled" -> entity.setIsEnabled(changes.getIsEnabled());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(BadgeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getDescription() == null) {
            errors.add(new FieldError("description", "must not be null", "NotNull"));
        }
        if (entity.getImageFileId() == null) {
            errors.add(new FieldError("imageFileId", "must not be null", "NotNull"));
        }
        if (entity.getTargetEntity() == null) {
            errors.add(new FieldError("targetEntity", "must not be null", "NotNull"));
        }
        if (entity.getTriggerEvent() == null) {
            errors.add(new FieldError("triggerEvent", "must not be null", "NotNull"));
        }
        if (entity.getConditionExpr() == null) {
            errors.add(new FieldError("conditionExpr", "must not be null", "NotNull"));
        }
        if (entity.getUserField() == null) {
            errors.add(new FieldError("userField", "must not be null", "NotNull"));
        }
        if (!creating && entity.getGrantOnlyOnce() == null) {
            errors.add(new FieldError("grantOnlyOnce", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsEnabled() == null) {
            errors.add(new FieldError("isEnabled", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(BadgeEntity entity) {
        if (repository.takenTitle1(entity)) {
            throw new DuplicateResourceException("A badge with the same title already exists.");
        }
    }

    // ---- awarding a badge by hand ----

    static final String PERMISSION_AWARD = "lms.badge.award_badge";
    private static final String AWARD = "lms.badge_award";

    /**
     * lms.AwardBadge: awards the badge to a user today. The badge must be enabled (409 BADGE_DISABLED); a badge
     * granted only once is not awarded to the same user twice (409 ALREADY_AWARDED). Badges are tenant-level: awarding
     * one does not change it, so the operation's permission is enough.
     */
    public BadgeAward awardBadge(UUID id, UUID userId) {
        DataScope scope = DataScope.current(PERMISSION_AWARD);
        return doInWriteTransaction(() -> {
            BadgeEntity badge = loadVisible(scope, id);
            if (!Boolean.TRUE.equals(badge.getIsEnabled())) {
                throw new ConflictException("BADGE_DISABLED", "The badge is disabled.");
            }
            if (Boolean.TRUE.equals(badge.getGrantOnlyOnce()) && certification.awarded(id, userId)) {
                throw new ConflictException("ALREADY_AWARDED", "The user already holds the badge.");
            }
            BadgeAwardEntity award = BadgeAwardEntity.builder().badgeId(id).userId(userId)
                    .issuedOn(LocalDate.now(clock)).build();
            award.onCreatingData(Actor.current(), clock);
            award.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
            BadgeAwardEntity stored = awards.insert(award);
            audit.recordCreate(AWARD, stored.getId());
            outbox.publish(AWARD + LmsEvents.CREATED, AWARD, stored.getId(), LmsEvents.payload(stored.getId()));
            return awardMapper.toDto(stored);
        });
    }
}
