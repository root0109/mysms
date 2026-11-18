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
import com.mysms.authclient.AuthContext;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.id.UuidV7;
import com.mysms.lms.api.model.ExternalLink;
import com.mysms.lms.api.model.LiveClassParticipantList;
import com.mysms.lms.api.model.ScheduleLiveClassRequest;
import com.mysms.lms.domain.CohortEntity;
import com.mysms.lms.domain.ConferencingAccountEntity;
import com.mysms.lms.repository.CohortRepository;
import com.mysms.lms.repository.ConferencingAccountRepository;
import com.mysms.lms.repository.LiveClassMeetingRepository;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

/**
 * Business service of lms.live_class and of the records that exist only as part of a LiveClass (LiveClassParticipant):
 * audit and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant.
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
    private final CohortRepository cohorts;
    private final CohortAccess access;
    private final ConferencingAccountRepository conferencingAccounts;
    private final MeetingProviders providers;
    private final LiveClassMeetingRepository meetings;
    private final MemberDirectory directory;
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
            LiveClassParticipantMapper liveClassParticipantMapper,
            CohortRepository cohorts,
            CohortAccess access,
            ConferencingAccountRepository conferencingAccounts,
            MeetingProviders providers,
            LiveClassMeetingRepository meetings,
            MemberDirectory directory) {
        super(database, transactions);
        this.cohorts = cohorts;
        this.access = access;
        this.conferencingAccounts = conferencingAccounts;
        this.providers = providers;
        this.meetings = meetings;
        this.directory = directory;
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

    /** Refuses to start without a master key: this resource stores secret columns. */
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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
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

    /** Stores meetingPassword encrypted for the row's tenant and column; never the plaintext. */
    private void sealMeetingPassword(LiveClassEntity entity, String plaintext) {
        entity.setMeetingPasswordEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_MEETING_PASSWORD_ENC));
    }

    /** Stores moderatorPassword encrypted for the row's tenant and column; never the plaintext. */
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOf(LiveClassEntity entity) {
        return repository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected LiveClassUpdate fields; explicit per field so a mask can clear values. */
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

    /** Stores joinUrl encrypted for the row's tenant and column; never the plaintext. */
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

    // ---- the live class's meeting at its provider ----

    static final String PERMISSION_SCHEDULE = "lms.live_class.schedule_live_class";
    static final String PERMISSION_START_URL = "lms.live_class.get_live_class_start_url";
    static final String PERMISSION_JOIN = "lms.live_class.join_live_class";
    static final String PERMISSION_END = "lms.live_class.end_live_class";
    static final String PERMISSION_SYNC = "lms.live_class.sync_live_class_attendance";
    static final String PERMISSION_RETRY_INVITES = "lms.live_class.retry_live_class_invites";
    private static final String SECRET_CLIENT_SECRET = "lms.conferencing_account.client_secret_enc";
    private static final String PARTICIPANT = "lms.live_class_participant";

    /** What a meeting call needs, read before the provider is called. */
    private record MeetingContext(LiveClassEntity liveClass, MeetingProvider provider, MeetingProvider.Account account) {
    }

    /**
     * Creates the meeting at the account's provider and the live class of the cohort, hosted by the caller. The cohort
     * must be in scope (404), writable (403) and not archived (409); the account must exist and be enabled (422), and
     * its provider configured (503 CONFERENCING_NOT_CONFIGURED). Calendar invites stay pending.
     */
    public LiveClass scheduleLiveClass(ScheduleLiveClassRequest request) {
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        ZoneId zone = zone(request.getTimezone());
        ScheduleLiveClassRequest.AutoRecordingEnum auto = request.getAutoRecording();
        String recording = auto == null ? null : auto.getValue();
        DataScope scope = DataScope.current(PERMISSION_SCHEDULE);
        MeetingContext context = doInReadTransaction(() -> {
            CohortEntity cohort = writableCohort(scope, request.getCohortId());
            ConferencingAccountEntity account = conferencingAccounts.findById(request.getConferencingAccountId())
                    .filter(found -> Boolean.TRUE.equals(found.getIsEnabled()))
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("conferencingAccountId",
                            "conferencing account does not exist or is disabled", "NOT_FOUND"))));
            LiveClassEntity draft = LiveClassEntity.builder().cohortId(cohort.getId()).title(request.getTitle())
                    .description(request.getDescription()).classDate(request.getClassDate())
                    .startTime(LocalTime.parse(request.getStartTime())).durationMinutes(request.getDurationMinutes())
                    .timezone(request.getTimezone()).conferencingProvider(account.getProvider())
                    .conferencingAccountId(account.getId()).autoRecording(recording).build();
            return new MeetingContext(draft, providers.of(account.getProvider()), account(account));
        });
        LiveClassEntity draft = context.liveClass();
        MeetingProvider.Meeting meeting = context.provider().create(context.account(),
                new MeetingProvider.MeetingRequest(draft.getTitle(), draft.getDescription(),
                        draft.getClassDate().atTime(draft.getStartTime()).atZone(zone).toOffsetDateTime(),
                        draft.getTimezone(), draft.getDurationMinutes(), draft.getAutoRecording()));
        return doInWriteTransaction(() -> {
            writableCohort(scope, draft.getCohortId());
            draft.setHostUserId(AuthContext.require().userId());
            draft.setMeetingId(meeting.meetingId());
            draft.setMeetingUuid(meeting.meetingUuid());
            draft.setJoinUrl(meeting.joinUrl());
            draft.setProviderCreatedRef(meeting.meetingId());
            draft.onCreatingData(Actor.current(), clock);
            draft.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
            if (meeting.password() != null && !meeting.password().isBlank()) {
                cipher.requireConfigured();
                draft.setMeetingPasswordEnc(cipher.encrypt(meeting.password(), draft.getTenantId(),
                        SECRET_MEETING_PASSWORD_ENC));
            }
            LiveClassEntity stored = repository.insert(draft);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + ".schedule", AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /** A fresh host start link, for the host or the cohort's teaching staff; an ended class is 409. */
    public ExternalLink getLiveClassStartUrl(UUID id) {
        MeetingContext context = doInReadTransaction(() -> hosted(id, PERMISSION_START_URL));
        MeetingProvider.StartLink link = context.provider().startLink(context.account(),
                context.liveClass().getMeetingId());
        ExternalLink answer = new ExternalLink(link.url());
        answer.setExpiresAt(link.expiresAt().atOffset(ZoneOffset.UTC));
        return answer;
    }

    /**
     * The join link of a live class for its cohort's students and teaching staff (403 for anybody else who sees it),
     * recording the first request to join; an ended class is 409.
     */
    public ExternalLink joinLiveClass(UUID id) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_JOIN);
        return doInWriteTransaction(() -> {
            LiveClassEntity liveClass = loadVisible(scope, id);
            CohortEntity cohort = access.visible(liveClass.getCohortId(), PERMISSION_JOIN);
            boolean staff = access.isTeachingStaff(cohort) || caller.userId().equals(liveClass.getHostUserId());
            if (!staff && !access.isStudent(cohort)) {
                throw new PermissionDeniedException("Only the cohort's members may join its live classes.");
            }
            if (liveClass.getEndedAt() != null || liveClass.getJoinUrl() == null) {
                throw new InvalidStateException("The live class has ended or has no meeting.");
            }
            UUID participant = UuidV7.next(clock);
            if (meetings.recordJoin(participant, id, caller.userId(), staff, now()) == 1) {
                audit.recordCreate(PARTICIPANT, participant);
            }
            return new ExternalLink(liveClass.getJoinUrl());
        });
    }

    /** Ends the meeting for everyone at the provider, then the class; the host or teaching staff; ended twice is 409. */
    public LiveClass endLiveClass(UUID id) {
        MeetingContext context = doInReadTransaction(() -> hosted(id, PERMISSION_END));
        context.provider().end(context.account(), context.liveClass().getMeetingId());
        return doInWriteTransaction(() -> {
            LiveClassEntity current = hosted(id, PERMISSION_END).liveClass();
            if (meetings.end(id, current.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The live class changed meanwhile; try again.");
            }
            audit.recordTransition(AGGREGATE, id, "end", List.of("endedAt", "isActive"), null);
            outbox.publish(AGGREGATE + ".end", AGGREGATE, id, LmsEvents.payload(id));
            return dto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * Reads who attended from the provider and records it on the class's participants (matched to accounts by
     * e-mail through iam); the host or teaching staff, after the class has ended (409 before).
     */
    public LiveClassParticipantList syncLiveClassAttendance(UUID id) {
        MeetingContext context = doInReadTransaction(() -> {
            if (accessible(id, PERMISSION_SYNC, true).getEndedAt() == null) {
                throw new InvalidStateException("Attendance is read once the live class has ended.");
            }
            return hosted(id, PERMISSION_SYNC, true);
        });
        List<MeetingProvider.Attendance> attendance = context.provider().attendance(context.account(),
                context.liveClass().getMeetingUuid());
        Map<String, UUID> accounts = directory.byEmail(attendance.stream().map(MeetingProvider.Attendance::email)
                .toList(), "Reading live class attendance");
        return doInWriteTransaction(() -> {
            hosted(id, PERMISSION_SYNC, true);
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            for (MeetingProvider.Attendance attended : attendance) {
                UUID user = attended.email() == null ? null : accounts.get(attended.email().toLowerCase(Locale.ROOT));
                if (user != null) {
                    meetings.attended(UuidV7.next(clock), id, user, attended.joinedAt(), attended.leftAt(),
                            attended.durationMinutes(), now, actor);
                }
            }
            meetings.countAttendees(id, now, actor);
            audit.recordTransition(AGGREGATE, id, "sync_attendance", List.of("attendeeCount"), null);
            outbox.publish(AGGREGATE + ".sync_attendance", AGGREGATE, id, LmsEvents.payload(id));
            SortSpec sort = SortSpec.parse(null, LiveClassParticipantRepository.SORT_FIELDS,
                    LiveClassParticipantRepository.DEFAULT_ORDER, LiveClassParticipantRepository.ID_COLUMN);
            return new LiveClassParticipantList(liveClassParticipantRepository.list(PageRequest.of(1,
                    PageRequest.MAX_SIZE, null, sort, "attendance|" + id, clock), LiveClassParticipantRepository
                    .ofParent(id)).items().stream().map(this::dtoLiveClassParticipant).toList());
        });
    }

    /** Marks the class's calendar invites retrying and counts the attempt; comms delivers them from the event. */
    public LiveClass retryLiveClassInvites(UUID id) {
        return doInWriteTransaction(() -> {
            LiveClassEntity liveClass = accessible(id, PERMISSION_RETRY_INVITES);
            if (meetings.queueInvites(id, liveClass.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The live class changed meanwhile; try again.");
            }
            audit.recordTransition(AGGREGATE, id, "retry_invites", List.of("inviteStatus", "inviteAttempts"), null);
            outbox.publish(AGGREGATE + ".invites_requested", AGGREGATE, id, LmsEvents.payload(id));
            return dto(repository.findById(id).orElseThrow());
        });
    }

    /** The class for its host or the cohort's teaching staff (403 otherwise), with a meeting; no provider needed. */
    private LiveClassEntity accessible(UUID id, String permission) {
        return accessible(id, permission, false);
    }

    private LiveClassEntity accessible(UUID id, String permission, boolean endedAllowed) {
        DataScope scope = DataScope.current(permission);
        LiveClassEntity liveClass = loadVisible(scope, id);
        scope.requireWritable(institutionOf(liveClass));
        CohortEntity cohort = access.visible(liveClass.getCohortId(), permission);
        if (!AuthContext.require().userId().equals(liveClass.getHostUserId()) && !access.isTeachingStaff(cohort)) {
            throw new PermissionDeniedException("Only the host or the cohort's teachers may do this.");
        }
        if (!endedAllowed && liveClass.getEndedAt() != null) {
            throw new InvalidStateException("The live class has ended.");
        }
        if (liveClass.getMeetingId() == null || liveClass.getConferencingAccountId() == null) {
            throw new InvalidStateException("The live class has no meeting at a provider.");
        }
        return liveClass;
    }

    private MeetingContext hosted(UUID id, String permission) {
        return hosted(id, permission, false);
    }

    /** Like {@link #accessible}, with the class's provider and account resolved for a meeting call. */
    private MeetingContext hosted(UUID id, String permission, boolean endedAllowed) {
        LiveClassEntity liveClass = accessible(id, permission, endedAllowed);
        ConferencingAccountEntity account = conferencingAccounts.findById(liveClass.getConferencingAccountId())
                .orElseThrow(() -> new InvalidStateException("The live class's conferencing account is gone."));
        return new MeetingContext(liveClass, providers.of(account.getProvider()), account(account));
    }

    private CohortEntity writableCohort(DataScope scope, UUID cohortId) {
        CohortEntity cohort = cohorts.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException(CohortAccess.COHORT, cohortId));
        if (!scope.canRead(CohortAccess.institutionOf(cohort))) {
            throw new ResourceNotFoundException(CohortAccess.COHORT, cohortId);
        }
        scope.requireWritable(CohortAccess.institutionOf(cohort));
        if ("archived".equals(cohort.getLifecycleStatus())) {
            throw new InvalidStateException("An archived cohort is read-only.");
        }
        return cohort;
    }

    private MeetingProvider.Account account(ConferencingAccountEntity account) {
        String secret = account.getClientSecretEnc() == null ? null
                : cipher.decrypt(account.getClientSecretEnc(), account.getTenantId(), SECRET_CLIENT_SECRET)
                        .orElse(null);
        if (account.getExternalAccountId() == null || account.getClientId() == null || secret == null) {
            throw new ConferencingNotConfiguredException(account.getProvider());
        }
        return new MeetingProvider.Account(account.getExternalAccountId(), account.getClientId(), secret);
    }

    private static ZoneId zone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException unknown) {
            throw new ValidationFailedException(List.of(new FieldError("timezone", "is not a known time zone",
                    "INVALID_TIMEZONE")));
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
