package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.CloseNoticeRepliesRequest;
import com.mysms.comms.api.model.DeactivateNoticeRequest;
import com.mysms.comms.api.model.Notice;
import com.mysms.comms.api.model.NoticeAudience;
import com.mysms.comms.api.model.NoticeAudienceUpdate;
import com.mysms.comms.api.model.NoticeUpdate;
import com.mysms.comms.api.model.NoticeVersion;
import com.mysms.comms.api.model.PublishNoticeRequest;
import com.mysms.comms.api.model.ReactivateNoticeRequest;
import com.mysms.comms.api.model.UnpublishNoticeRequest;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.NoticeAudienceEntity;
import com.mysms.comms.domain.NoticeEntity;
import com.mysms.comms.domain.NoticeVersionEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.NoticeAudienceMapper;
import com.mysms.comms.mapper.NoticeMapper;
import com.mysms.comms.mapper.NoticeVersionMapper;
import com.mysms.comms.repository.NoticeAudienceRepository;
import com.mysms.comms.repository.NoticeRepository;
import com.mysms.comms.repository.NoticeVersionRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
 * Business service of comms.notice and of the records that exist only as part of a Notice (NoticeAudience,
 * NoticeVersion): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class NoticeService extends AppTransactionManager {

    public static final String AGGREGATE = NoticeEntity.TABLE;
    static final String PERMISSION_LIST = "comms.notice.read";
    static final String PERMISSION_GET = "comms.notice.read";
    static final String PERMISSION_CREATE = "comms.notice.create";
    static final String PERMISSION_UPDATE = "comms.notice.update";
    static final String PERMISSION_DELETE = "comms.notice.delete";
    static final String PERMISSION_PUBLISH_NOTICE = "comms.notice.publish_notice";
    static final String PERMISSION_UNPUBLISH_NOTICE = "comms.notice.unpublish_notice";
    static final String PERMISSION_DEACTIVATE_NOTICE = "comms.notice.deactivate_notice";
    static final String PERMISSION_CLOSE_NOTICE_REPLIES = "comms.notice.close_notice_replies";
    static final String PERMISSION_REACTIVATE_NOTICE = "comms.notice.reactivate_notice";
    /** The contract's NoticeUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "audience", "classSectionId", "backgroundId", "title", "body",
            "attachmentFileId", "publishAt", "expireAt", "cohortId", "showInCalendar", "allowReplies",
            "discussionTopicId", "isClosed", "isDeactivated", "authorComment");

    public static final String NOTICE_AUDIENCE_AGGREGATE = NoticeAudienceEntity.TABLE;
    static final String NOTICE_AUDIENCE_PERMISSION_LIST = "comms.notice_audience.read";
    static final String NOTICE_AUDIENCE_PERMISSION_GET = "comms.notice_audience.read";
    static final String NOTICE_AUDIENCE_PERMISSION_CREATE = "comms.notice_audience.create";
    static final String NOTICE_AUDIENCE_PERMISSION_UPDATE = "comms.notice_audience.update";
    static final String NOTICE_AUDIENCE_PERMISSION_DELETE = "comms.notice_audience.delete";
    /** The contract's NoticeAudienceUpdate fields, in apply order. */
    static final List<String> NOTICE_AUDIENCE_WRITABLE = List.of("institutionId", "roleId");

    public static final String NOTICE_VERSION_AGGREGATE = NoticeVersionEntity.TABLE;
    static final String NOTICE_VERSION_PERMISSION_LIST = "comms.notice_version.read";
    static final String NOTICE_VERSION_PERMISSION_GET = "comms.notice_version.read";

    private final NoticeRepository repository;
    private final NoticeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final NoticeAudienceRepository noticeAudienceRepository;
    private final NoticeAudienceMapper noticeAudienceMapper;
    private final NoticeVersionRepository noticeVersionRepository;
    private final NoticeVersionMapper noticeVersionMapper;

    public NoticeService(NoticeRepository repository,
            NoticeMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            NoticeAudienceRepository noticeAudienceRepository,
            NoticeAudienceMapper noticeAudienceMapper,
            NoticeVersionRepository noticeVersionRepository,
            NoticeVersionMapper noticeVersionMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.noticeAudienceRepository = noticeAudienceRepository;
        this.noticeAudienceMapper = noticeAudienceMapper;
        this.noticeVersionRepository = noticeVersionRepository;
        this.noticeVersionMapper = noticeVersionMapper;
    }

    public Notice getNotice(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Notice> listNotices(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, NoticeRepository.SORT_FIELDS, NoticeRepository.DEFAULT_ORDER,
                NoticeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NoticeRepository.FILTER_FIELDS)
                .and(NoticeRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<NoticeEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Notice createNotice(Notice request, boolean validateOnly) {
        validator.validate(request);
        NoticeEntity entity = mapper.toEntity(request);
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
            NoticeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Notice updateNotice(UUID id, String ifMatch, NoticeUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            NoticeEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNotice(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            NoticeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByNoticeAudienceNoticeId(id)) {
                throw new InvalidStateException(
                        "The notice is still referenced by comms.notice_audience rows; remove those first.");
            }
            if (repository.referencedByNoticeVersionNoticeId(id)) {
                throw new InvalidStateException(
                        "The notice is still referenced by comms.notice_version rows; remove those first.");
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
     * comms.PublishNotice: Publish. Allowed while is_published is false and is_closed is false, on a row the caller may
     * see and write; otherwise 409 and nothing changes.
     */
    public Notice publishNotice(UUID id, PublishNoticeRequest request) {
        DataScope scope = DataScope.current(PERMISSION_PUBLISH_NOTICE);
        return doInWriteTransaction(() -> {
            NoticeEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(!Boolean.TRUE.equals(entity.getIsPublished()) && !Boolean.TRUE.equals(entity.getIsClosed()))) {
                throw new InvalidStateException("The notice cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.publishNotice(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "publish", List.of("isPublished"), null);
            outbox.publish(AGGREGATE + ".publish", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.UnpublishNotice: Unpublish. Allowed while is_published is true, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public Notice unpublishNotice(UUID id, UnpublishNoticeRequest request) {
        DataScope scope = DataScope.current(PERMISSION_UNPUBLISH_NOTICE);
        return doInWriteTransaction(() -> {
            NoticeEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Boolean.TRUE.equals(entity.getIsPublished()))) {
                throw new InvalidStateException("The notice cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.unpublishNotice(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "unpublish", List.of("isPublished"), null);
            outbox.publish(AGGREGATE + ".unpublish", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.DeactivateNotice: Withdraw a notice. Allowed while is_closed is false, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
     */
    public Notice deactivateNotice(UUID id, DeactivateNoticeRequest request) {
        DataScope scope = DataScope.current(PERMISSION_DEACTIVATE_NOTICE);
        return doInWriteTransaction(() -> {
            NoticeEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(!Boolean.TRUE.equals(entity.getIsClosed()))) {
                throw new InvalidStateException("The notice cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.deactivateNotice(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "deactivate", List.of("isClosed"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".deactivate", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.CloseNoticeReplies: Stop accepting replies. Allowed while allow_replies is true, on a row the caller may
     * see and write; otherwise 409 and nothing changes.
     */
    public Notice closeNoticeReplies(UUID id, CloseNoticeRepliesRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CLOSE_NOTICE_REPLIES);
        return doInWriteTransaction(() -> {
            NoticeEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!Boolean.TRUE.equals(entity.getAllowReplies())) {
                throw new InvalidStateException("The notice does not accept replies.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.closeNoticeReplies(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "close_replies", List.of("allowReplies"), null);
            outbox.publish(AGGREGATE + ".close_replies", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.ReactivateNotice: Restore a withdrawn notice. Allowed while is_closed is true, on a row the caller may see
     * and write; otherwise 409 and nothing changes.
     */
    public Notice reactivateNotice(UUID id, ReactivateNoticeRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REACTIVATE_NOTICE);
        return doInWriteTransaction(() -> {
            NoticeEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Boolean.TRUE.equals(entity.getIsClosed()))) {
                throw new InvalidStateException("The notice cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.reactivateNotice(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reactivate", List.of("isClosed"), null);
            outbox.publish(AGGREGATE + ".reactivate", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NoticeEntity loadVisible(DataScope scope, UUID id) {
        NoticeEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(NoticeEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected NoticeUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(NoticeEntity entity, NoticeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "audience" -> entity.setAudience(Optional.ofNullable(changes.getAudience()).map(v -> v.getValue())
                        .orElse(null));
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "backgroundId" -> entity.setBackgroundId(changes.getBackgroundId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "body" -> entity.setBody(changes.getBody());
                case "attachmentFileId" -> entity.setAttachmentFileId(changes.getAttachmentFileId());
                case "publishAt" -> entity.setPublishAt(changes.getPublishAt());
                case "expireAt" -> entity.setExpireAt(changes.getExpireAt());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "showInCalendar" -> entity.setShowInCalendar(changes.getShowInCalendar());
                case "allowReplies" -> entity.setAllowReplies(changes.getAllowReplies());
                case "discussionTopicId" -> entity.setDiscussionTopicId(changes.getDiscussionTopicId());
                case "isClosed" -> entity.setIsClosed(changes.getIsClosed());
                case "isDeactivated" -> entity.setIsDeactivated(changes.getIsDeactivated());
                case "authorComment" -> entity.setAuthorComment(changes.getAuthorComment());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(NoticeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAudience() == null) {
            errors.add(new FieldError("audience", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getBody() == null) {
            errors.add(new FieldError("body", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowInCalendar() == null) {
            errors.add(new FieldError("showInCalendar", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowReplies() == null) {
            errors.add(new FieldError("allowReplies", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsClosed() == null) {
            errors.add(new FieldError("isClosed", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsDeactivated() == null) {
            errors.add(new FieldError("isDeactivated", "must not be null", "NotNull"));
        }
        if (entity.getBackgroundId() != null && !repository.existsBackgroundId(entity.getBackgroundId())) {
            errors.add(new FieldError("backgroundId", "notice background does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(NoticeEntity entity) {

    }

    // ---- NoticeAudience ----

    public NoticeAudience getNoticeAudience(UUID noticeId, UUID id) {
        return doInReadTransaction(() -> {
            return noticeAudienceMapper.toDto(loadVisibleNoticeAudience(DataScope.current(NOTICE_AUDIENCE_PERMISSION_GET),
                    noticeId, id));
        });
    }

    public Page<NoticeAudience> listNoticeAudiences(UUID noticeId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NoticeAudienceRepository.SORT_FIELDS, NoticeAudienceRepository.DEFAULT_ORDER,
                NoticeAudienceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NoticeAudienceRepository.FILTER_FIELDS)
                .and(NoticeAudienceRepository.ofParent(noticeId))
                        .and(NoticeAudienceRepository.scope(DataScope.current(NOTICE_AUDIENCE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + noticeId, clock);
        return doInReadTransaction(() -> {
            Page<NoticeAudienceEntity> rows = noticeAudienceRepository.list(page, where);
            return rows.map(noticeAudienceMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public NoticeAudience createNoticeAudience(UUID noticeId, NoticeAudience request, boolean validateOnly) {
        validator.validate(request);
        NoticeAudienceEntity entity = noticeAudienceMapper.toEntity(request);
        entity.setNoticeId(noticeId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(NOTICE_AUDIENCE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!noticeAudienceRepository.existsNoticeId(entity.getNoticeId())) {
                throw new ResourceNotFoundException("comms.notice", entity.getNoticeId());
            }
            assertValidNoticeAudience(entity, true);
            scope.requireWritable(institutionOfNoticeAudience(entity));
            assertUniqueNoticeAudience(entity);
            if (validateOnly) {
                return noticeAudienceMapper.toDto(entity);
            }
            NoticeAudienceEntity stored = noticeAudienceRepository.insert(entity);
            audit.recordCreate(NOTICE_AUDIENCE_AGGREGATE, stored.getId());
            outbox.publish(NOTICE_AUDIENCE_AGGREGATE + CommsEvents.CREATED, NOTICE_AUDIENCE_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return noticeAudienceMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public NoticeAudience updateNoticeAudience(UUID noticeId, UUID id, String ifMatch, NoticeAudienceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTICE_AUDIENCE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTICE_AUDIENCE_PERMISSION_UPDATE);
            NoticeAudienceEntity entity = loadVisibleNoticeAudience(scope, noticeId, id);
            scope.requireWritable(institutionOfNoticeAudience(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyNoticeAudience(entity, changes, fields);
            validator.validate(noticeAudienceMapper.toDto(entity));
            assertValidNoticeAudience(entity, false);
            scope.requireWritable(institutionOfNoticeAudience(entity));
            assertUniqueNoticeAudience(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return noticeAudienceMapper.toDto(entity);
            }
            if (noticeAudienceRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(NOTICE_AUDIENCE_AGGREGATE, id, fields);
            outbox.publish(NOTICE_AUDIENCE_AGGREGATE + CommsEvents.UPDATED, NOTICE_AUDIENCE_AGGREGATE, id,
                    CommsEvents.payload(id));
            return noticeAudienceMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteNoticeAudience(UUID noticeId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(NOTICE_AUDIENCE_PERMISSION_DELETE);
            NoticeAudienceEntity entity = loadVisibleNoticeAudience(scope, noticeId, id);
            scope.requireWritable(institutionOfNoticeAudience(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (noticeAudienceRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(NOTICE_AUDIENCE_AGGREGATE, id);
            outbox.publish(NOTICE_AUDIENCE_AGGREGATE + CommsEvents.DELETED, NOTICE_AUDIENCE_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NoticeAudienceEntity loadVisibleNoticeAudience(DataScope scope, UUID noticeId, UUID id) {
        NoticeAudienceEntity entity = noticeAudienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_AUDIENCE_AGGREGATE, id));
        if (!scope.canRead(institutionOfNoticeAudience(entity)) || !noticeId.equals(entity.getNoticeId())) {
            throw new ResourceNotFoundException(NOTICE_AUDIENCE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOfNoticeAudience(NoticeAudienceEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected NoticeAudienceUpdate fields; explicit per field so a mask can clear values. */
    private static void applyNoticeAudience(NoticeAudienceEntity entity, NoticeAudienceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "roleId" -> entity.setRoleId(changes.getRoleId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidNoticeAudience(NoticeAudienceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getNoticeId() == null) {
            errors.add(new FieldError("noticeId", "must not be null", "NotNull"));
        }
        if (entity.getNoticeId() != null && !noticeAudienceRepository.existsNoticeId(entity.getNoticeId())) {
            errors.add(new FieldError("noticeId", "notice does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueNoticeAudience(NoticeAudienceEntity entity) {

    }

    // ---- NoticeVersion ----

    public NoticeVersion getNoticeVersion(UUID noticeId, UUID id) {
        return doInReadTransaction(() -> {
            return noticeVersionMapper.toDto(loadVisibleNoticeVersion(DataScope.current(NOTICE_VERSION_PERMISSION_GET),
                    noticeId, id));
        });
    }

    public Page<NoticeVersion> listNoticeVersions(UUID noticeId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, NoticeVersionRepository.SORT_FIELDS, NoticeVersionRepository.DEFAULT_ORDER,
                NoticeVersionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NoticeVersionRepository.FILTER_FIELDS)
                .and(NoticeVersionRepository.ofParent(noticeId))
                        .and(NoticeVersionRepository.scope(DataScope.current(NOTICE_VERSION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + noticeId, clock);
        return doInReadTransaction(() -> {
            Page<NoticeVersionEntity> rows = noticeVersionRepository.list(page, where);
            return rows.map(noticeVersionMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private NoticeVersionEntity loadVisibleNoticeVersion(DataScope scope, UUID noticeId, UUID id) {
        NoticeVersionEntity entity = noticeVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_VERSION_AGGREGATE, id));
        if (!scope.canRead(institutionOfNoticeVersion(entity)) || !noticeId.equals(entity.getNoticeId())) {
            throw new ResourceNotFoundException(NOTICE_VERSION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the comms.notice the row belongs to. */
    private Optional<UUID> institutionOfNoticeVersion(NoticeVersionEntity entity) {
        return noticeVersionRepository.institutionOf(entity.getNoticeId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidNoticeVersion(NoticeVersionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getNoticeId() == null) {
            errors.add(new FieldError("noticeId", "must not be null", "NotNull"));
        }
        if (entity.getVersionedAt() == null) {
            errors.add(new FieldError("versionedAt", "must not be null", "NotNull"));
        }
        if (entity.getNoticeId() != null && !noticeVersionRepository.existsNoticeId(entity.getNoticeId())) {
            errors.add(new FieldError("noticeId", "notice does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueNoticeVersion(NoticeVersionEntity entity) {

    }
}
