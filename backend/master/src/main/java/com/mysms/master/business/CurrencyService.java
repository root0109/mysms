package com.mysms.master.business;

import com.mysms.master.api.model.Currency;
import com.mysms.master.api.model.CurrencyUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.master.config.MasterDatabaseConfig;
import com.mysms.master.config.MasterEventsConfig;
import com.mysms.master.domain.CurrencyEntity;
import com.mysms.master.events.MasterEvents;
import com.mysms.master.mapper.CurrencyMapper;
import com.mysms.master.repository.CurrencyRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of master.currency (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change.
 */
@Service
public class CurrencyService extends AppTransactionManager {

    public static final String AGGREGATE = CurrencyEntity.TABLE;
    /** The contract's CurrencyUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("code", "name", "symbol", "minorUnits", "isActive");
    private static final int DEFAULT_MINOR_UNITS = 2;
    private static final int MAX_MINOR_UNITS = 4;

    private final CurrencyRepository repository;
    private final CurrencyMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public CurrencyService(CurrencyRepository repository, CurrencyMapper mapper,
                     @Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(MasterDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                     @Qualifier(MasterEventsConfig.OUTBOX) OutboxWriter outbox,
                     @Qualifier(MasterEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Currency getCurrency(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(load(id));
        });
    }

    public Page<Currency> listCurrencies(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CurrencyRepository.SORT_FIELDS, CurrencyRepository.DEFAULT_ORDER,
                CurrencyRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CurrencyRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CurrencyEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Currency createCurrency(Currency request, boolean validateOnly) {
        validator.validate(request);
        CurrencyEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        normalize(entity);
        return doInWriteTransaction(() -> {
            assertValid(entity);
            assertUnique(entity);
            if (!validateOnly) {
                repository.insert(entity);
                audit.recordCreate(AGGREGATE, entity.getId());
                outbox.publish(AGGREGATE + MasterEvents.CREATED, AGGREGATE, entity.getId(),
                        MasterEvents.payload(entity.getId()));
            }
            return mapper.toDto(entity);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole before it is written.
     */
    public Currency updateCurrency(UUID id, String ifMatch, CurrencyUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            CurrencyEntity entity = load(id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            normalize(entity);
            validator.validate(mapper.toDto(entity));
            assertValid(entity);
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (!validateOnly) {
                if (repository.update(entity, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                entity.setRowVersion(expected + 1);
                audit.recordUpdate(AGGREGATE, id, fields);
                outbox.publish(AGGREGATE + MasterEvents.UPDATED, AGGREGATE, id, MasterEvents.payload(id));
            }
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match. */
    public void deleteCurrency(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            CurrencyEntity entity = load(id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + MasterEvents.DELETED, AGGREGATE, id, MasterEvents.payload(id));
        });
    }

    private CurrencyEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** Applies the selected CurrencyUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(CurrencyEntity entity, CurrencyUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(upper(changes.getCode()));
                case "name" -> entity.setName(changes.getName());
                case "symbol" -> entity.setSymbol(changes.getSymbol());
                case "minorUnits" -> entity.setMinorUnits(changes.getMinorUnits());
                case "isActive" -> entity.setActive(!Boolean.FALSE.equals(changes.getIsActive()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    private static void normalize(CurrencyEntity entity) {
        entity.setCode(upper(entity.getCode()));
        if (entity.getMinorUnits() == null) {
            entity.setMinorUnits(DEFAULT_MINOR_UNITS);
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CurrencyEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() != null && !entity.getCode().matches("[A-Z]{3}")) {
            errors.add(new FieldError("code", "must be [A-Z]{3}", "Pattern"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getMinorUnits() != null && (entity.getMinorUnits() < 0 || entity.getMinorUnits() > MAX_MINOR_UNITS)) {
            errors.add(new FieldError("minorUnits", "must be between 0 and " + MAX_MINOR_UNITS, "Range"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CurrencyEntity entity) {
        if (repository.codeTaken(entity.getCode(), entity.getId())) {
            throw new DuplicateResourceException("A currency with this code already exists.");
        }
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
