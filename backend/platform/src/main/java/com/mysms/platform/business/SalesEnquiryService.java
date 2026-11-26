package com.mysms.platform.business;

import com.mysms.authclient.DataScope;
import com.mysms.platform.api.model.SalesEnquiry;
import com.mysms.platform.api.model.SalesEnquiryUpdate;
import com.mysms.platform.api.model.MarkEnquiryContactedRequest;
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
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.SalesEnquiryEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.SalesEnquiryMapper;
import com.mysms.platform.repository.SalesEnquiryRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.sales_enquiry: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class SalesEnquiryService extends AppTransactionManager {

    public static final String AGGREGATE = SalesEnquiryEntity.TABLE;


    private final SalesEnquiryRepository repository;
    private final SalesEnquiryMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public SalesEnquiryService(SalesEnquiryRepository repository, SalesEnquiryMapper mapper,
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

    public SalesEnquiry getSalesEnquiry(UUID id) {
        return doInGlobalReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.global(), id));
        });
    }

    public Page<SalesEnquiry> listSalesEnquirys(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SalesEnquiryRepository.SORT_FIELDS, SalesEnquiryRepository.DEFAULT_ORDER,
                SalesEnquiryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SalesEnquiryRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInGlobalReadTransaction(() -> {
            Page<SalesEnquiryEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SalesEnquiry createSalesEnquiry(SalesEnquiry request, boolean validateOnly) {
        validator.validate(request);
        SalesEnquiryEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        DataScope scope = DataScope.global();
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                assertValid(entity, true);
                scope.requireWritable(institutionOf(entity));
                assertUnique(entity);
                if (validateOnly) {
                    return mapper.toDto(entity);
                }
                SalesEnquiryEntity stored = repository.insert(entity);
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                        eventPayload(stored, stored.getRowVersion()));
                return mapper.toDto(stored);

            }
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public SalesEnquiry updateSalesEnquiry(UUID id, String ifMatch, SalesEnquiryUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                SalesEnquiryEntity entity = loadVisible(scope, id);
                scope.requireWritable(institutionOf(entity));
                if (entity.getRowVersion() != expected) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                List<String> fields = apply(entity, changes);
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
                outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, id, eventPayload(entity, expected + 1));
                return mapper.toDto(entity);

            }
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSalesEnquiry(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                DataScope scope = DataScope.global();
                SalesEnquiryEntity entity = loadVisible(scope, id);
                scope.requireWritable(institutionOf(entity));
                if (entity.getRowVersion() != expected) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                entity.onDeletingData(Actor.current(), clock);
                if (repository.softDelete(entity, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                audit.recordDelete(AGGREGATE, id);
                outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, eventPayload(entity, expected + 1));
            }
        });
    }

    /**
     * platform.MarkEnquiryContacted (platform staff): a new enquiry is marked contacted (409 from any other status);
     * the reason, when given, is kept in the audit record.
     */
    public SalesEnquiry markEnquiryContacted(UUID id, MarkEnquiryContactedRequest request) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        return doInGlobalWriteTransaction(() -> {
            try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
                SalesEnquiryEntity entity = loadVisible(DataScope.global(), id);
                if (!"new".equals(entity.getStatus())) {
                    throw new InvalidStateException("Only a new enquiry can be marked contacted.");
                }
                if (repository.markContacted(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                    throw new PreconditionFailedException("The row changed since it was read.");
                }
                String reason = request.getReason();
                audit.recordTransition(AGGREGATE, id, "markContacted", List.of("status"),
                        reason == null || reason.isBlank() ? null : reason.trim());
                outbox.publish(AGGREGATE + ".markContacted", AGGREGATE, id, eventPayload(entity,
                        entity.getRowVersion() + 1));
                return mapper.toDto(repository.findById(id).orElseThrow());
            }
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SalesEnquiryEntity loadVisible(DataScope scope, UUID id) {
        SalesEnquiryEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(SalesEnquiryEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(SalesEnquiryEntity entity, SalesEnquiryUpdate changes) {
        Patches patch = new Patches();
        patch.apply("contactName", changes.getContactName(), entity::setContactName);
        patch.apply("email", changes.getEmail(), entity::setEmail);
        patch.apply("phone", changes.getPhone(), entity::setPhone);
        patch.apply("organisationName", changes.getOrganisationName(), entity::setOrganisationName);
        patch.apply("designation", changes.getDesignation(), entity::setDesignation);
        patch.apply("channel", changes.getChannel(), entity::setChannel);
        patch.apply("message", changes.getMessage(), entity::setMessage);
        patch.apply("status", changes.getStatus(), v -> entity.setStatus(v == null ? null : v.getValue()));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(SalesEnquiryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getContactName() == null) {
            errors.add(new FieldError("contactName", "must not be null", "NotNull"));
        }
        if (entity.getEmail() == null) {
            errors.add(new FieldError("email", "must not be null", "NotNull"));
        }
        if (entity.getPhone() == null) {
            errors.add(new FieldError("phone", "must not be null", "NotNull"));
        }
        if (entity.getOrganisationName() == null) {
            errors.add(new FieldError("organisationName", "must not be null", "NotNull"));
        }
        if (entity.getDesignation() == null) {
            errors.add(new FieldError("designation", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(SalesEnquiryEntity entity) {

    }

    /** The event of a change to a global row: its id and the row version the change produced. */
    private static Map<String, Object> eventPayload(SalesEnquiryEntity e, int version) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", e.getId().toString());
        payload.put("rowVersion", version);
        return payload;
    }
}
