package com.mysms.org.business;

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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.org.api.model.AcademicTerm;
import com.mysms.org.api.model.AcademicTermList;
import com.mysms.org.api.model.AcademicTermUpdate;
import com.mysms.org.api.model.AcademicYear;
import com.mysms.org.api.model.AcademicYearUpdate;
import com.mysms.org.api.model.ActivateAcademicYearRequest;
import com.mysms.org.api.model.CloseAcademicYearRequest;
import com.mysms.org.config.OrgDatabaseConfig;
import com.mysms.org.config.OrgEventsConfig;
import com.mysms.org.domain.AcademicTermEntity;
import com.mysms.org.domain.AcademicYearEntity;
import com.mysms.org.events.OrgEvents;
import com.mysms.org.mapper.AcademicTermMapper;
import com.mysms.org.mapper.AcademicYearMapper;
import com.mysms.org.repository.AcademicTermRepository;
import com.mysms.org.repository.AcademicYearRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of org.academic_year: audit and domain event in the same transaction as every change; rows outside
 * the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the AcademicYear aggregate: AcademicTerm.
 */
@Service
public class AcademicYearService extends AppTransactionManager {

    public static final String AGGREGATE = AcademicYearEntity.TABLE;
    static final String PERMISSION_LIST = "org.academic_year.read";
    static final String PERMISSION_GET = "org.academic_year.read";
    static final String PERMISSION_CREATE = "org.academic_year.create";
    static final String PERMISSION_UPDATE = "org.academic_year.update";
    static final String PERMISSION_DELETE = "org.academic_year.delete";
    static final String PERMISSION_CLOSE_ACADEMIC_YEAR = "org.academic_year.close_academic_year";
    static final String PERMISSION_ACTIVATE_ACADEMIC_YEAR = "org.academic_year.activate_academic_year";
    static final String PERMISSION_GENERATE_TERMS = "org.academic_year.generate_terms";
    /** The contract's AcademicYearUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "name", "description", "startDate", "endDate", "termStructure", "isCurrent");

    public static final String ACADEMIC_TERM_AGGREGATE = AcademicTermEntity.TABLE;
    static final String ACADEMIC_TERM_PERMISSION_LIST = "org.academic_term.read";
    static final String ACADEMIC_TERM_PERMISSION_GET = "org.academic_term.read";
    static final String ACADEMIC_TERM_PERMISSION_CREATE = "org.academic_term.create";
    static final String ACADEMIC_TERM_PERMISSION_UPDATE = "org.academic_term.update";
    static final String ACADEMIC_TERM_PERMISSION_DELETE = "org.academic_term.delete";
    /** The contract's AcademicTermUpdate fields, in apply order. */
    static final List<String> ACADEMIC_TERM_WRITABLE =
            List.of("parentTermId", "name", "startDate", "endDate", "sequence", "code", "isActive");

    private final AcademicYearRepository repository;
    private final AcademicYearMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final AcademicTermRepository academicTermRepository;
    private final AcademicTermMapper academicTermMapper;

    public AcademicYearService(AcademicYearRepository repository,
            AcademicYearMapper mapper,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(OrgEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(OrgEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            AcademicTermRepository academicTermRepository,
            AcademicTermMapper academicTermMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.academicTermRepository = academicTermRepository;
        this.academicTermMapper = academicTermMapper;
    }

    public AcademicYear getAcademicYear(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AcademicYear> listAcademicYears(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AcademicYearRepository.SORT_FIELDS, AcademicYearRepository.DEFAULT_ORDER,
                AcademicYearRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AcademicYearRepository.FILTER_FIELDS)
                .and(AcademicYearRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AcademicYearEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AcademicYear createAcademicYear(AcademicYear request, boolean validateOnly) {
        validator.validate(request);
        AcademicYearEntity entity = mapper.toEntity(request);
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
            AcademicYearEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + OrgEvents.CREATED, AGGREGATE, stored.getId(), OrgEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AcademicYear updateAcademicYear(UUID id, String ifMatch, AcademicYearUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AcademicYearEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + OrgEvents.UPDATED, AGGREGATE, id, OrgEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAcademicYear(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AcademicYearEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAcademicTermAcademicYearId(id)) {
                throw new InvalidStateException(
                        "The academic year is still referenced by org.academic_term rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + OrgEvents.DELETED, AGGREGATE, id, OrgEvents.payload(id));
        });
    }

    /**
     * org.CloseAcademicYear: Close the year (read-only afterwards). The row must be visible and writable for the
     * caller, and the step is allowed while status is active; in any other state
     * the answer is 409 and nothing changes.
     */
    public AcademicYear closeAcademicYear(UUID id, CloseAcademicYearRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CLOSE_ACADEMIC_YEAR);
        return doInWriteTransaction(() -> {
            AcademicYearEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("active").contains(entity.getStatus()))) {
                throw new InvalidStateException("The academic year cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.closeAcademicYear(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "close", List.of("status", "isCurrent"), null);
            outbox.publish(AGGREGATE + ".close", AGGREGATE, id, OrgEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * org.ActivateAcademicYear: a planned year becomes its institution's active and current year (any other year
     * marked current loses the mark). One active year per institution: while another is active the answer is 409 —
     * close it first. Publishes {@code org.academic_year.activate} with the institution for the modules that follow
     * the current year.
     */
    public AcademicYear activateAcademicYear(UUID id, ActivateAcademicYearRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ACTIVATE_ACADEMIC_YEAR);
        OptionalInt bodyVersion = Etags.bodyVersion(request == null ? null : request.getEtag());
        return doInWriteTransaction(() -> {
            AcademicYearEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (bodyVersion.isPresent() && bodyVersion.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"planned".equals(entity.getStatus())) {
                throw new InvalidStateException("The academic year cannot be changed this way in its current state.");
            }
            UUID institution = entity.getInstitutionId();
            if (repository.otherActive(institution, id)) {
                throw new InvalidStateException(
                        "Another academic year of the institution is active; close it before activating this one.");
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            for (UUID other : repository.lockOtherCurrent(institution, id)) {
                repository.clearCurrent(other, now, actor);
                audit.recordUpdate(AGGREGATE, other, List.of("isCurrent"));
                outbox.publish(AGGREGATE + OrgEvents.UPDATED, AGGREGATE, other, OrgEvents.payload(other));
            }
            if (repository.activateAcademicYear(id, entity.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "activate", List.of("status", "isCurrent"), null);
            outbox.publish(AGGREGATE + ".activate", AGGREGATE, id,
                    Map.of("id", id.toString(), "institutionId", institution.toString()));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * org.GenerateTerms: creates the terms the year's term structure divides it into ({@link TermPlan}), quarters
     * inside their semesters. Idempotent: a planned term that already exists with its name and dates is kept, so a
     * re-run creates nothing, also when runs overlap (each waits for the year's row lock); a planned term that would
     * overlap another live term at its level is refused (409), as is a closed year, and a year without a structure to
     * plan from is 422. Answers every planned term, in order.
     */
    public AcademicTermList generateTerms(UUID academicYearId) {
        DataScope scope = DataScope.current(PERMISSION_GENERATE_TERMS);
        return doInWriteTransaction(() -> {
            loadVisible(scope, academicYearId);
            AcademicYearEntity year = repository.lockById(academicYearId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, academicYearId));
            scope.requireWritable(institutionOf(year));
            if ("closed".equals(year.getStatus())) {
                throw new InvalidStateException("A closed academic year no longer changes.");
            }
            List<TermPlan.Term> plan = TermPlan.of(year.getTermStructure(), year.getStartDate(), year.getEndDate())
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("termStructure",
                            "the year has no term structure to generate terms from", "TERM_STRUCTURE"))));
            if (!TermPlan.fits(year.getTermStructure(), year.getStartDate(), year.getEndDate())) {
                throw new ValidationFailedException(List.of(new FieldError("endDate",
                        "the year is too short for its term structure", "TERM_STRUCTURE")));
            }
            List<AcademicTermEntity> existing = new ArrayList<>(academicTermRepository.ofYear(academicYearId));
            AcademicTermList result = new AcademicTermList();
            for (TermPlan.Term term : plan) {
                AcademicTermEntity parent = placeTerm(year, null, term, existing, result);
                for (TermPlan.Term child : term.children()) {
                    placeTerm(year, parent.getId(), child, existing, result);
                }
            }
            return result;
        });
    }

    /** The existing term the plan names, or the new one written for it; refused when it would overlap another. */
    private AcademicTermEntity placeTerm(AcademicYearEntity year, UUID parentId, TermPlan.Term term,
            List<AcademicTermEntity> existing, AcademicTermList result) {
        AcademicTermEntity placed = null;
        for (AcademicTermEntity other : existing) {
            if (!Objects.equals(other.getParentTermId(), parentId)) {
                continue;
            }
            boolean same = other.getName().equals(term.name()) && other.getStartDate().equals(term.startDate())
                    && other.getEndDate().equals(term.endDate());
            if (same) {
                placed = other;
            } else if (overlaps(other, term.startDate(), term.endDate()) || other.getName().equals(term.name())) {
                throw new InvalidStateException("The year already has terms that overlap the ones its structure "
                        + "plans; remove or change them first.");
            }
        }
        if (placed == null) {
            AcademicTermEntity entity = new AcademicTermEntity();
            entity.onCreatingData(Actor.current(), clock);
            entity.setTenantId(year.getTenantId());
            entity.setAcademicYearId(year.getId());
            entity.setParentTermId(parentId);
            entity.setName(term.name());
            entity.setStartDate(term.startDate());
            entity.setEndDate(term.endDate());
            entity.setSequence(term.sequence());
            entity.setIsActive(true);
            placed = academicTermRepository.insert(entity);
            existing.add(placed);
            audit.recordCreate(ACADEMIC_TERM_AGGREGATE, placed.getId());
            outbox.publish(ACADEMIC_TERM_AGGREGATE + OrgEvents.CREATED, ACADEMIC_TERM_AGGREGATE, placed.getId(),
                    OrgEvents.payload(placed.getId()));
        }
        result.addItemsItem(academicTermMapper.toDto(placed));
        return placed;
    }

    private static boolean overlaps(AcademicTermEntity term, LocalDate start, LocalDate end) {
        return !term.getStartDate().isAfter(end) && !term.getEndDate().isBefore(start);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AcademicYearEntity loadVisible(DataScope scope, UUID id) {
        AcademicYearEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(AcademicYearEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected AcademicYearUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(AcademicYearEntity entity, AcademicYearUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "name" -> entity.setName(changes.getName());
                case "description" -> entity.setDescription(changes.getDescription());
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                case "termStructure" -> entity.setTermStructure(Optional.ofNullable(changes.getTermStructure())
                        .map(v -> v.getValue()).orElse(null));
                case "isCurrent" -> entity.setIsCurrent(changes.getIsCurrent());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AcademicYearEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getStartDate() == null) {
            errors.add(new FieldError("startDate", "must not be null", "NotNull"));
        }
        if (entity.getEndDate() == null) {
            errors.add(new FieldError("endDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsCurrent() == null) {
            errors.add(new FieldError("isCurrent", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() != null && !repository.existsInstitutionId(entity.getInstitutionId())) {
            errors.add(new FieldError("institutionId", "institution does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AcademicYearEntity entity) {
        if (repository.takenInstitutionIdName1(entity)) {
            throw new DuplicateResourceException(
                    "A academic year with the same institution id and name already exists.");
        }
    }

    // ---- AcademicTerm: a record of the AcademicYear aggregate ----

    public AcademicTerm getAcademicTerm(UUID academicYearId, UUID id) {
        return doInReadTransaction(() -> {
            return academicTermMapper.toDto(loadVisibleAcademicTerm(DataScope.current(ACADEMIC_TERM_PERMISSION_GET),
                    academicYearId, id));
        });
    }

    public Page<AcademicTerm> listAcademicTerms(UUID academicYearId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AcademicTermRepository.SORT_FIELDS, AcademicTermRepository.DEFAULT_ORDER,
                AcademicTermRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AcademicTermRepository.FILTER_FIELDS)
                .and(AcademicTermRepository.ofParent(academicYearId))
                        .and(AcademicTermRepository.scope(DataScope.current(ACADEMIC_TERM_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + academicYearId, clock);
        return doInReadTransaction(() -> {
            Page<AcademicTermEntity> rows = academicTermRepository.list(page, where);
            return rows.map(academicTermMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AcademicTerm createAcademicTerm(UUID academicYearId, AcademicTerm request, boolean validateOnly) {
        validator.validate(request);
        AcademicTermEntity entity = academicTermMapper.toEntity(request);
        entity.setAcademicYearId(academicYearId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ACADEMIC_TERM_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!academicTermRepository.existsAcademicYearId(entity.getAcademicYearId())) {
                throw new ResourceNotFoundException("org.academic_year", entity.getAcademicYearId());
            }
            if (!scope.canRead(institutionOfAcademicTerm(entity))) {
                throw new ResourceNotFoundException("org.academic_year", entity.getAcademicYearId());
            }
            assertValidAcademicTerm(entity, true);
            scope.requireWritable(institutionOfAcademicTerm(entity));
            assertUniqueAcademicTerm(entity);
            if (validateOnly) {
                return academicTermMapper.toDto(entity);
            }
            AcademicTermEntity stored = academicTermRepository.insert(entity);
            audit.recordCreate(ACADEMIC_TERM_AGGREGATE, stored.getId());
            outbox.publish(ACADEMIC_TERM_AGGREGATE + OrgEvents.CREATED, ACADEMIC_TERM_AGGREGATE, stored.getId(),
                    OrgEvents.payload(stored.getId()));
            return academicTermMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AcademicTerm updateAcademicTerm(UUID academicYearId, UUID id, String ifMatch, AcademicTermUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ACADEMIC_TERM_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ACADEMIC_TERM_PERMISSION_UPDATE);
            AcademicTermEntity entity = loadVisibleAcademicTerm(scope, academicYearId, id);
            scope.requireWritable(institutionOfAcademicTerm(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAcademicTerm(entity, changes, fields);
            validator.validate(academicTermMapper.toDto(entity));
            assertValidAcademicTerm(entity, false);
            scope.requireWritable(institutionOfAcademicTerm(entity));
            assertUniqueAcademicTerm(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return academicTermMapper.toDto(entity);
            }
            if (academicTermRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ACADEMIC_TERM_AGGREGATE, id, fields);
            outbox.publish(ACADEMIC_TERM_AGGREGATE + OrgEvents.UPDATED, ACADEMIC_TERM_AGGREGATE, id, OrgEvents.payload(id));
            return academicTermMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAcademicTerm(UUID academicYearId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ACADEMIC_TERM_PERMISSION_DELETE);
            AcademicTermEntity entity = loadVisibleAcademicTerm(scope, academicYearId, id);
            scope.requireWritable(institutionOfAcademicTerm(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (academicTermRepository.referencedByAcademicTermParentTermId(id)) {
                throw new InvalidStateException(
                        "The academic term is still referenced by org.academic_term rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (academicTermRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ACADEMIC_TERM_AGGREGATE, id);
            outbox.publish(ACADEMIC_TERM_AGGREGATE + OrgEvents.DELETED, ACADEMIC_TERM_AGGREGATE, id, OrgEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AcademicTermEntity loadVisibleAcademicTerm(DataScope scope, UUID academicYearId, UUID id) {
        AcademicTermEntity entity = academicTermRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ACADEMIC_TERM_AGGREGATE, id));
        if (!scope.canRead(institutionOfAcademicTerm(entity)) || !academicYearId.equals(entity.getAcademicYearId())) {
            throw new ResourceNotFoundException(ACADEMIC_TERM_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the org.academic_year it belongs to). */
    private Optional<UUID> institutionOfAcademicTerm(AcademicTermEntity entity) {
        return academicTermRepository.institutionOf(entity.getAcademicYearId());
    }

    /**
     * Applies the selected AcademicTermUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyAcademicTerm(AcademicTermEntity entity, AcademicTermUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "parentTermId" -> entity.setParentTermId(changes.getParentTermId());
                case "name" -> entity.setName(changes.getName());
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                case "sequence" -> entity.setSequence(changes.getSequence());
                case "code" -> entity.setCode(changes.getCode());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAcademicTerm(AcademicTermEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getStartDate() == null) {
            errors.add(new FieldError("startDate", "must not be null", "NotNull"));
        }
        if (entity.getEndDate() == null) {
            errors.add(new FieldError("endDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() != null && !academicTermRepository.existsAcademicYearId(entity.getAcademicYearId())) {
            errors.add(new FieldError("academicYearId", "academic year does not exist", "NOT_FOUND"));
        }
        if (entity.getParentTermId() != null && !academicTermRepository.existsParentTermId(entity.getParentTermId())) {
            errors.add(new FieldError("parentTermId", "academic term does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAcademicTerm(AcademicTermEntity entity) {
        if (academicTermRepository.takenAcademicYearIdParentTermIdNameName(entity)) {
            throw new DuplicateResourceException(
                    "A academic term with the same academic year id and parent term id and name already exists.");
        }
        if (academicTermRepository.takenAcademicYearIdParentTermIdStartDateStart(entity)) {
            throw new DuplicateResourceException(
                    "A academic term with the same academic year id and parent term id and start date already exists.");
        }
    }
}
