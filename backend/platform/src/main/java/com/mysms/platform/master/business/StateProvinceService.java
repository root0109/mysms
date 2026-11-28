package com.mysms.platform.master.business;

import com.mysms.platform.master.api.model.StateProvince;
import com.mysms.platform.master.api.model.StateProvinceUpdate;
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
import com.mysms.core.web.Patches;
import com.mysms.platform.master.config.MasterDatabaseConfig;
import com.mysms.platform.master.config.MasterEventsConfig;
import com.mysms.platform.master.domain.StateProvinceEntity;
import com.mysms.platform.master.events.MasterEvents;
import com.mysms.platform.master.mapper.StateProvinceMapper;
import com.mysms.platform.master.repository.CountryRepository;
import com.mysms.platform.master.repository.StateProvinceRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/** Business service of master.state_province: audit and domain event in the same transaction as every change. */
@Service
public class StateProvinceService extends AppTransactionManager {

    public static final String AGGREGATE = StateProvinceEntity.TABLE;

    private final StateProvinceRepository repository;
    private final CountryRepository countries;
    private final StateProvinceMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StateProvinceService(StateProvinceRepository repository, CountryRepository countries, StateProvinceMapper mapper,
                     @Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(MasterDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                     @Qualifier(MasterEventsConfig.OUTBOX) OutboxWriter outbox,
                     @Qualifier(MasterEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.countries = countries;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public StateProvince getStateProvince(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(load(id));
        });
    }

    public Page<StateProvince> listStateProvinces(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, StateProvinceRepository.SORT_FIELDS, StateProvinceRepository.DEFAULT_ORDER,
                StateProvinceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StateProvinceRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StateProvinceEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StateProvince createStateProvince(StateProvince request, boolean validateOnly) {
        validator.validate(request);
        StateProvinceEntity entity = mapper.toEntity(request);
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
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole before it is written.
     */
    public StateProvince updateStateProvince(UUID id, String ifMatch, StateProvinceUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            StateProvinceEntity entity = load(id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            normalize(entity);
            validator.validate(mapper.toDto(entity));
            assertValid(entity);
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
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
    public void deleteStateProvince(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            StateProvinceEntity entity = load(id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.hasLiveCities(entity.getId())) {
                throw new InvalidStateException("The state / province still has cities; delete those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + MasterEvents.DELETED, AGGREGATE, id, MasterEvents.payload(id));
        });
    }

    private StateProvinceEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(StateProvinceEntity entity, StateProvinceUpdate changes) {
        Patches patch = new Patches();
        patch.apply("countryId", changes.getCountryId(), entity::setCountryId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("isActive", changes.getIsActive(), v -> entity.setActive(!Boolean.FALSE.equals(v)));
        return patch.applied();
    }

    private static void normalize(StateProvinceEntity entity) {

    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StateProvinceEntity entity) {
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
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StateProvinceEntity entity) {
        if (repository.nameTaken(entity.getCountryId(), entity.getName(), entity.getId())) {
            throw new DuplicateResourceException("A state / province with this name already exists in the country.");
        }
        if (entity.getCode() != null && repository.codeTaken(entity.getCountryId(), entity.getCode(), entity.getId())) {
            throw new DuplicateResourceException("A state / province with this code already exists in the country.");
        }
    }
}
