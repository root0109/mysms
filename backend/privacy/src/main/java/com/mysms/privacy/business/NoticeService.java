package com.mysms.privacy.business;

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
import com.mysms.privacy.api.model.MyNotice;
import com.mysms.privacy.api.model.Notice;
import com.mysms.privacy.api.model.NoticeTranslation;
import com.mysms.privacy.api.model.NoticeTranslationUpdate;
import com.mysms.privacy.api.model.NoticeUpdate;
import com.mysms.privacy.api.model.NoticeVersion;
import com.mysms.privacy.api.model.NoticeVersionUpdate;
import com.mysms.privacy.api.model.PublishNoticeVersionRequest;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.NoticeEntity;
import com.mysms.privacy.domain.NoticeTranslationEntity;
import com.mysms.privacy.domain.NoticeVersionEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.NoticeMapper;
import com.mysms.privacy.mapper.NoticeTranslationMapper;
import com.mysms.privacy.mapper.NoticeVersionMapper;
import com.mysms.privacy.model.Sha256;
import com.mysms.privacy.repository.NoticeRepository;
import com.mysms.privacy.repository.NoticeTranslationRepository;
import com.mysms.privacy.repository.NoticeVersionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The notice aggregate: a tenant's privacy notices (privacy.notice), their versions (privacy.notice_version) and the
 * versions' translations (privacy.notice_translation). Every row is tenant-level data, changed only through a
 * tenant-wide grant; a version is edited while a draft and frozen, with its translations, once published.
 */
@Service
public class NoticeService extends AppTransactionManager {

    public static final String AGGREGATE = NoticeEntity.TABLE;
    static final String PERMISSION_CREATE = "privacy.notice.create";
    static final String PERMISSION_UPDATE = "privacy.notice.update";
    static final String PERMISSION_DELETE = "privacy.notice.delete";
    /** The contract's NoticeUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("title");

    public static final String NOTICE_VERSION_AGGREGATE = NoticeVersionEntity.TABLE;
    static final String NOTICE_VERSION_PERMISSION_CREATE = "privacy.notice_version.create";
    static final String NOTICE_VERSION_PERMISSION_UPDATE = "privacy.notice_version.update";
    static final String NOTICE_VERSION_PERMISSION_PUBLISH = "privacy.notice_version.publish";
    /** The contract's NoticeVersionUpdate fields, in apply order. */
    static final List<String> NOTICE_VERSION_WRITABLE =
            List.of("requiresReconsent", "purposeCodes", "grievanceContact", "documentFileId");

    public static final String NOTICE_TRANSLATION_AGGREGATE = NoticeTranslationEntity.TABLE;
    static final String NOTICE_TRANSLATION_PERMISSION_CREATE = "privacy.notice_translation.create";
    static final String NOTICE_TRANSLATION_PERMISSION_UPDATE = "privacy.notice_translation.update";
    static final String NOTICE_TRANSLATION_PERMISSION_DELETE = "privacy.notice_translation.delete";
    /** The contract's NoticeTranslationUpdate fields, in apply order. */
    static final List<String> NOTICE_TRANSLATION_WRITABLE = List.of("language", "bodyMarkdown", "summaryForChildren");

    /** No institution owns a row of this aggregate. */
    private static final Optional<UUID> TENANT_LEVEL = Optional.empty();
    private static final String DRAFT = "draft";
    /** The language a notice is shown in when it has no translation into the one asked for. */
    private static final String FALLBACK_LANGUAGE = "en";

    private final NoticeRepository repository;
    private final ConsentStateWriter stateWriter;
    private final NoticeMapper mapper;
    private final NoticeVersionRepository noticeVersionRepository;
    private final NoticeVersionMapper noticeVersionMapper;
    private final NoticeTranslationRepository noticeTranslationRepository;
    private final NoticeTranslationMapper noticeTranslationMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public NoticeService(NoticeRepository repository,
            ConsentStateWriter stateWriter,
            NoticeMapper mapper,
            NoticeVersionRepository noticeVersionRepository,
            NoticeVersionMapper noticeVersionMapper,
            NoticeTranslationRepository noticeTranslationRepository,
            NoticeTranslationMapper noticeTranslationMapper,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.stateWriter = stateWriter;
        this.mapper = mapper;
        this.noticeVersionRepository = noticeVersionRepository;
        this.noticeVersionMapper = noticeVersionMapper;
        this.noticeTranslationRepository = noticeTranslationRepository;
        this.noticeTranslationMapper = noticeTranslationMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Notice getNotice(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(loadNotice(id)));
    }

    public Page<Notice> listNotices(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, NoticeRepository.SORT_FIELDS, NoticeRepository.DEFAULT_ORDER,
                NoticeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NoticeRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /** Creates the notice; with validateOnly every check runs and nothing is written. */
    public Notice createNotice(Notice request, boolean validateOnly) {
        requireTenantWide(PERMISSION_CREATE);
        validator.validate(request);
        NoticeEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant());
        return doInWriteTransaction(() -> {
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            NoticeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PrivacyEvents.CREATED, AGGREGATE, stored.getId(),
                    PrivacyEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /** Partial update under If-Match; the code is set at creation and never changes. */
    public Notice updateNotice(UUID id, String ifMatch, NoticeUpdate changes, String updateMask, boolean validateOnly) {
        requireTenantWide(PERMISSION_UPDATE);
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            NoticeEntity entity = loadNotice(id);
            requireCurrent(entity.getRowVersion(), expected);
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw stale();
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + PrivacyEvents.UPDATED, AGGREGATE, id, PrivacyEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /**
     * Soft delete under If-Match; refused while a purpose setting names the notice or one of its versions is active
     * (what was shown stays as consent evidence).
     */
    public void deleteNotice(UUID id, String ifMatch) {
        requireTenantWide(PERMISSION_DELETE);
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            NoticeEntity entity = loadNotice(id);
            requireCurrent(entity.getRowVersion(), expected);
            if (repository.referencedByTenantPurpose(id)) {
                throw new InvalidStateException(
                        "The notice is still named by privacy.tenant_purpose rows; give those purposes another notice first.");
            }
            if (repository.hasActiveVersion(id)) {
                throw new InvalidStateException("The notice has an active version, which is kept as consent evidence.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw stale();
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PrivacyEvents.DELETED, AGGREGATE, id, PrivacyEvents.payload(id));
        });
    }

    /** The live notice of the caller's tenant; anything else is 404. */
    private NoticeEntity loadNotice(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** Applies the selected NoticeUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(NoticeEntity entity, NoticeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    private void assertUnique(NoticeEntity entity) {
        if (repository.codeTaken(entity)) {
            throw new DuplicateResourceException("A notice with the same code already exists.");
        }
    }

    /**
     * privacy.GetMyNotice: what the consent centre shows before a decision — the active version of the tenant's notice
     * with the code, in the language asked for, else in English; 404 when there is no such notice, no active version
     * or neither translation. Any signed-in person may read it.
     */
    public MyNotice getMyNotice(String code, String language) {
        return doInReadTransaction(() -> {
            NoticeEntity notice = repository.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, code));
            NoticeVersionEntity version = noticeVersionRepository.findActive(notice.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, code));
            NoticeTranslationEntity translation = noticeTranslationRepository.findByLanguage(version.getId(), language)
                    .or(() -> noticeTranslationRepository.findByLanguage(version.getId(), FALLBACK_LANGUAGE))
                    .orElseThrow(() -> new ResourceNotFoundException(NOTICE_TRANSLATION_AGGREGATE, language));
            return new MyNotice()
                    .noticeVersionId(version.getId())
                    .noticeCode(notice.getCode())
                    .version(version.getVersion())
                    .language(translation.getLanguage())
                    .purposeCodes(List.copyOf(version.getPurposeCodes()))
                    .requiresReconsent(version.getRequiresReconsent())
                    .grievanceContact(noticeVersionMapper.toContract(version.getGrievanceContact()))
                    .bodyMarkdown(translation.getBodyMarkdown())
                    .summaryForChildren(translation.getSummaryForChildren())
                    .contentSha256(translation.getContentSha256())
                    .publishedAt(version.getPublishedAt());
        });
    }

    // ---- NoticeVersion: a record of the Notice aggregate ----

    public NoticeVersion getNoticeVersion(UUID noticeId, UUID id) {
        return doInReadTransaction(() -> noticeVersionMapper.toDto(loadNoticeVersion(noticeId, id)));
    }

    public Page<NoticeVersion> listNoticeVersions(UUID noticeId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, NoticeVersionRepository.SORT_FIELDS,
                NoticeVersionRepository.DEFAULT_ORDER, NoticeVersionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NoticeVersionRepository.FILTER_FIELDS)
                .and(NoticeVersionRepository.ofParent(noticeId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + noticeId, clock);
        return doInReadTransaction(() -> {
            loadNotice(noticeId);
            return noticeVersionRepository.list(page, where).map(noticeVersionMapper::toDto);
        });
    }

    /**
     * Creates the notice's next version as a draft (number = the highest so far + 1); with validateOnly every check
     * runs and nothing is written.
     */
    public NoticeVersion createNoticeVersion(UUID noticeId, NoticeVersion request, boolean validateOnly) {
        requireTenantWide(NOTICE_VERSION_PERMISSION_CREATE);
        validator.validate(request);
        NoticeVersionEntity entity = noticeVersionMapper.toEntity(request);
        entity.setNoticeId(noticeId);
        entity.setStatus(DRAFT);
        entity.setRequiresReconsent(Boolean.TRUE.equals(request.getRequiresReconsent()));
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant());
        return doInWriteTransaction(() -> {
            loadNotice(noticeId);
            assertValidNoticeVersion(entity);
            entity.setVersion(noticeVersionRepository.nextVersion(noticeId));
            if (validateOnly) {
                return noticeVersionMapper.toDto(entity);
            }
            NoticeVersionEntity stored = noticeVersionRepository.insert(entity);
            audit.recordCreate(NOTICE_VERSION_AGGREGATE, stored.getId());
            outbox.publish(NOTICE_VERSION_AGGREGATE + PrivacyEvents.CREATED, NOTICE_VERSION_AGGREGATE, stored.getId(),
                    PrivacyEvents.payload(stored.getId()));
            return noticeVersionMapper.toDto(stored);
        });
    }

    /** Partial update of a draft under If-Match; a published version never changes (409). */
    public NoticeVersion updateNoticeVersion(UUID noticeId, UUID id, String ifMatch, NoticeVersionUpdate changes,
            String updateMask, boolean validateOnly) {
        requireTenantWide(NOTICE_VERSION_PERMISSION_UPDATE);
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTICE_VERSION_WRITABLE);
        return doInWriteTransaction(() -> {
            NoticeVersionEntity entity = loadNoticeVersion(noticeId, id);
            requireCurrent(entity.getRowVersion(), expected);
            requireDraftNoticeVersion(entity);
            applyNoticeVersion(entity, changes, fields);
            validator.validate(noticeVersionMapper.toDto(entity));
            assertValidNoticeVersion(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return noticeVersionMapper.toDto(entity);
            }
            if (noticeVersionRepository.update(entity, expected) == 0) {
                throw stale();
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(NOTICE_VERSION_AGGREGATE, id, fields);
            outbox.publish(NOTICE_VERSION_AGGREGATE + PrivacyEvents.UPDATED, NOTICE_VERSION_AGGREGATE, id,
                    PrivacyEvents.payload(id));
            return noticeVersionMapper.toDto(entity);
        });
    }

    /**
     * privacy.PublishNoticeVersion: a draft with at least one translation becomes the notice's active version and the
     * previously active one is retired, superseded by it, in the same transaction. Compares the body etag when sent.
     * A version that asks everyone again has the consent answers of every purpose naming the notice recomputed in
     * that transaction: grants recorded before it no longer count.
     */
    public NoticeVersion publishNoticeVersion(UUID noticeId, UUID id, PublishNoticeVersionRequest request) {
        requireTenantWide(NOTICE_VERSION_PERMISSION_PUBLISH);
        OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
        return doInWriteTransaction(() -> {
            NoticeVersionEntity draft = loadNoticeVersion(noticeId, id);
            if (expected.isPresent() && expected.getAsInt() != draft.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!DRAFT.equals(draft.getStatus())) {
                throw new InvalidStateException("Only a draft notice version can be published.");
            }
            if (!noticeTranslationRepository.existsForVersion(id)) {
                throw new InvalidStateException("A notice version needs a translation before it is published.");
            }
            assertValidNoticeVersion(draft);
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            UUID actor = Actor.current().orElse(null);
            Optional<NoticeVersionEntity> previous = noticeVersionRepository.findActive(noticeId);
            if (previous.isPresent()) {
                retireNoticeVersion(previous.get(), id, now, actor);
            }
            if (noticeVersionRepository.publish(id, draft.getRowVersion(), now, actor) == 0) {
                throw stale();
            }
            audit.recordTransition(NOTICE_VERSION_AGGREGATE, id, "publish", List.of("status", "publishedAt"), null);
            outbox.publish(NOTICE_VERSION_AGGREGATE + PrivacyEvents.PUBLISHED, NOTICE_VERSION_AGGREGATE, id,
                    PrivacyEvents.noticeVersionPublished(id, noticeId, draft.getVersion(), draft.getPurposeCodes(),
                            Boolean.TRUE.equals(draft.getRequiresReconsent())));
            if (Boolean.TRUE.equals(draft.getRequiresReconsent())) {
                repository.purposesNaming(noticeId).forEach(stateWriter::recomputePurpose);
            }
            return noticeVersionMapper.toDto(loadNoticeVersion(noticeId, id));
        });
    }

    /** active → retired, superseded by the version being published. */
    private void retireNoticeVersion(NoticeVersionEntity active, UUID successor, OffsetDateTime at, UUID actor) {
        if (noticeVersionRepository.retire(active.getId(), active.getRowVersion(), successor, at, actor) == 0) {
            throw stale();
        }
        audit.recordTransition(NOTICE_VERSION_AGGREGATE, active.getId(), "retire", List.of("status", "supersededById"),
                null);
        outbox.publish(NOTICE_VERSION_AGGREGATE + PrivacyEvents.RETIRED, NOTICE_VERSION_AGGREGATE, active.getId(),
                PrivacyEvents.payload(active.getId()));
    }

    /** The version, if it belongs to the live notice of the path: another notice's version is 404. */
    private NoticeVersionEntity loadNoticeVersion(UUID noticeId, UUID id) {
        loadNotice(noticeId);
        NoticeVersionEntity entity = noticeVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_VERSION_AGGREGATE, id));
        if (!noticeId.equals(entity.getNoticeId())) {
            throw new ResourceNotFoundException(NOTICE_VERSION_AGGREGATE, id);
        }
        return entity;
    }

    /** A published version, and with it its translations, is frozen: what was shown must stay what is kept. */
    private static void requireDraftNoticeVersion(NoticeVersionEntity version) {
        if (!DRAFT.equals(version.getStatus())) {
            throw new InvalidStateException("The notice version is published; publish a new version to change it.");
        }
    }

    /** Applies the selected NoticeVersionUpdate fields; explicit per field so a mask can clear values. */
    private void applyNoticeVersion(NoticeVersionEntity entity, NoticeVersionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "requiresReconsent" -> entity.setRequiresReconsent(changes.getRequiresReconsent());
                case "purposeCodes" -> entity.setPurposeCodes(changes.getPurposeCodes());
                case "grievanceContact" ->
                        entity.setGrievanceContact(noticeVersionMapper.toOfficer(changes.getGrievanceContact()));
                case "documentFileId" -> entity.setDocumentFileId(changes.getDocumentFileId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Every purpose code names an active catalogue purpose (422 per failing code). */
    private void assertValidNoticeVersion(NoticeVersionEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRequiresReconsent() == null) {
            errors.add(new FieldError("requiresReconsent", "must not be null", "NotNull"));
        }
        List<String> codes = entity.getPurposeCodes();
        Set<String> active = noticeVersionRepository.activePurposeCodes(codes);
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            if (code == null || !active.contains(code)) {
                errors.add(new FieldError("purposeCodes[" + i + "]",
                        "must name an active purpose of the catalogue", "PURPOSE_NOT_ACTIVE"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    // ---- NoticeTranslation: a record of the Notice aggregate ----

    public NoticeTranslation getNoticeTranslation(UUID noticeId, UUID noticeVersionId, UUID id) {
        return doInReadTransaction(() -> noticeTranslationMapper.toDto(
                loadNoticeTranslation(loadNoticeVersion(noticeId, noticeVersionId), id)));
    }

    public Page<NoticeTranslation> listNoticeTranslations(UUID noticeId, UUID noticeVersionId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, NoticeTranslationRepository.SORT_FIELDS,
                NoticeTranslationRepository.DEFAULT_ORDER, NoticeTranslationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, NoticeTranslationRepository.FILTER_FIELDS)
                .and(NoticeTranslationRepository.ofParent(noticeVersionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + noticeVersionId, clock);
        return doInReadTransaction(() -> {
            loadNoticeVersion(noticeId, noticeVersionId);
            return noticeTranslationRepository.list(page, where).map(noticeTranslationMapper::toDto);
        });
    }

    /** Creates a translation of a draft version; with validateOnly every check runs and nothing is written. */
    public NoticeTranslation createNoticeTranslation(UUID noticeId, UUID noticeVersionId, NoticeTranslation request,
            boolean validateOnly) {
        requireTenantWide(NOTICE_TRANSLATION_PERMISSION_CREATE);
        validator.validate(request);
        NoticeTranslationEntity entity = noticeTranslationMapper.toEntity(request);
        entity.setNoticeVersionId(noticeVersionId);
        entity.setContentSha256(hashNoticeTranslation(entity));
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant());
        return doInWriteTransaction(() -> {
            requireDraftNoticeVersion(loadNoticeVersion(noticeId, noticeVersionId));
            assertUniqueNoticeTranslation(entity);
            if (validateOnly) {
                return noticeTranslationMapper.toDto(entity);
            }
            NoticeTranslationEntity stored = noticeTranslationRepository.insert(entity);
            audit.recordCreate(NOTICE_TRANSLATION_AGGREGATE, stored.getId());
            outbox.publish(NOTICE_TRANSLATION_AGGREGATE + PrivacyEvents.CREATED, NOTICE_TRANSLATION_AGGREGATE,
                    stored.getId(), PrivacyEvents.payload(stored.getId()));
            return noticeTranslationMapper.toDto(stored);
        });
    }

    /** Partial update under If-Match while the version is a draft; the content hash follows the new wording. */
    public NoticeTranslation updateNoticeTranslation(UUID noticeId, UUID noticeVersionId, UUID id, String ifMatch,
            NoticeTranslationUpdate changes, String updateMask, boolean validateOnly) {
        requireTenantWide(NOTICE_TRANSLATION_PERMISSION_UPDATE);
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, NOTICE_TRANSLATION_WRITABLE);
        return doInWriteTransaction(() -> {
            NoticeVersionEntity version = loadNoticeVersion(noticeId, noticeVersionId);
            NoticeTranslationEntity entity = loadNoticeTranslation(version, id);
            requireCurrent(entity.getRowVersion(), expected);
            requireDraftNoticeVersion(version);
            applyNoticeTranslation(entity, changes, fields);
            validator.validate(noticeTranslationMapper.toDto(entity));
            entity.setContentSha256(hashNoticeTranslation(entity));
            assertUniqueNoticeTranslation(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return noticeTranslationMapper.toDto(entity);
            }
            if (noticeTranslationRepository.update(entity, expected) == 0) {
                throw stale();
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(NOTICE_TRANSLATION_AGGREGATE, id, fields);
            outbox.publish(NOTICE_TRANSLATION_AGGREGATE + PrivacyEvents.UPDATED, NOTICE_TRANSLATION_AGGREGATE, id,
                    PrivacyEvents.payload(id));
            return noticeTranslationMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match while the version is a draft. */
    public void deleteNoticeTranslation(UUID noticeId, UUID noticeVersionId, UUID id, String ifMatch) {
        requireTenantWide(NOTICE_TRANSLATION_PERMISSION_DELETE);
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            NoticeVersionEntity version = loadNoticeVersion(noticeId, noticeVersionId);
            NoticeTranslationEntity entity = loadNoticeTranslation(version, id);
            requireCurrent(entity.getRowVersion(), expected);
            requireDraftNoticeVersion(version);
            entity.onDeletingData(Actor.current(), clock);
            if (noticeTranslationRepository.softDelete(entity, expected) == 0) {
                throw stale();
            }
            audit.recordDelete(NOTICE_TRANSLATION_AGGREGATE, id);
            outbox.publish(NOTICE_TRANSLATION_AGGREGATE + PrivacyEvents.DELETED, NOTICE_TRANSLATION_AGGREGATE, id,
                    PrivacyEvents.payload(id));
        });
    }

    /** The live translation, if it belongs to the version of the path: another version's translation is 404. */
    private NoticeTranslationEntity loadNoticeTranslation(NoticeVersionEntity version, UUID id) {
        NoticeTranslationEntity entity = noticeTranslationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOTICE_TRANSLATION_AGGREGATE, id));
        if (!version.getId().equals(entity.getNoticeVersionId())) {
            throw new ResourceNotFoundException(NOTICE_TRANSLATION_AGGREGATE, id);
        }
        return entity;
    }

    /** Applies the selected NoticeTranslationUpdate fields; explicit per field so a mask can clear values. */
    private static void applyNoticeTranslation(NoticeTranslationEntity entity, NoticeTranslationUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "language" -> entity.setLanguage(changes.getLanguage());
                case "bodyMarkdown" -> entity.setBodyMarkdown(changes.getBodyMarkdown());
                case "summaryForChildren" -> entity.setSummaryForChildren(changes.getSummaryForChildren());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    private void assertUniqueNoticeTranslation(NoticeTranslationEntity entity) {
        if (noticeTranslationRepository.languageTaken(entity)) {
            throw new DuplicateResourceException("The notice version already has a translation in this language.");
        }
    }

    /**
     * Lowercase hex SHA-256 of what is shown, the value consent evidence records: the UTF-8 body, a blank line, then
     * the children's summary (nothing when there is none).
     */
    private static String hashNoticeTranslation(NoticeTranslationEntity entity) {
        String summary = entity.getSummaryForChildren() == null ? "" : entity.getSummaryForChildren();
        return Sha256.hex(entity.getBodyMarkdown() + "\n\n" + summary);
    }

    // ---- shared by the records of the aggregate ----

    /** Refuses a change without a tenant-wide grant of the permission (403): every row here is tenant-level. */
    private static void requireTenantWide(String permission) {
        DataScope.current(permission).requireWritable(TENANT_LEVEL);
    }

    private static UUID tenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }

    /** 412 when If-Match names another version of the row than the one read. */
    private static void requireCurrent(int rowVersion, int expected) {
        if (rowVersion != expected) {
            throw stale();
        }
    }

    private static PreconditionFailedException stale() {
        return new PreconditionFailedException("If-Match does not match the current ETag.");
    }
}
