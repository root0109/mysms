package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.LiveClass;
import com.mysms.lms.api.model.LiveClassParticipant;
import com.mysms.lms.api.model.LiveClassUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.LiveClassEntity;
import com.mysms.lms.domain.LiveClassParticipantEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.LiveClassMapper;
import com.mysms.lms.mapper.LiveClassParticipantMapper;
import com.mysms.lms.repository.LiveClassParticipantRepository;
import com.mysms.lms.repository.LiveClassRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.live_class (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a LiveClass
 * are served here too — LiveClassParticipant.
 */
@Service
public class LiveClassService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = LiveClassEntity.TABLE;
    static final String PERMISSION_LIST = "lms.live_class.read";
    static final String PERMISSION_GET = "lms.live_class.read";
    static final String PERMISSION_CREATE = "lms.live_class.create";
    static final String PERMISSION_UPDATE = "lms.live_class.update";
    static final String PERMISSION_DELETE = "lms.live_class.delete";
    /** The contract's LiveClassUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("cohortId", "title", "description", "classDate", "startTime", "durationMinutes", "timezone",
                    "hostUserId",
            "conferencingProvider", "conferencingAccountId", "meetingId", "meetingUuid", "joinUrl", "meetingPassword",
            "autoRecording", "calendarEventId", "attendeeCount", "inviteStatus", "inviteAttempts", "moderatorPassword",
            "welcomeMessage", "providerCreatedRef", "recordingUrl", "inviteeCount", "prepMinutes", "extensionMinutes",
            "showInCalendar", "hostJoined", "isActive", "endedAt");

    public static final String LIVE_CLASS_PARTICIPANT_AGGREGATE = LiveClassParticipantEntity.TABLE;
    static final String LIVE_CLASS_PARTICIPANT_PERMISSION_LIST = "lms.live_class_participant.read";
    static final String LIVE_CLASS_PARTICIPANT_PERMISSION_GET = "lms.live_class_participant.read";

    private static final String LIVE_CLASS_PARTICIPANT_SECRET_JOIN_URL_ENC = "lms.live_class_participant.join_url_enc";

    private final LiveClassRepository repository;
    private final LiveClassMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("meetingPassword", "moderatorPassword");
    private static final String SECRET_MEETING_PASSWORD_ENC = "lms.live_class.meeting_password_enc";
    private static final String SECRET_MODERATOR_PASSWORD_ENC = "lms.live_class.moderator_password_enc";
    private final FieldCipher cipher;
    private final LiveClassParticipantRepository liveClassParticipantRepository;
    private final LiveClassParticipantMapper liveClassParticipantMapper;

    public LiveClassService(LiveClassRepository repository,
            LiveClassMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            FieldCipher cipher,
            LiveClassParticipantRepository liveClassParticipantRepository,
            LiveClassParticipantMapper liveClassParticipantMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.cipher = cipher;
        this.liveClassParticipantRepository = liveClassParticipantRepository;
        this.liveClassParticipantMapper = liveClassParticipantMapper;
    }

    /** Refuses to start without a master key: this resource stores secret columns (DEPLOYMENT.md "Secrets"). */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public LiveClass getLiveClass(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LiveClass> listLiveClasss(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LiveClassRepository.SORT_FIELDS, LiveClassRepository.DEFAULT_ORDER,
                LiveClassRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LiveClassRepository.FILTER_FIELDS)
                .and(LiveClassRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LiveClassEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LiveClass createLiveClass(LiveClass request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        LiveClassEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealMeetingPassword(entity, request.getMeetingPassword());
        sealModeratorPassword(entity, request.getModeratorPassword());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            LiveClassEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LiveClass updateLiveClass(UUID id, String ifMatch, LiveClassUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LiveClassEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validateExcept(dto(entity), SECRETS);
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return dto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return dto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLiveClass(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LiveClassEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByContentItemLiveClassId(id)) {
                throw new InvalidStateException(
                        "The live class is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByLiveClassParticipantLiveClassId(id)) {
                throw new InvalidStateException(
                        "The live class is still referenced by lms.live_class_participant rows; remove those first.");
            }
            if (repository.referencedByTimetableEntryLiveClassId(id)) {
                throw new InvalidStateException(
                        "The live class is still referenced by lms.timetable_entry rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /**
     * Stores meetingPassword encrypted for the row's tenant and column (database/README "Secrets"); never the
     * plaintext.
     */
    private void sealMeetingPassword(LiveClassEntity entity, String plaintext) {
        entity.setMeetingPasswordEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_MEETING_PASSWORD_ENC));
    }

    /**
     * Stores moderatorPassword encrypted for the row's tenant and column (database/README "Secrets"); never the
     * plaintext.
     */
    private void sealModeratorPassword(LiveClassEntity entity, String plaintext) {
        entity.setModeratorPasswordEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_MODERATOR_PASSWORD_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private LiveClass dto(LiveClassEntity entity) {
        LiveClass dto = mapper.toDto(entity);
        dto.setMeetingPasswordMasked(
                cipher.maskStored(entity.getMeetingPasswordEnc(), entity.getTenantId(), SECRET_MEETING_PASSWORD_ENC));
        dto.setModeratorPasswordMasked(
                cipher.maskStored(entity.getModeratorPasswordEnc(), entity.getTenantId(),
                        SECRET_MODERATOR_PASSWORD_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LiveClassEntity loadVisible(DataScope scope, UUID id) {
        LiveClassEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOf(LiveClassEntity entity) {
        return repository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected LiveClassUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private void apply(LiveClassEntity entity, LiveClassUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "classDate" -> entity.setClassDate(changes.getClassDate());
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "durationMinutes" -> entity.setDurationMinutes(changes.getDurationMinutes());
                case "timezone" -> entity.setTimezone(changes.getTimezone());
                case "hostUserId" -> entity.setHostUserId(changes.getHostUserId());
                case "conferencingProvider" -> entity.setConferencingProvider(
                        Optional.ofNullable(changes.getConferencingProvider()).map(v -> v.getValue()).orElse(null));
                case "conferencingAccountId" -> entity.setConferencingAccountId(changes.getConferencingAccountId());
                case "meetingId" -> entity.setMeetingId(changes.getMeetingId());
                case "meetingUuid" -> entity.setMeetingUuid(changes.getMeetingUuid());
                case "joinUrl" -> entity.setJoinUrl(changes.getJoinUrl());
                case "meetingPassword" -> sealMeetingPassword(entity, changes.getMeetingPassword());
                case "autoRecording" -> entity.setAutoRecording(Optional.ofNullable(changes.getAutoRecording())
                        .map(v -> v.getValue()).orElse(null));
                case "calendarEventId" -> entity.setCalendarEventId(changes.getCalendarEventId());
                case "attendeeCount" -> entity.setAttendeeCount(changes.getAttendeeCount());
                case "inviteStatus" -> entity.setInviteStatus(Optional.ofNullable(changes.getInviteStatus())
                        .map(v -> v.getValue()).orElse(null));
                case "inviteAttempts" -> entity.setInviteAttempts(changes.getInviteAttempts());
                case "moderatorPassword" -> sealModeratorPassword(entity, changes.getModeratorPassword());
                case "welcomeMessage" -> entity.setWelcomeMessage(changes.getWelcomeMessage());
                case "providerCreatedRef" -> entity.setProviderCreatedRef(changes.getProviderCreatedRef());
                case "recordingUrl" -> entity.setRecordingUrl(changes.getRecordingUrl());
                case "inviteeCount" -> entity.setInviteeCount(changes.getInviteeCount());
                case "prepMinutes" -> entity.setPrepMinutes(changes.getPrepMinutes());
                case "extensionMinutes" -> entity.setExtensionMinutes(changes.getExtensionMinutes());
                case "showInCalendar" -> entity.setShowInCalendar(changes.getShowInCalendar());
                case "hostJoined" -> entity.setHostJoined(changes.getHostJoined());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "endedAt" -> entity.setEndedAt(changes.getEndedAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LiveClassEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getClassDate() == null) {
            errors.add(new FieldError("classDate", "must not be null", "NotNull"));
        }
        if (entity.getStartTime() == null) {
            errors.add(new FieldError("startTime", "must not be null", "NotNull"));
        }
        if (entity.getDurationMinutes() == null) {
            errors.add(new FieldError("durationMinutes", "must not be null", "NotNull"));
        }
        if (entity.getTimezone() == null) {
            errors.add(new FieldError("timezone", "must not be null", "NotNull"));
        }
        if (entity.getHostUserId() == null) {
            errors.add(new FieldError("hostUserId", "must not be null", "NotNull"));
        }
        if (entity.getConferencingProvider() == null) {
            errors.add(new FieldError("conferencingProvider", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAutoRecording() == null) {
            errors.add(new FieldError("autoRecording", "must not be null", "NotNull"));
        }
        if (!creating && entity.getInviteStatus() == null) {
            errors.add(new FieldError("inviteStatus", "must not be null", "NotNull"));
        }
        if (!creating && entity.getInviteAttempts() == null) {
            errors.add(new FieldError("inviteAttempts", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowInCalendar() == null) {
            errors.add(new FieldError("showInCalendar", "must not be null", "NotNull"));
        }
        if (!creating && entity.getHostJoined() == null) {
            errors.add(new FieldError("hostJoined", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getConferencingAccountId() != null
                && !repository.existsConferencingAccountId(entity.getConferencingAccountId())) {
            errors.add(new FieldError("conferencingAccountId", "conferencing account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LiveClassEntity entity) {

    }

    // ---- LiveClassParticipant: a record of the LiveClass aggregate ----

    public LiveClassParticipant getLiveClassParticipant(UUID liveClassId, UUID id) {
        return doInReadTransaction(() -> {
            return dtoLiveClassParticipant(loadVisibleLiveClassParticipant(DataScope.current(
                    LIVE_CLASS_PARTICIPANT_PERMISSION_GET), liveClassId, id));
        });
    }

    public Page<LiveClassParticipant> listLiveClassParticipants(UUID liveClassId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LiveClassParticipantRepository.SORT_FIELDS,
                        LiveClassParticipantRepository.DEFAULT_ORDER,
                LiveClassParticipantRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LiveClassParticipantRepository.FILTER_FIELDS)
                .and(LiveClassParticipantRepository.ofParent(liveClassId))
                        .and(LiveClassParticipantRepository.scope(DataScope.current(LIVE_CLASS_PARTICIPANT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + liveClassId, clock);
        return doInReadTransaction(() -> {
            Page<LiveClassParticipantEntity> rows = liveClassParticipantRepository.list(page, where);
            return rows.map(this::dtoLiveClassParticipant);
        });
    }

    /** Stores joinUrl encrypted for the row's tenant and column (database/README "Secrets"); never the plaintext. */
    private void sealJoinUrlLiveClassParticipant(LiveClassParticipantEntity entity, String plaintext) {
        entity.setJoinUrlEnc(cipher.encrypt(plaintext, entity.getTenantId(), LIVE_CLASS_PARTICIPANT_SECRET_JOIN_URL_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private LiveClassParticipant dtoLiveClassParticipant(LiveClassParticipantEntity entity) {
        LiveClassParticipant dto = liveClassParticipantMapper.toDto(entity);
        dto.setJoinUrlMasked(cipher.maskStored(entity.getJoinUrlEnc(), entity.getTenantId(),
                LIVE_CLASS_PARTICIPANT_SECRET_JOIN_URL_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LiveClassParticipantEntity loadVisibleLiveClassParticipant(DataScope scope, UUID liveClassId, UUID id) {
        LiveClassParticipantEntity entity = liveClassParticipantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LIVE_CLASS_PARTICIPANT_AGGREGATE, id));
        if (!scope.canRead(institutionOfLiveClassParticipant(entity)) || !liveClassId.equals(entity.getLiveClassId())) {
            throw new ResourceNotFoundException(LIVE_CLASS_PARTICIPANT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.live_class → lms.cohort it belongs to). */
    private Optional<UUID> institutionOfLiveClassParticipant(LiveClassParticipantEntity entity) {
        return liveClassParticipantRepository.institutionOf(entity.getLiveClassId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidLiveClassParticipant(LiveClassParticipantEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getLiveClassId() == null) {
            errors.add(new FieldError("liveClassId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getJoinedAt() == null) {
            errors.add(new FieldError("joinedAt", "must not be null", "NotNull"));
        }
        if (entity.getLiveClassId() != null && !liveClassParticipantRepository.existsLiveClassId(entity.getLiveClassId())) {
            errors.add(new FieldError("liveClassId", "live class does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueLiveClassParticipant(LiveClassParticipantEntity entity) {

    }
}
