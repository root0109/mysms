package com.mysms.sis.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.sis.api.model.DisciplineIncident;
import com.mysms.sis.api.model.DisciplineIncidentUpdate;
import com.mysms.sis.api.model.MarkIncidentActionTakenRequest;
import com.mysms.sis.api.model.MarkIncidentSeenRequest;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
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
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.DisciplineIncidentEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.DisciplineIncidentMapper;
import com.mysms.sis.repository.DisciplineIncidentRepository;
import com.mysms.sis.repository.FamilyLinkRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.discipline_incident: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class DisciplineIncidentService extends AppTransactionManager {

    public static final String AGGREGATE = DisciplineIncidentEntity.TABLE;
    static final String PERMISSION_LIST = "sis.discipline_incident.read";
    static final String PERMISSION_GET = "sis.discipline_incident.read";
    static final String PERMISSION_CREATE = "sis.discipline_incident.create";
    static final String PERMISSION_UPDATE = "sis.discipline_incident.update";
    static final String PERMISSION_DELETE = "sis.discipline_incident.delete";
    /** The contract's DisciplineIncidentUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "classSectionId", "studentId", "incidentAt", "reportedByUserId",
            "incidentType", "details", "response", "actionTaken", "evidenceMediaType", "evidenceFileId",
            "notifyGuardians", "seenByGuardian");
    static final String PERMISSION_MARK_ACTION_TAKEN = "sis.discipline_incident.mark_incident_action_taken";
    static final String PERMISSION_MARK_SEEN = "sis.discipline_incident.mark_incident_seen";

    private final DisciplineIncidentRepository repository;
    private final DisciplineIncidentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final FamilyLinkRepository families;

    public DisciplineIncidentService(DisciplineIncidentRepository repository, DisciplineIncidentMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, FamilyLinkRepository families) {

        super(database, transactions);
        this.families = families;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public DisciplineIncident getDisciplineIncident(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<DisciplineIncident> listDisciplineIncidents(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, DisciplineIncidentRepository.SORT_FIELDS,
                        DisciplineIncidentRepository.DEFAULT_ORDER,
                DisciplineIncidentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, DisciplineIncidentRepository.FILTER_FIELDS)
                .and(DisciplineIncidentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<DisciplineIncidentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public DisciplineIncident createDisciplineIncident(DisciplineIncident request, boolean validateOnly) {
        validator.validate(request);
        DisciplineIncidentEntity entity = mapper.toEntity(request);
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
            DisciplineIncidentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public DisciplineIncident updateDisciplineIncident(UUID id, String ifMatch, DisciplineIncidentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            DisciplineIncidentEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteDisciplineIncident(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            DisciplineIncidentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /**
     * sis.MarkIncidentActionTaken: the school records that it acted on the incident, with what it did (the response;
     * when the request carries none, the stored one stays). The row must be visible and writable for the caller, and
     * the step is allowed while no action is recorded; after that the answer is 409 and nothing changes.
     */
    public DisciplineIncident markIncidentActionTaken(UUID id, MarkIncidentActionTakenRequest request) {
        DataScope scope = DataScope.current(PERMISSION_MARK_ACTION_TAKEN);
        String given = request == null ? null : request.getResponse();
        String response = given == null || given.isBlank() ? null : given.strip();
        return doInWriteTransaction(() -> {
            DisciplineIncidentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            requireCurrent(entity, request == null ? null : request.getEtag());
            if (Boolean.TRUE.equals(entity.getActionTaken())) {
                throw new InvalidStateException("Action has already been recorded for the incident.");
            }
            if (repository.markIncidentActionTaken(id, entity.getRowVersion(), response, now(),
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "markActionTaken",
                    response == null ? List.of("actionTaken") : List.of("actionTaken", "response"), null);
            outbox.publish(AGGREGATE + ".markActionTaken", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * sis.MarkIncidentSeen: a guardian acknowledges an incident the school shared with guardians. A guardian of the
     * incident's student (an active link of the caller's account or person) may do it for their own child, wherever
     * the child goes to school; anyone else records the acknowledgement for the guardian (a signed slip, say) and
     * needs, besides the operation's permission, to be able to read and write incidents of the institution. To
     * everyone else the incident does not exist (404) — and to a guardian, an incident the school did not share does
     * not exist either. An incident already seen, or (for staff) one not shared with guardians, answers 409.
     */
    public DisciplineIncident markIncidentSeen(UUID id, MarkIncidentSeenRequest request) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_MARK_SEEN);
        return doInWriteTransaction(() -> {
            DisciplineIncidentEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            boolean shared = Boolean.TRUE.equals(entity.getNotifyGuardians());
            if (families.isGuardianOf(entity.getStudentId(), caller.userId(), caller.personId())) {
                if (!shared) {
                    throw new ResourceNotFoundException(AGGREGATE, id);
                }
            } else {
                if (!caller.hasPermission(PERMISSION_GET) || !scope.canRead(institutionOf(entity))) {
                    throw new ResourceNotFoundException(AGGREGATE, id);
                }
                scope.requireWritable(institutionOf(entity));
            }
            requireCurrent(entity, request == null ? null : request.getEtag());
            if (!shared) {
                throw new InvalidStateException("The incident is not shared with guardians.");
            }
            if (Boolean.TRUE.equals(entity.getSeenByGuardian())) {
                throw new InvalidStateException("The incident has already been acknowledged.");
            }
            if (repository.markIncidentSeen(id, entity.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "markSeen", List.of("seenByGuardian"), null);
            outbox.publish(AGGREGATE + ".markSeen", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** A body etag, when the request carries one, must name the row's current version (412 otherwise). */
    private static void requireCurrent(DisciplineIncidentEntity entity, String etag) {
        OptionalInt expected = Etags.bodyVersion(etag);
        if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private DisciplineIncidentEntity loadVisible(DataScope scope, UUID id) {
        DisciplineIncidentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(DisciplineIncidentEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected DisciplineIncidentUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(DisciplineIncidentEntity entity, DisciplineIncidentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "incidentAt" -> entity.setIncidentAt(changes.getIncidentAt());
                case "reportedByUserId" -> entity.setReportedByUserId(changes.getReportedByUserId());
                case "incidentType" -> entity.setIncidentType(Optional.ofNullable(changes.getIncidentType())
                        .map(v -> v.getValue()).orElse(null));
                case "details" -> entity.setDetails(changes.getDetails());
                case "response" -> entity.setResponse(changes.getResponse());
                case "actionTaken" -> entity.setActionTaken(changes.getActionTaken());
                case "evidenceMediaType" -> entity.setEvidenceMediaType(
                        Optional.ofNullable(changes.getEvidenceMediaType()).map(v -> v.getValue()).orElse(null));
                case "evidenceFileId" -> entity.setEvidenceFileId(changes.getEvidenceFileId());
                case "notifyGuardians" -> entity.setNotifyGuardians(changes.getNotifyGuardians());
                case "seenByGuardian" -> entity.setSeenByGuardian(changes.getSeenByGuardian());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(DisciplineIncidentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getIncidentAt() == null) {
            errors.add(new FieldError("incidentAt", "must not be null", "NotNull"));
        }
        if (entity.getReportedByUserId() == null) {
            errors.add(new FieldError("reportedByUserId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIncidentType() == null) {
            errors.add(new FieldError("incidentType", "must not be null", "NotNull"));
        }
        if (entity.getDetails() == null) {
            errors.add(new FieldError("details", "must not be null", "NotNull"));
        }
        if (!creating && entity.getActionTaken() == null) {
            errors.add(new FieldError("actionTaken", "must not be null", "NotNull"));
        }
        if (!creating && entity.getNotifyGuardians() == null) {
            errors.add(new FieldError("notifyGuardians", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSeenByGuardian() == null) {
            errors.add(new FieldError("seenByGuardian", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() != null && !repository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(DisciplineIncidentEntity entity) {

    }
}
