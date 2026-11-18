package com.mysms.master.business;

import com.mysms.master.api.model.Country;
import com.mysms.master.api.model.CountryUpdate;
import com.mysms.core.context.Actor;
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
import com.mysms.master.config.MasterDatabaseConfig;
import com.mysms.master.config.MasterEventsConfig;
import com.mysms.master.domain.CountryEntity;
import com.mysms.master.events.MasterEvents;
import com.mysms.master.mapper.CountryMapper;
import com.mysms.master.repository.CountryRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/** Business service of master.country: audit and domain event in the same transaction as every change. */
@Service
public class CountryService extends AppTransactionManager {

    public static final String AGGREGATE = CountryEntity.TABLE;
    /** The contract's CountryUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("iso2", "iso3", "name", "shortName", "phonePrefix", "sortOrder", "isActive");

    private final CountryRepository repository;
    private final CountryMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public CountryService(CountryRepository repository, CountryMapper mapper,
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

    public Country getCountry(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(load(id));
        });
    }

    public Page<Country> listCountries(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CountryRepository.SORT_FIELDS, CountryRepository.DEFAULT_ORDER,
                CountryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CountryRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CountryEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Country createCountry(Country request, boolean validateOnly) {
        validator.validate(request);
        CountryEntity entity = mapper.toEntity(request);
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
    public Country updateCountry(UUID id, String ifMatch, CountryUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            CountryEntity entity = load(id);
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
    public void deleteCountry(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            CountryEntity entity = load(id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.hasLiveStatesOrCities(entity.getId())) {
                throw new InvalidStateException("The country still has states or cities; delete those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + MasterEvents.DELETED, AGGREGATE, id, MasterEvents.payload(id));
        });
    }

    private CountryEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** Applies the selected CountryUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(CountryEntity entity, CountryUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "iso2" -> entity.setIso2(upper(changes.getIso2()));
                case "iso3" -> entity.setIso3(upper(changes.getIso3()));
                case "name" -> entity.setName(changes.getName());
                case "shortName" -> entity.setShortName(changes.getShortName());
                case "phonePrefix" -> entity.setPhonePrefix(changes.getPhonePrefix());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "isActive" -> entity.setActive(!Boolean.FALSE.equals(changes.getIsActive()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    private static void normalize(CountryEntity entity) {
        entity.setIso2(upper(entity.getIso2()));
        entity.setIso3(upper(entity.getIso3()));
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CountryEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getIso2() != null && !entity.getIso2().matches("[A-Z]{2}")) {
            errors.add(new FieldError("iso2", "must be [A-Z]{2}", "Pattern"));
        }
        if (entity.getIso3() != null && !entity.getIso3().matches("[A-Z]{3}")) {
            errors.add(new FieldError("iso3", "must be [A-Z]{3}", "Pattern"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CountryEntity entity) {
        if (entity.getIso2() != null && repository.iso2Taken(entity.getIso2(), entity.getId())) {
            throw new DuplicateResourceException("A country with this iso2 already exists.");
        }
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
