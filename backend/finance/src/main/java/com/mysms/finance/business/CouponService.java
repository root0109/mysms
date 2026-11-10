package com.mysms.finance.business;

import com.mysms.authclient.DataScope;
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
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.finance.api.model.Coupon;
import com.mysms.finance.api.model.CouponScope;
import com.mysms.finance.api.model.CouponScopeUpdate;
import com.mysms.finance.api.model.CouponUpdate;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.CouponEntity;
import com.mysms.finance.domain.CouponScopeEntity;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.CouponMapper;
import com.mysms.finance.mapper.CouponScopeMapper;
import com.mysms.finance.repository.CouponRepository;
import com.mysms.finance.repository.CouponScopeRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of finance.coupon (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Coupon
 * are served here too — CouponScope.
 */
@Service
public class CouponService extends AppTransactionManager {

    public static final String AGGREGATE = CouponEntity.TABLE;
    static final String PERMISSION_LIST = "finance.coupon.read";
    static final String PERMISSION_GET = "finance.coupon.read";
    static final String PERMISSION_CREATE = "finance.coupon.create";
    static final String PERMISSION_UPDATE = "finance.coupon.update";
    static final String PERMISSION_DELETE = "finance.coupon.delete";
    /** The contract's CouponUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("code", "discountType", "percentageDiscount", "fixedDiscount", "expiresOn", "usageLimit",
                    "redemptionCount",
            "isEnabled");

    public static final String COUPON_SCOPE_AGGREGATE = CouponScopeEntity.TABLE;
    static final String COUPON_SCOPE_PERMISSION_LIST = "finance.coupon_scope.read";
    static final String COUPON_SCOPE_PERMISSION_GET = "finance.coupon_scope.read";
    static final String COUPON_SCOPE_PERMISSION_CREATE = "finance.coupon_scope.create";
    static final String COUPON_SCOPE_PERMISSION_UPDATE = "finance.coupon_scope.update";
    static final String COUPON_SCOPE_PERMISSION_DELETE = "finance.coupon_scope.delete";
    /** The contract's CouponScopeUpdate fields, in apply order. */
    static final List<String> COUPON_SCOPE_WRITABLE = List.of("lmsCourseId", "lmsCohortId");

    private final CouponRepository repository;
    private final CouponMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CouponScopeRepository couponScopeRepository;
    private final CouponScopeMapper couponScopeMapper;

    public CouponService(CouponRepository repository,
            CouponMapper mapper,
            @Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            CouponScopeRepository couponScopeRepository,
            CouponScopeMapper couponScopeMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.couponScopeRepository = couponScopeRepository;
        this.couponScopeMapper = couponScopeMapper;
    }

    public Coupon getCoupon(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Coupon> listCoupons(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CouponRepository.SORT_FIELDS, CouponRepository.DEFAULT_ORDER,
                CouponRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CouponRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CouponEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Coupon createCoupon(Coupon request, boolean validateOnly) {
        validator.validate(request);
        CouponEntity entity = mapper.toEntity(request);
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
            CouponEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, stored.getId(),
                    FinanceEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Coupon updateCoupon(UUID id, String ifMatch, CouponUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CouponEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + FinanceEvents.UPDATED, AGGREGATE, id, FinanceEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCoupon(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CouponEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCouponScopeCouponId(id)) {
                throw new InvalidStateException(
                        "The coupon is still referenced by finance.coupon_scope rows; remove those first.");
            }
            if (repository.referencedByPaymentCouponId(id)) {
                throw new InvalidStateException(
                        "The coupon is still referenced by finance.payment rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + FinanceEvents.DELETED, AGGREGATE, id, FinanceEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CouponEntity loadVisible(DataScope scope, UUID id) {
        CouponEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(CouponEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected CouponUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(CouponEntity entity, CouponUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "discountType" -> entity.setDiscountType(Optional.ofNullable(changes.getDiscountType())
                        .map(v -> v.getValue()).orElse(null));
                case "percentageDiscount" -> entity.setPercentageDiscount(
                        WireValues.decimal(changes.getPercentageDiscount()));
                case "fixedDiscount" -> entity.setFixedDiscount(WireValues.decimal(changes.getFixedDiscount()));
                case "expiresOn" -> entity.setExpiresOn(changes.getExpiresOn());
                case "usageLimit" -> entity.setUsageLimit(changes.getUsageLimit());
                case "redemptionCount" -> entity.setRedemptionCount(changes.getRedemptionCount());
                case "isEnabled" -> entity.setIsEnabled(changes.getIsEnabled());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CouponEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getDiscountType() == null) {
            errors.add(new FieldError("discountType", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("percentageDiscount", WireValues.decimal(entity.getPercentageDiscount()), 5, 2, errors);
        WireValues.checkDecimal("fixedDiscount", WireValues.decimal(entity.getFixedDiscount()), 14, 2, errors);
        if (!creating && entity.getRedemptionCount() == null) {
            errors.add(new FieldError("redemptionCount", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsEnabled() == null) {
            errors.add(new FieldError("isEnabled", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CouponEntity entity) {
        if (repository.takenCode1(entity)) {
            throw new DuplicateResourceException("A coupon with the same code already exists.");
        }
    }

    // ---- CouponScope: a record of the Coupon aggregate ----

    public CouponScope getCouponScope(UUID couponId, UUID id) {
        return doInReadTransaction(() -> {
            return couponScopeMapper.toDto(loadVisibleCouponScope(DataScope.current(COUPON_SCOPE_PERMISSION_GET), couponId, id));
        });
    }

    public Page<CouponScope> listCouponScopes(UUID couponId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CouponScopeRepository.SORT_FIELDS, CouponScopeRepository.DEFAULT_ORDER,
                CouponScopeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CouponScopeRepository.FILTER_FIELDS)
                .and(CouponScopeRepository.ofParent(couponId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + couponId, clock);
        return doInReadTransaction(() -> {
            Page<CouponScopeEntity> rows = couponScopeRepository.list(page, where);
            return rows.map(couponScopeMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CouponScope createCouponScope(UUID couponId, CouponScope request, boolean validateOnly) {
        validator.validate(request);
        CouponScopeEntity entity = couponScopeMapper.toEntity(request);
        entity.setCouponId(couponId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COUPON_SCOPE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!couponScopeRepository.existsCouponId(entity.getCouponId())) {
                throw new ResourceNotFoundException("finance.coupon", entity.getCouponId());
            }
            assertValidCouponScope(entity, true);
            scope.requireWritable(institutionOfCouponScope(entity));
            assertUniqueCouponScope(entity);
            if (validateOnly) {
                return couponScopeMapper.toDto(entity);
            }
            CouponScopeEntity stored = couponScopeRepository.insert(entity);
            audit.recordCreate(COUPON_SCOPE_AGGREGATE, stored.getId());
            outbox.publish(COUPON_SCOPE_AGGREGATE + FinanceEvents.CREATED, COUPON_SCOPE_AGGREGATE, stored.getId(),
                    FinanceEvents.payload(stored.getId()));
            return couponScopeMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CouponScope updateCouponScope(UUID couponId, UUID id, String ifMatch, CouponScopeUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COUPON_SCOPE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COUPON_SCOPE_PERMISSION_UPDATE);
            CouponScopeEntity entity = loadVisibleCouponScope(scope, couponId, id);
            scope.requireWritable(institutionOfCouponScope(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCouponScope(entity, changes, fields);
            validator.validate(couponScopeMapper.toDto(entity));
            assertValidCouponScope(entity, false);
            scope.requireWritable(institutionOfCouponScope(entity));
            assertUniqueCouponScope(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return couponScopeMapper.toDto(entity);
            }
            if (couponScopeRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COUPON_SCOPE_AGGREGATE, id, fields);
            outbox.publish(COUPON_SCOPE_AGGREGATE + FinanceEvents.UPDATED, COUPON_SCOPE_AGGREGATE, id, FinanceEvents.payload(id));
            return couponScopeMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCouponScope(UUID couponId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COUPON_SCOPE_PERMISSION_DELETE);
            CouponScopeEntity entity = loadVisibleCouponScope(scope, couponId, id);
            scope.requireWritable(institutionOfCouponScope(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (couponScopeRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COUPON_SCOPE_AGGREGATE, id);
            outbox.publish(COUPON_SCOPE_AGGREGATE + FinanceEvents.DELETED, COUPON_SCOPE_AGGREGATE, id, FinanceEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CouponScopeEntity loadVisibleCouponScope(DataScope scope, UUID couponId, UUID id) {
        CouponScopeEntity entity = couponScopeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COUPON_SCOPE_AGGREGATE, id));
        if (!scope.canRead(institutionOfCouponScope(entity)) || !couponId.equals(entity.getCouponId())) {
            throw new ResourceNotFoundException(COUPON_SCOPE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfCouponScope(CouponScopeEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected CouponScopeUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyCouponScope(CouponScopeEntity entity, CouponScopeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "lmsCourseId" -> entity.setLmsCourseId(changes.getLmsCourseId());
                case "lmsCohortId" -> entity.setLmsCohortId(changes.getLmsCohortId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCouponScope(CouponScopeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCouponId() == null) {
            errors.add(new FieldError("couponId", "must not be null", "NotNull"));
        }
        if (entity.getCouponId() != null && !couponScopeRepository.existsCouponId(entity.getCouponId())) {
            errors.add(new FieldError("couponId", "coupon does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCouponScope(CouponScopeEntity entity) {

    }
}
