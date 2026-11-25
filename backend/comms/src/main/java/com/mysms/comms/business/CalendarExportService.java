package com.mysms.comms.business;

import java.util.Map;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.core.id.UuidV7;
import com.mysms.core.context.CallContextHolder;
import com.mysms.comms.repository.OperationRepository;
import com.mysms.comms.mapper.OperationMapper;
import com.mysms.comms.domain.OperationEntity;
import com.mysms.comms.api.model.Operation;
import com.mysms.comms.api.model.ExportCalendarRequest;
import com.mysms.comms.api.model.CalendarItem;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.CalendarExportEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.CalendarExportMapper;
import com.mysms.comms.repository.CalendarExportRepository;
import com.mysms.comms.api.model.CalendarExport;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.calendar_export: audit and domain event commit in the same transaction as every change.
 * A person's exports are theirs alone: every read and change reaches only the caller's own rows, whatever the
 * caller's grants.
 */
@Service
public class CalendarExportService extends AppTransactionManager {

    public static final String AGGREGATE = CalendarExportEntity.TABLE;
    static final String PERMISSION_LIST = "comms.calendar_export.read";
    static final String PERMISSION_GET = "comms.calendar_export.read";
    static final String PERMISSION_DELETE = "comms.calendar_export.delete";
    static final String PERMISSION_EXPORT_CALENDAR = "comms.calendar_export.export_calendar";
    static final String OPERATION_EXPORT = "comms.ExportCalendar";
    /** The provider property a row of an export keeps: the item's iCalendar UID. */
    static final String UID_PROPERTY = "ics_uid";


    private final CalendarExportRepository repository;
    private final CalendarExportMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final MyCalendarService calendar;
    private final PlatformFiles files;
    private final OperationRepository operations;
    private final OperationMapper operationMapper;

    public CalendarExportService(CalendarExportRepository repository, CalendarExportMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, MyCalendarService calendar, PlatformFiles files,
            OperationRepository operations, OperationMapper operationMapper) {

        super(database, transactions);
        this.calendar = calendar;
        this.files = files;
        this.operations = operations;
        this.operationMapper = operationMapper;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public CalendarExport getCalendarExport(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<CalendarExport> listCalendarExports(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CalendarExportRepository.SORT_FIELDS, CalendarExportRepository.DEFAULT_ORDER,
                CalendarExportRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CalendarExportRepository.FILTER_FIELDS)
                .and(CalendarExportRepository.ownedBy(OwnRows.caller()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + OwnRows.caller(), clock);
        return doInReadTransaction(() -> {
            Page<CalendarExportEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCalendarExport(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CalendarExportEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /**
     * comms.ExportCalendar: the caller's own calendar between two instants (at most 366 days apart) as an iCalendar
     * file in the caller's file store, which the chosen calendar (Google, Microsoft) imports; every exported item is
     * recorded against that provider so a later export updates it in place. Nobody else's personal events are ever
     * part of it. Answers a finished operation whose result carries the file and its download link.
     */
    public Operation exportCalendar(ExportCalendarRequest request) {
        validator.validate(request);
        UUID caller = OwnRows.caller();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        List<CalendarItem> items = calendar.items(request.getFrom(), request.getTo(), PERMISSION_EXPORT_CALENDAR);
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        UUID fileId = files.write("calendar-" + now.toLocalDate() + ".ics", ICalendar.MIME_TYPE,
                ICalendar.write(items, now));
        PlatformFiles.Link link = files.link(fileId);
        String provider = request.getProvider().getValue();
        return doInWriteTransaction(() -> {
            UUID actor = Actor.current().orElse(null);
            for (CalendarItem item : items) {
                String kind = item.getItemType() == CalendarItem.ItemTypeEnum.OCCURRENCE ? "occurrence" : "event";
                UUID row = repository.exported(UuidV7.next(clock), caller, provider, item.getCohortId(), kind,
                        item.getId(), UID_PROPERTY, ICalendar.uid(item), now, actor);
                audit.recordUpdate(AGGREGATE, row, List.of("lastExportedAt"));
            }
            OperationEntity operation = OperationEntity.builder().operationName(OPERATION_EXPORT)
                    .resourceType(AGGREGATE).resultEntityType("file").resultEntityId(fileId).startedByUserId(caller)
                    .tenantId(tenant).build();
            operation.onCreatingData(Actor.current(), clock);
            operation.setStartedAt(operation.getCreatedAt());
            operation.setFinishedAt(operation.getCreatedAt());
            Operation done = operationMapper.toDto(operations.insertCompleted(operation));
            done.setResult(Map.of("fileId", fileId.toString(), "url", link.url(),
                    "expiresAt", link.expiresAt().toString(), "items", items.size()));
            audit.recordCreate("comms.operation", done.getOperationId());
            return done;
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CalendarExportEntity loadVisible(DataScope scope, UUID id) {
        CalendarExportEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        OwnRows.require(entity.getUserId(), AGGREGATE, id);
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(CalendarExportEntity entity) {
        return Optional.empty();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CalendarExportEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getProvider() == null) {
            errors.add(new FieldError("provider", "must not be null", "NotNull"));
        }
        if (entity.getEntityType() == null) {
            errors.add(new FieldError("entityType", "must not be null", "NotNull"));
        }
        if (entity.getEntityId() == null) {
            errors.add(new FieldError("entityId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CalendarExportEntity entity) {
        if (entity.getPropertyName() != null && repository.takenUserIdProviderEntityTypeEntityIdPropertyName1(entity)) {
            throw new DuplicateResourceException(
                    "A calendar export with the same user id and provider and entity type and entity id and property "
                            + "name already exists.");
        }
    }
}
