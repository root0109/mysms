package com.mysms.privacy.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
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
import com.mysms.privacy.api.model.LegalHold;
import com.mysms.privacy.api.model.LegalHoldUpdate;
import com.mysms.privacy.api.model.ListActiveHoldsResponse;
import com.mysms.privacy.api.model.ReleaseLegalHoldRequest;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.LegalHoldEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.LegalHoldMapper;
import com.mysms.privacy.repository.LegalHoldRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The legal holds of a tenant (privacy.legal_hold, design §5.5, §10): what no erasure task and no retention sweep may
 * remove while a hold is active — a subject's data, a data category (of a subject or of everyone) or named records of
 * one module. The data protection officer or the safeguarding lead places a hold with its reason and a review date,
 * moves the review date while it is active, and releases it; a hold is never deleted. Every change writes its audit
 * record and its event (ids and codes only) in the same transaction.
 *
 * <p>Holds are tenant-level data: reading needs the permission, every change a tenant-wide grant, and nothing is
 * placed or changed while impersonating (403). What a hold covers must fit its scope (422) and is fixed once placed;
 * its review date is today or later (UTC). An edit compares If-Match, a release its body etag (412); a released hold
 * changes no more (409). Whether a review is due is derived at read time.
 *
 * <p>Modules ask which holds apply before they erase or sweep a subject's data ({@link #listActiveHolds}, an
 * internal operation for service callers): the task they are dispatched carries no hold, so a hold placed after the
 * dispatch still counts.
 */
@Service
public class LegalHoldService extends AppTransactionManager {

    public static final String AGGREGATE = LegalHoldEntity.TABLE;
    static final String PERMISSION_PLACE = "privacy.legal_hold.place";
    static final String PERMISSION_UPDATE = "privacy.legal_hold.update";
    static final String PERMISSION_RELEASE = "privacy.legal_hold.release";
    /** The contract's LegalHoldUpdate fields. */
    static final List<String> WRITABLE = List.of("reviewBy");

    /** No institution owns a row of this aggregate. */
    private static final Optional<UUID> TENANT_LEVEL = Optional.empty();
    private static final Pattern MODULE = Pattern.compile("^[a-z]{2,30}$");
    private static final String SUBJECT = "subject";
    private static final String CATEGORY = "category";
    private static final String RECORDS = "records";
    private static final String REQUIRED = "LEGAL_HOLD_TARGET_REQUIRED";
    private static final String NOT_ALLOWED = "LEGAL_HOLD_TARGET_NOT_ALLOWED";
    private static final String REQUIRED_SENTENCE = "is required for this scope of hold";
    private static final String NOT_ALLOWED_SENTENCE = "does not apply to this scope of hold";

    private final LegalHoldRepository repository;
    private final LegalHoldMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LegalHoldService(LegalHoldRepository repository,
            LegalHoldMapper mapper,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public LegalHold getLegalHold(UUID id) {
        return doInReadTransaction(() -> dto(load(id), today()));
    }

    /** The holds page by page, the nearest review date first. */
    public Page<LegalHold> listLegalHolds(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LegalHoldRepository.SORT_FIELDS, LegalHoldRepository.DEFAULT_ORDER,
                LegalHoldRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LegalHoldRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            LocalDate today = today();
            return repository.list(page, where).map(h -> dto(h, today));
        });
    }

    /**
     * privacy.PlaceLegalHold: an active hold, placed by the caller now, on what its scope names (422 when the targets
     * do not fit the scope, the category is not in the catalogue, or the review date has passed).
     */
    public LegalHold placeLegalHold(LegalHold request) {
        requireTenantWide(PERMISSION_PLACE);
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        String scope = request.getScope().getValue();
        List<UUID> entityIds = request.getEntityIds();
        LegalHoldEntity entity = LegalHoldEntity.builder()
                .reasonCode(request.getReasonCode().getValue())
                .scope(scope)
                .subjectPersonId(request.getSubjectPersonId())
                .categoryCode(request.getCategoryCode())
                .module(request.getModule())
                .entityType(request.getEntityType())
                .entityIds(entityIds == null || entityIds.isEmpty() ? null : List.copyOf(entityIds))
                .placedByUserId(caller.userId())
                .reviewBy(request.getReviewBy())
                .build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant());
        entity.setPlacedAt(entity.getCreatedAt());
        return doInWriteTransaction(() -> {
            List<FieldError> errors = targetErrors(entity);
            reviewByError(entity.getReviewBy()).ifPresent(errors::add);
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            repository.insert(entity);
            audit.recordCreate(AGGREGATE, entity.getId());
            outbox.publish(AGGREGATE + PrivacyEvents.PLACED, AGGREGATE, entity.getId(),
                    PrivacyEvents.legalHold(entity));
            return dto(entity, today());
        });
    }

    /** privacy.UpdateLegalHold: an active hold's review date (409 once released), under If-Match. */
    public LegalHold updateLegalHold(UUID id, String ifMatch, LegalHoldUpdate changes, String updateMask) {
        requireTenantWide(PERMISSION_UPDATE);
        refuseImpersonation(AuthContext.require());
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            LegalHoldEntity entity = load(id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            requireActive(entity);
            if (fields.contains("reviewBy")) {
                entity.setReviewBy(changes.getReviewBy());
            }
            validator.validate(mapper.toDto(entity));
            reviewByError(entity.getReviewBy()).ifPresent(error -> {
                throw new ValidationFailedException(List.of(error));
            });
            entity.onUpdatingData(Actor.current(), clock);
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("The legal hold changed since it was read.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + PrivacyEvents.UPDATED, AGGREGATE, id, PrivacyEvents.legalHold(entity));
            return dto(entity, today());
        });
    }

    /** privacy.ReleaseLegalHold: active → released by the caller now (409 once released). */
    public LegalHold releaseLegalHold(UUID id, ReleaseLegalHoldRequest request) {
        requireTenantWide(PERMISSION_RELEASE);
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        return doInWriteTransaction(() -> {
            LegalHoldEntity entity = load(id);
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            requireActive(entity);
            OffsetDateTime now = now();
            if (repository.release(id, entity.getRowVersion(), now, caller.userId()) == 0) {
                throw new PreconditionFailedException("The legal hold changed since it was read.");
            }
            entity.setReleasedAt(now);
            entity.setReleasedByUserId(caller.userId());
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(caller.userId());
            entity.setRowVersion(entity.getRowVersion() + 1);
            audit.recordTransition(AGGREGATE, id, "release", List.of("releasedAt", "releasedByUserId"), null);
            outbox.publish(AGGREGATE + PrivacyEvents.RELEASED, AGGREGATE, id, PrivacyEvents.legalHold(entity));
            return dto(entity, now.toLocalDate());
        });
    }

    /**
     * privacy.ListActiveHolds (internal): the active holds that may cover the subject's data — the subject's own and
     * every category or record-set hold of the subject or of everyone, record sets of other modules left out when a
     * module is named. Read on the primary: a hold placed a moment ago must count.
     */
    public ListActiveHoldsResponse listActiveHolds(UUID subjectPersonId, String module) {
        if (module != null && !MODULE.matcher(module).matches()) {
            throw new ValidationFailedException(List.of(new FieldError("module",
                    "must name a MySMS module by its code", "LEGAL_HOLD_MODULE_INVALID")));
        }
        return doInPrimaryReadTransaction(() -> new ListActiveHoldsResponse().holds(
                repository.activeFor(subjectPersonId, module).stream().map(mapper::toActive).toList()));
    }

    /** The errors of what the hold names against its scope (each target required or not allowed), all at once. */
    private List<FieldError> targetErrors(LegalHoldEntity hold) {
        String scope = hold.getScope();
        List<FieldError> errors = new ArrayList<>();
        fit(errors, "subjectPersonId", hold.getSubjectPersonId() != null, SUBJECT.equals(scope), true);
        fit(errors, "categoryCode", hold.getCategoryCode() != null, CATEGORY.equals(scope), CATEGORY.equals(scope));
        fit(errors, "module", hold.getModule() != null, RECORDS.equals(scope), RECORDS.equals(scope));
        fit(errors, "entityType", hold.getEntityType() != null, RECORDS.equals(scope), RECORDS.equals(scope));
        List<UUID> entityIds = hold.getEntityIds();
        fit(errors, "entityIds", entityIds != null && !entityIds.isEmpty(), RECORDS.equals(scope), RECORDS.equals(scope));
        if (hold.getCategoryCode() != null && CATEGORY.equals(scope)
                && !repository.categoryExists(hold.getCategoryCode())) {
            errors.add(new FieldError("categoryCode", "must name a data category of the catalogue",
                    "LEGAL_HOLD_CATEGORY_UNKNOWN"));
        }
        if (hold.getModule() != null && !MODULE.matcher(hold.getModule()).matches()) {
            errors.add(new FieldError("module", "must name a MySMS module by its code", "LEGAL_HOLD_MODULE_INVALID"));
        }
        return errors;
    }

    /** One target against the scope: missing while required, or present while not allowed. */
    private static void fit(List<FieldError> errors, String field, boolean present, boolean required,
            boolean allowed) {
        if (required && !present) {
            errors.add(new FieldError(field, REQUIRED_SENTENCE, REQUIRED));
        } else if (present && !allowed) {
            errors.add(new FieldError(field, NOT_ALLOWED_SENTENCE, NOT_ALLOWED));
        }
    }

    /** A review date before today (UTC) is refused: a hold is placed or moved to be reviewed in the future. */
    private Optional<FieldError> reviewByError(LocalDate reviewBy) {
        return reviewBy != null && reviewBy.isBefore(today())
                ? Optional.of(new FieldError("reviewBy", "must be today or later", "LEGAL_HOLD_REVIEW_BY_PAST"))
                : Optional.empty();
    }

    private static void requireActive(LegalHoldEntity hold) {
        if (hold.getReleasedAt() != null) {
            throw new InvalidStateException("A released legal hold cannot be changed.");
        }
    }

    private LegalHoldEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** The hold as the contract shows it on {@code today}: due for review once active on or past its review date. */
    private LegalHold dto(LegalHoldEntity hold, LocalDate today) {
        return mapper.toDto(hold).reviewDue(hold.getReleasedAt() == null && !hold.getReviewBy().isAfter(today));
    }

    /** Support staff acting as a user may read the holds, never place, move or release one. */
    private static void refuseImpersonation(AuthContext caller) {
        if (caller.impersonatorId().isPresent()) {
            throw new PermissionDeniedException("Legal holds cannot be placed or changed while acting as another user.");
        }
    }

    /** Refuses a change without a tenant-wide grant of the permission (403): holds are tenant-level. */
    private static void requireTenantWide(String permission) {
        DataScope.current(permission).requireWritable(TENANT_LEVEL);
    }

    private LocalDate today() {
        return now().toLocalDate();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static UUID tenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }
}
