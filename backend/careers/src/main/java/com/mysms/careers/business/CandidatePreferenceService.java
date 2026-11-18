package com.mysms.careers.business;

import com.mysms.authclient.DataScope;
import com.mysms.careers.api.model.CandidatePreference;
import com.mysms.careers.api.model.CandidatePreferenceUpdate;
import com.mysms.careers.api.model.CandidatePreferredFunction;
import com.mysms.careers.api.model.CandidatePreferredFunctionUpdate;
import com.mysms.careers.api.model.CandidatePreferredIndustry;
import com.mysms.careers.api.model.CandidatePreferredIndustryUpdate;
import com.mysms.careers.config.CareersDatabaseConfig;
import com.mysms.careers.config.CareersEventsConfig;
import com.mysms.careers.domain.CandidatePreferenceEntity;
import com.mysms.careers.domain.CandidatePreferredFunctionEntity;
import com.mysms.careers.domain.CandidatePreferredIndustryEntity;
import com.mysms.careers.events.CareersEvents;
import com.mysms.careers.mapper.CandidatePreferenceMapper;
import com.mysms.careers.mapper.CandidatePreferredFunctionMapper;
import com.mysms.careers.mapper.CandidatePreferredIndustryMapper;
import com.mysms.careers.repository.CandidatePreferenceRepository;
import com.mysms.careers.repository.CandidatePreferredFunctionRepository;
import com.mysms.careers.repository.CandidatePreferredIndustryRepository;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of careers.candidate_preference and of the records that exist only as part of a CandidatePreference
 * (CandidatePreferredFunction, CandidatePreferredIndustry): audit and domain event commit in the same transaction as
 * every change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a
 * tenant-wide grant.
 */
@Service
public class CandidatePreferenceService extends AppTransactionManager {

    public static final String AGGREGATE = CandidatePreferenceEntity.TABLE;
    static final String PERMISSION_LIST = "careers.candidate_preference.read";
    static final String PERMISSION_GET = "careers.candidate_preference.read";
    static final String PERMISSION_CREATE = "careers.candidate_preference.create";
    static final String PERMISSION_UPDATE = "careers.candidate_preference.update";
    static final String PERMISSION_DELETE = "careers.candidate_preference.delete";
    /** The contract's CandidatePreferenceUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("personId", "openTo", "preferredLocation", "dreamCompanies", "attire", "collaboration",
                    "roleClarity",
            "locationPreference", "workHours", "companyType");

    public static final String CANDIDATE_PREFERRED_FUNCTION_AGGREGATE = CandidatePreferredFunctionEntity.TABLE;
    static final String CANDIDATE_PREFERRED_FUNCTION_PERMISSION_LIST = "careers.candidate_preferred_function.read";
    static final String CANDIDATE_PREFERRED_FUNCTION_PERMISSION_GET = "careers.candidate_preferred_function.read";
    static final String CANDIDATE_PREFERRED_FUNCTION_PERMISSION_CREATE = "careers.candidate_preferred_function.create";
    static final String CANDIDATE_PREFERRED_FUNCTION_PERMISSION_UPDATE = "careers.candidate_preferred_function.update";
    static final String CANDIDATE_PREFERRED_FUNCTION_PERMISSION_DELETE = "careers.candidate_preferred_function.delete";
    /** The contract's CandidatePreferredFunctionUpdate fields, in apply order. */
    static final List<String> CANDIDATE_PREFERRED_FUNCTION_WRITABLE = List.of("functionAreaId");

    public static final String CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE = CandidatePreferredIndustryEntity.TABLE;
    static final String CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_LIST = "careers.candidate_preferred_industry.read";
    static final String CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_GET = "careers.candidate_preferred_industry.read";
    static final String CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_CREATE = "careers.candidate_preferred_industry.create";
    static final String CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_UPDATE = "careers.candidate_preferred_industry.update";
    static final String CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_DELETE = "careers.candidate_preferred_industry.delete";
    /** The contract's CandidatePreferredIndustryUpdate fields, in apply order. */
    static final List<String> CANDIDATE_PREFERRED_INDUSTRY_WRITABLE = List.of("industryId");

    private final CandidatePreferenceRepository repository;
    private final CandidatePreferenceMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CandidatePreferredFunctionRepository candidatePreferredFunctionRepository;
    private final CandidatePreferredFunctionMapper candidatePreferredFunctionMapper;
    private final CandidatePreferredIndustryRepository candidatePreferredIndustryRepository;
    private final CandidatePreferredIndustryMapper candidatePreferredIndustryMapper;

    public CandidatePreferenceService(CandidatePreferenceRepository repository,
            CandidatePreferenceMapper mapper,
            @Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CareersDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CareersEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CareersEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            CandidatePreferredFunctionRepository candidatePreferredFunctionRepository,
            CandidatePreferredFunctionMapper candidatePreferredFunctionMapper,
            CandidatePreferredIndustryRepository candidatePreferredIndustryRepository,
            CandidatePreferredIndustryMapper candidatePreferredIndustryMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.candidatePreferredFunctionRepository = candidatePreferredFunctionRepository;
        this.candidatePreferredFunctionMapper = candidatePreferredFunctionMapper;
        this.candidatePreferredIndustryRepository = candidatePreferredIndustryRepository;
        this.candidatePreferredIndustryMapper = candidatePreferredIndustryMapper;
    }

    public CandidatePreference getCandidatePreference(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<CandidatePreference> listCandidatePreferences(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CandidatePreferenceRepository.SORT_FIELDS,
                        CandidatePreferenceRepository.DEFAULT_ORDER,
                CandidatePreferenceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CandidatePreferenceRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CandidatePreferenceEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CandidatePreference createCandidatePreference(CandidatePreference request, boolean validateOnly) {
        validator.validate(request);
        CandidatePreferenceEntity entity = mapper.toEntity(request);
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
            CandidatePreferenceEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CareersEvents.CREATED, AGGREGATE, stored.getId(),
                    CareersEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public CandidatePreference updateCandidatePreference(UUID id, String ifMatch, CandidatePreferenceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CandidatePreferenceEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CareersEvents.UPDATED, AGGREGATE, id, CareersEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCandidatePreference(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CandidatePreferenceEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCandidatePreferredFunctionCandidatePreferenceId(id)) {
                throw new InvalidStateException(
                        "The candidate preference is still referenced by careers.candidate_preferred_function rows; "
                                + "remove those first.");
            }
            if (repository.referencedByCandidatePreferredIndustryCandidatePreferenceId(id)) {
                throw new InvalidStateException(
                        "The candidate preference is still referenced by careers.candidate_preferred_industry rows; "
                                + "remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CareersEvents.DELETED, AGGREGATE, id, CareersEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CandidatePreferenceEntity loadVisible(DataScope scope, UUID id) {
        CandidatePreferenceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(CandidatePreferenceEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected CandidatePreferenceUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(CandidatePreferenceEntity entity, CandidatePreferenceUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "personId" -> entity.setPersonId(changes.getPersonId());
                case "openTo" -> entity.setOpenTo(Optional.ofNullable(changes.getOpenTo()).map(v -> v.getValue())
                        .orElse(null));
                case "preferredLocation" -> entity.setPreferredLocation(changes.getPreferredLocation());
                case "dreamCompanies" -> entity.setDreamCompanies(changes.getDreamCompanies());
                case "attire" -> entity.setAttire(Optional.ofNullable(changes.getAttire()).map(v -> v.getValue())
                        .orElse(null));
                case "collaboration" -> entity.setCollaboration(Optional.ofNullable(changes.getCollaboration())
                        .map(v -> v.getValue()).orElse(null));
                case "roleClarity" -> entity.setRoleClarity(Optional.ofNullable(changes.getRoleClarity())
                        .map(v -> v.getValue()).orElse(null));
                case "locationPreference" -> entity.setLocationPreference(
                        Optional.ofNullable(changes.getLocationPreference()).map(v -> v.getValue()).orElse(null));
                case "workHours" -> entity.setWorkHours(Optional.ofNullable(changes.getWorkHours())
                        .map(v -> v.getValue()).orElse(null));
                case "companyType" -> entity.setCompanyType(Optional.ofNullable(changes.getCompanyType())
                        .map(v -> v.getValue()).orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CandidatePreferenceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CandidatePreferenceEntity entity) {
        if (repository.takenPersonIdId(entity)) {
            throw new DuplicateResourceException("A candidate preference with the same person id already exists.");
        }
    }

    // ---- CandidatePreferredFunction: a record of the CandidatePreference aggregate ----

    public CandidatePreferredFunction getCandidatePreferredFunction(UUID candidatePreferenceId, UUID id) {
        return doInReadTransaction(() -> {
            return candidatePreferredFunctionMapper.toDto(loadVisibleCandidatePreferredFunction(DataScope.current(
                    CANDIDATE_PREFERRED_FUNCTION_PERMISSION_GET), candidatePreferenceId, id));
        });
    }

    public Page<CandidatePreferredFunction> listCandidatePreferredFunctions(UUID candidatePreferenceId,
            Integer pageNumber, Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CandidatePreferredFunctionRepository.SORT_FIELDS,
                        CandidatePreferredFunctionRepository.DEFAULT_ORDER,
                CandidatePreferredFunctionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CandidatePreferredFunctionRepository.FILTER_FIELDS)
                .and(CandidatePreferredFunctionRepository.ofParent(candidatePreferenceId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + candidatePreferenceId, clock);
        return doInReadTransaction(() -> {
            Page<CandidatePreferredFunctionEntity> rows = candidatePreferredFunctionRepository.list(page, where);
            return rows.map(candidatePreferredFunctionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CandidatePreferredFunction createCandidatePreferredFunction(UUID candidatePreferenceId,
            CandidatePreferredFunction request, boolean validateOnly) {
        validator.validate(request);
        CandidatePreferredFunctionEntity entity = candidatePreferredFunctionMapper.toEntity(request);
        entity.setCandidatePreferenceId(candidatePreferenceId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CANDIDATE_PREFERRED_FUNCTION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!candidatePreferredFunctionRepository.existsCandidatePreferenceId(entity.getCandidatePreferenceId())) {
                throw new ResourceNotFoundException("careers.candidate_preference", entity.getCandidatePreferenceId());
            }
            assertValidCandidatePreferredFunction(entity, true);
            scope.requireWritable(institutionOfCandidatePreferredFunction(entity));
            assertUniqueCandidatePreferredFunction(entity);
            if (validateOnly) {
                return candidatePreferredFunctionMapper.toDto(entity);
            }
            CandidatePreferredFunctionEntity stored = candidatePreferredFunctionRepository.insert(entity);
            audit.recordCreate(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, stored.getId());
            outbox.publish(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE + CareersEvents.CREATED,
                    CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, stored.getId(),
                    CareersEvents.payload(stored.getId()));
            return candidatePreferredFunctionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public CandidatePreferredFunction updateCandidatePreferredFunction(UUID candidatePreferenceId, UUID id,
            String ifMatch, CandidatePreferredFunctionUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CANDIDATE_PREFERRED_FUNCTION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CANDIDATE_PREFERRED_FUNCTION_PERMISSION_UPDATE);
            CandidatePreferredFunctionEntity entity = loadVisibleCandidatePreferredFunction(scope, candidatePreferenceId, id);
            scope.requireWritable(institutionOfCandidatePreferredFunction(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCandidatePreferredFunction(entity, changes, fields);
            validator.validate(candidatePreferredFunctionMapper.toDto(entity));
            assertValidCandidatePreferredFunction(entity, false);
            scope.requireWritable(institutionOfCandidatePreferredFunction(entity));
            assertUniqueCandidatePreferredFunction(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return candidatePreferredFunctionMapper.toDto(entity);
            }
            if (candidatePreferredFunctionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, id, fields);
            outbox.publish(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE + CareersEvents.UPDATED,
                    CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, id, CareersEvents.payload(id));
            return candidatePreferredFunctionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCandidatePreferredFunction(UUID candidatePreferenceId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CANDIDATE_PREFERRED_FUNCTION_PERMISSION_DELETE);
            CandidatePreferredFunctionEntity entity = loadVisibleCandidatePreferredFunction(scope, candidatePreferenceId, id);
            scope.requireWritable(institutionOfCandidatePreferredFunction(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (candidatePreferredFunctionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, id);
            outbox.publish(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE + CareersEvents.DELETED,
                    CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, id, CareersEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CandidatePreferredFunctionEntity loadVisibleCandidatePreferredFunction(DataScope scope,
            UUID candidatePreferenceId, UUID id) {
        CandidatePreferredFunctionEntity entity = candidatePreferredFunctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, id));
        if (!scope.canRead(institutionOfCandidatePreferredFunction(entity))
                || !candidatePreferenceId.equals(entity.getCandidatePreferenceId())) {
            throw new ResourceNotFoundException(CANDIDATE_PREFERRED_FUNCTION_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfCandidatePreferredFunction(CandidatePreferredFunctionEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected CandidatePreferredFunctionUpdate fields; explicit per field so a mask can clear values. */
    private static void applyCandidatePreferredFunction(CandidatePreferredFunctionEntity entity,
            CandidatePreferredFunctionUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "functionAreaId" -> entity.setFunctionAreaId(changes.getFunctionAreaId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCandidatePreferredFunction(CandidatePreferredFunctionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCandidatePreferenceId() == null) {
            errors.add(new FieldError("candidatePreferenceId", "must not be null", "NotNull"));
        }
        if (entity.getFunctionAreaId() == null) {
            errors.add(new FieldError("functionAreaId", "must not be null", "NotNull"));
        }
        if (entity.getCandidatePreferenceId() != null
                && !candidatePreferredFunctionRepository.existsCandidatePreferenceId(entity.getCandidatePreferenceId())) {
            errors.add(new FieldError("candidatePreferenceId", "candidate preference does not exist", "NOT_FOUND"));
        }
        if (entity.getFunctionAreaId() != null
                && !candidatePreferredFunctionRepository.existsFunctionAreaId(entity.getFunctionAreaId())) {
            errors.add(new FieldError("functionAreaId", "function area does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCandidatePreferredFunction(CandidatePreferredFunctionEntity entity) {
        if (candidatePreferredFunctionRepository.takenCandidatePreferenceIdFunctionAreaId1(entity)) {
            throw new DuplicateResourceException(
                    "A candidate preferred function with the same candidate preference id and function area id "
                            + "already exists.");
        }
    }

    // ---- CandidatePreferredIndustry: a record of the CandidatePreference aggregate ----

    public CandidatePreferredIndustry getCandidatePreferredIndustry(UUID candidatePreferenceId, UUID id) {
        return doInReadTransaction(() -> {
            return candidatePreferredIndustryMapper.toDto(loadVisibleCandidatePreferredIndustry(DataScope.current(
                    CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_GET), candidatePreferenceId, id));
        });
    }

    public Page<CandidatePreferredIndustry> listCandidatePreferredIndustrys(UUID candidatePreferenceId,
            Integer pageNumber, Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CandidatePreferredIndustryRepository.SORT_FIELDS,
                        CandidatePreferredIndustryRepository.DEFAULT_ORDER,
                CandidatePreferredIndustryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CandidatePreferredIndustryRepository.FILTER_FIELDS)
                .and(CandidatePreferredIndustryRepository.ofParent(candidatePreferenceId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + candidatePreferenceId, clock);
        return doInReadTransaction(() -> {
            Page<CandidatePreferredIndustryEntity> rows = candidatePreferredIndustryRepository.list(page, where);
            return rows.map(candidatePreferredIndustryMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CandidatePreferredIndustry createCandidatePreferredIndustry(UUID candidatePreferenceId,
            CandidatePreferredIndustry request, boolean validateOnly) {
        validator.validate(request);
        CandidatePreferredIndustryEntity entity = candidatePreferredIndustryMapper.toEntity(request);
        entity.setCandidatePreferenceId(candidatePreferenceId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!candidatePreferredIndustryRepository.existsCandidatePreferenceId(entity.getCandidatePreferenceId())) {
                throw new ResourceNotFoundException("careers.candidate_preference", entity.getCandidatePreferenceId());
            }
            assertValidCandidatePreferredIndustry(entity, true);
            scope.requireWritable(institutionOfCandidatePreferredIndustry(entity));
            assertUniqueCandidatePreferredIndustry(entity);
            if (validateOnly) {
                return candidatePreferredIndustryMapper.toDto(entity);
            }
            CandidatePreferredIndustryEntity stored = candidatePreferredIndustryRepository.insert(entity);
            audit.recordCreate(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, stored.getId());
            outbox.publish(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE + CareersEvents.CREATED,
                    CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, stored.getId(),
                    CareersEvents.payload(stored.getId()));
            return candidatePreferredIndustryMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public CandidatePreferredIndustry updateCandidatePreferredIndustry(UUID candidatePreferenceId, UUID id,
            String ifMatch, CandidatePreferredIndustryUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CANDIDATE_PREFERRED_INDUSTRY_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_UPDATE);
            CandidatePreferredIndustryEntity entity = loadVisibleCandidatePreferredIndustry(scope, candidatePreferenceId, id);
            scope.requireWritable(institutionOfCandidatePreferredIndustry(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCandidatePreferredIndustry(entity, changes, fields);
            validator.validate(candidatePreferredIndustryMapper.toDto(entity));
            assertValidCandidatePreferredIndustry(entity, false);
            scope.requireWritable(institutionOfCandidatePreferredIndustry(entity));
            assertUniqueCandidatePreferredIndustry(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return candidatePreferredIndustryMapper.toDto(entity);
            }
            if (candidatePreferredIndustryRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, id, fields);
            outbox.publish(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE + CareersEvents.UPDATED,
                    CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, id, CareersEvents.payload(id));
            return candidatePreferredIndustryMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCandidatePreferredIndustry(UUID candidatePreferenceId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CANDIDATE_PREFERRED_INDUSTRY_PERMISSION_DELETE);
            CandidatePreferredIndustryEntity entity = loadVisibleCandidatePreferredIndustry(scope, candidatePreferenceId, id);
            scope.requireWritable(institutionOfCandidatePreferredIndustry(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (candidatePreferredIndustryRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, id);
            outbox.publish(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE + CareersEvents.DELETED,
                    CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, id, CareersEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CandidatePreferredIndustryEntity loadVisibleCandidatePreferredIndustry(DataScope scope,
            UUID candidatePreferenceId, UUID id) {
        CandidatePreferredIndustryEntity entity = candidatePreferredIndustryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, id));
        if (!scope.canRead(institutionOfCandidatePreferredIndustry(entity))
                || !candidatePreferenceId.equals(entity.getCandidatePreferenceId())) {
            throw new ResourceNotFoundException(CANDIDATE_PREFERRED_INDUSTRY_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfCandidatePreferredIndustry(CandidatePreferredIndustryEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected CandidatePreferredIndustryUpdate fields; explicit per field so a mask can clear values. */
    private static void applyCandidatePreferredIndustry(CandidatePreferredIndustryEntity entity,
            CandidatePreferredIndustryUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "industryId" -> entity.setIndustryId(changes.getIndustryId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCandidatePreferredIndustry(CandidatePreferredIndustryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCandidatePreferenceId() == null) {
            errors.add(new FieldError("candidatePreferenceId", "must not be null", "NotNull"));
        }
        if (entity.getIndustryId() == null) {
            errors.add(new FieldError("industryId", "must not be null", "NotNull"));
        }
        if (entity.getCandidatePreferenceId() != null
                && !candidatePreferredIndustryRepository.existsCandidatePreferenceId(entity.getCandidatePreferenceId())) {
            errors.add(new FieldError("candidatePreferenceId", "candidate preference does not exist", "NOT_FOUND"));
        }
        if (entity.getIndustryId() != null && !candidatePreferredIndustryRepository.existsIndustryId(entity.getIndustryId())) {
            errors.add(new FieldError("industryId", "industry does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCandidatePreferredIndustry(CandidatePreferredIndustryEntity entity) {
        if (candidatePreferredIndustryRepository.takenCandidatePreferenceIdIndustryId1(entity)) {
            throw new DuplicateResourceException(
                    "A candidate preferred industry with the same candidate preference id and industry id already "
                            + "exists.");
        }
    }
}
