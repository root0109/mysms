package com.mysms.master.business;

import com.mysms.contracts.master.model.StateProvince;
import com.mysms.contracts.master.model.StateProvinceList;
import com.mysms.contracts.master.model.StateProvinceUpdate;
import com.mysms.core.context.Actor;
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
import com.mysms.master.domain.StateProvinceEntity;
import com.mysms.master.events.MasterEvents;
import com.mysms.master.mapper.StateProvinceMapper;
import com.mysms.master.repository.CountryRepository;
import com.mysms.master.repository.StateProvinceRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Business service of master.state_province (JAVA-CODING-RULES §4): one plain class, explicit methods, programmatic
 * transactions through the module's executor, audit + domain event in the same transaction as every change.
 */
@Service
public class StateProvinceService {

    public static final String AGGREGATE = StateProvinceEntity.TABLE;
    /** The contract's StateProvinceUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("countryId", "code", "name", "isActive");

    private final StateProvinceRepository repository;
    private final CountryRepository countries;
    private final StateProvinceMapper mapper;
    private final ModuleDatabase database;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StateProvinceService(StateProvinceRepository repository, CountryRepository countries, StateProvinceMapper mapper,
                     @Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database,
                     @Qualifier(MasterEventsConfig.OUTBOX) OutboxWriter outbox,
                     @Qualifier(MasterEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        this.repository = repository;
        this.countries = countries;
        this.mapper = mapper;
        this.database = database;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public StateProvince getStateProvince(UUID id) {
        return database.tx().read(() -> mapper.toDto(load(id)));
    }

    public StateProvinceList listStateProvinces(Integer pageSize, String pageToken, String filter, String orderBy) {
        SortSpec sort = SortSpec.parse(orderBy, StateProvinceRepository.SORT_FIELDS, StateProvinceRepository.DEFAULT_ORDER,
                StateProvinceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StateProvinceRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageSize, pageToken, sort, filter, clock);
        return database.tx().read(() -> {
            Page<StateProvinceEntity> rows = repository.list(page, where);
            return new StateProvinceList(rows.items().stream().map(mapper::toDto).toList())
                    .nextPageToken(rows.nextPageToken())
                    .totalSize(repository.count(where));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StateProvince createStateProvince(StateProvince request, boolean validateOnly) {
        validator.validate(request);
        StateProvinceEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        normalize(entity);
        return database.tx().write(() -> {
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
    public StateProvince updateStateProvince(UUID id, String ifMatch, StateProvinceUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return database.tx().write(() -> {
            StateProvinceEntity entity = load(id);
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
    public void deleteStateProvince(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        database.tx().write(() -> {
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
            return null;
        });
    }

    private StateProvinceEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** Applies the selected StateProvinceUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(StateProvinceEntity entity, StateProvinceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "isActive" -> entity.setActive(!Boolean.FALSE.equals(changes.getIsActive()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
