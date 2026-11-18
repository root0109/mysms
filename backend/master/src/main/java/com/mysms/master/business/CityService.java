package com.mysms.master.business;

import com.mysms.master.api.model.City;
import com.mysms.master.api.model.CityUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
import com.mysms.master.domain.CityEntity;
import com.mysms.master.domain.StateProvinceEntity;
import com.mysms.master.events.MasterEvents;
import com.mysms.master.mapper.CityMapper;
import com.mysms.master.repository.CityRepository;
import com.mysms.master.repository.CountryRepository;
import com.mysms.master.repository.StateProvinceRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/** Business service of master.city: audit and domain event in the same transaction as every change. */
@Service
public class CityService extends AppTransactionManager {

    public static final String AGGREGATE = CityEntity.TABLE;
    /** The contract's CityUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("countryId", "stateProvinceId", "name", "isActive");

    private final CityRepository repository;
    private final CountryRepository countries;
    private final StateProvinceRepository states;
    private final CityMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public CityService(CityRepository repository, CountryRepository countries, StateProvinceRepository states, CityMapper mapper,
                     @Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(MasterDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                     @Qualifier(MasterEventsConfig.OUTBOX) OutboxWriter outbox,
                     @Qualifier(MasterEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.countries = countries;
        this.states = states;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public City getCity(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(load(id));
        });
    }

    public Page<City> listCities(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CityRepository.SORT_FIELDS, CityRepository.DEFAULT_ORDER,
                CityRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CityRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CityEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public City createCity(City request, boolean validateOnly) {
        validator.validate(request);
        CityEntity entity = mapper.toEntity(request);
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
    public City updateCity(UUID id, String ifMatch, CityUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            CityEntity entity = load(id);
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
    public void deleteCity(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            CityEntity entity = load(id);
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

    private CityEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** Applies the selected CityUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(CityEntity entity, CityUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                case "stateProvinceId" -> entity.setStateProvinceId(changes.getStateProvinceId());
                case "name" -> entity.setName(changes.getName());
                case "isActive" -> entity.setActive(!Boolean.FALSE.equals(changes.getIsActive()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    private static void normalize(CityEntity entity) {

    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CityEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCountryId() == null) {
            errors.add(new FieldError("countryId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getCountryId() != null && !countries.existsLive(entity.getCountryId())) {
            errors.add(new FieldError("countryId", "country does not exist", "NOT_FOUND"));
        }
        if (entity.getStateProvinceId() != null) {
            Optional<StateProvinceEntity> state = states.findById(entity.getStateProvinceId());
            if (state.isEmpty()) {
                errors.add(new FieldError("stateProvinceId", "state / province does not exist", "NOT_FOUND"));
            } else if (!state.get().getCountryId().equals(entity.getCountryId())) {
                errors.add(new FieldError("stateProvinceId", "state / province belongs to another country", "MISMATCH"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CityEntity entity) {
        // cities have no business key: the same name may exist in several states
    }
}
