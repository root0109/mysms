package com.mysms.campus.library.business;

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
import com.mysms.core.web.Patches;
import com.mysms.campus.library.api.model.Media;
import com.mysms.campus.library.api.model.MediaAuthor;
import com.mysms.campus.library.api.model.MediaAuthorUpdate;
import com.mysms.campus.library.api.model.MediaCourse;
import com.mysms.campus.library.api.model.MediaCourseUpdate;
import com.mysms.campus.library.api.model.MediaPublisher;
import com.mysms.campus.library.api.model.MediaPublisherUpdate;
import com.mysms.campus.library.api.model.MediaSubject;
import com.mysms.campus.library.api.model.MediaSubjectUpdate;
import com.mysms.campus.library.api.model.MediaUnit;
import com.mysms.campus.library.api.model.MediaUnitUpdate;
import com.mysms.campus.library.api.model.MediaUpdate;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.config.LibraryEventsConfig;
import com.mysms.campus.library.domain.MediaAuthorEntity;
import com.mysms.campus.library.domain.MediaCourseEntity;
import com.mysms.campus.library.domain.MediaEntity;
import com.mysms.campus.library.domain.MediaPublisherEntity;
import com.mysms.campus.library.domain.MediaSubjectEntity;
import com.mysms.campus.library.domain.MediaUnitEntity;
import com.mysms.campus.library.events.LibraryEvents;
import com.mysms.campus.library.mapper.MediaAuthorMapper;
import com.mysms.campus.library.mapper.MediaCourseMapper;
import com.mysms.campus.library.mapper.MediaMapper;
import com.mysms.campus.library.mapper.MediaPublisherMapper;
import com.mysms.campus.library.mapper.MediaSubjectMapper;
import com.mysms.campus.library.mapper.MediaUnitMapper;
import com.mysms.campus.library.repository.MediaAuthorRepository;
import com.mysms.campus.library.repository.MediaCourseRepository;
import com.mysms.campus.library.repository.MediaPublisherRepository;
import com.mysms.campus.library.repository.MediaRepository;
import com.mysms.campus.library.repository.MediaSubjectRepository;
import com.mysms.campus.library.repository.MediaUnitRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of library.media and of the records that exist only as part of a Media (MediaAuthor, MediaCourse,
 * MediaPublisher, MediaSubject, MediaUnit): audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class MediaService extends AppTransactionManager {

    public static final String AGGREGATE = MediaEntity.TABLE;
    static final String PERMISSION_LIST = "library.media.read";
    static final String PERMISSION_GET = "library.media.read";
    public static final String PERMISSION_CREATE = "library.media.create";
    static final String PERMISSION_UPDATE = "library.media.update";
    static final String PERMISSION_DELETE = "library.media.delete";

    public static final String MEDIA_AUTHOR_AGGREGATE = MediaAuthorEntity.TABLE;
    static final String MEDIA_AUTHOR_PERMISSION_LIST = "library.media_author.read";
    static final String MEDIA_AUTHOR_PERMISSION_GET = "library.media_author.read";
    static final String MEDIA_AUTHOR_PERMISSION_CREATE = "library.media_author.create";
    static final String MEDIA_AUTHOR_PERMISSION_UPDATE = "library.media_author.update";
    static final String MEDIA_AUTHOR_PERMISSION_DELETE = "library.media_author.delete";

    public static final String MEDIA_COURSE_AGGREGATE = MediaCourseEntity.TABLE;
    static final String MEDIA_COURSE_PERMISSION_LIST = "library.media_course.read";
    static final String MEDIA_COURSE_PERMISSION_GET = "library.media_course.read";
    static final String MEDIA_COURSE_PERMISSION_CREATE = "library.media_course.create";
    static final String MEDIA_COURSE_PERMISSION_UPDATE = "library.media_course.update";
    static final String MEDIA_COURSE_PERMISSION_DELETE = "library.media_course.delete";

    public static final String MEDIA_PUBLISHER_AGGREGATE = MediaPublisherEntity.TABLE;
    static final String MEDIA_PUBLISHER_PERMISSION_LIST = "library.media_publisher.read";
    static final String MEDIA_PUBLISHER_PERMISSION_GET = "library.media_publisher.read";
    static final String MEDIA_PUBLISHER_PERMISSION_CREATE = "library.media_publisher.create";
    static final String MEDIA_PUBLISHER_PERMISSION_UPDATE = "library.media_publisher.update";
    static final String MEDIA_PUBLISHER_PERMISSION_DELETE = "library.media_publisher.delete";

    public static final String MEDIA_SUBJECT_AGGREGATE = MediaSubjectEntity.TABLE;
    static final String MEDIA_SUBJECT_PERMISSION_LIST = "library.media_subject.read";
    static final String MEDIA_SUBJECT_PERMISSION_GET = "library.media_subject.read";
    static final String MEDIA_SUBJECT_PERMISSION_CREATE = "library.media_subject.create";
    static final String MEDIA_SUBJECT_PERMISSION_UPDATE = "library.media_subject.update";
    static final String MEDIA_SUBJECT_PERMISSION_DELETE = "library.media_subject.delete";

    public static final String MEDIA_UNIT_AGGREGATE = MediaUnitEntity.TABLE;
    static final String MEDIA_UNIT_PERMISSION_LIST = "library.media_unit.read";
    static final String MEDIA_UNIT_PERMISSION_GET = "library.media_unit.read";
    static final String MEDIA_UNIT_PERMISSION_CREATE = "library.media_unit.create";
    static final String MEDIA_UNIT_PERMISSION_UPDATE = "library.media_unit.update";
    static final String MEDIA_UNIT_PERMISSION_DELETE = "library.media_unit.delete";

    private final MediaRepository repository;
    private final MediaMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final MediaAuthorRepository mediaAuthorRepository;
    private final MediaAuthorMapper mediaAuthorMapper;
    private final MediaCourseRepository mediaCourseRepository;
    private final MediaCourseMapper mediaCourseMapper;
    private final MediaPublisherRepository mediaPublisherRepository;
    private final MediaPublisherMapper mediaPublisherMapper;
    private final MediaSubjectRepository mediaSubjectRepository;
    private final MediaSubjectMapper mediaSubjectMapper;
    private final MediaUnitRepository mediaUnitRepository;
    private final MediaUnitMapper mediaUnitMapper;

    public MediaService(MediaRepository repository,
            MediaMapper mapper,
            @Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LibraryDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LibraryEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            MediaAuthorRepository mediaAuthorRepository,
            MediaAuthorMapper mediaAuthorMapper,
            MediaCourseRepository mediaCourseRepository,
            MediaCourseMapper mediaCourseMapper,
            MediaPublisherRepository mediaPublisherRepository,
            MediaPublisherMapper mediaPublisherMapper,
            MediaSubjectRepository mediaSubjectRepository,
            MediaSubjectMapper mediaSubjectMapper,
            MediaUnitRepository mediaUnitRepository,
            MediaUnitMapper mediaUnitMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.mediaAuthorRepository = mediaAuthorRepository;
        this.mediaAuthorMapper = mediaAuthorMapper;
        this.mediaCourseRepository = mediaCourseRepository;
        this.mediaCourseMapper = mediaCourseMapper;
        this.mediaPublisherRepository = mediaPublisherRepository;
        this.mediaPublisherMapper = mediaPublisherMapper;
        this.mediaSubjectRepository = mediaSubjectRepository;
        this.mediaSubjectMapper = mediaSubjectMapper;
        this.mediaUnitRepository = mediaUnitRepository;
        this.mediaUnitMapper = mediaUnitMapper;
    }

    public Media getMedia(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Media> listMedias(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, MediaRepository.SORT_FIELDS, MediaRepository.DEFAULT_ORDER,
                MediaRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MediaRepository.FILTER_FIELDS)
                .and(MediaRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<MediaEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Media createMedia(Media request, boolean validateOnly) {
        validator.validate(request);
        MediaEntity entity = mapper.toEntity(request);
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
            MediaEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LibraryEvents.CREATED, AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Media updateMedia(UUID id, String ifMatch, MediaUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            MediaEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
            outbox.publish(AGGREGATE + LibraryEvents.UPDATED, AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMedia(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            MediaEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLoanMediaId(id)) {
                throw new InvalidStateException(
                        "The media is still referenced by library.loan rows; remove those first.");
            }
            if (repository.referencedByMediaAuthorMediaId(id)) {
                throw new InvalidStateException(
                        "The media is still referenced by library.media_author rows; remove those first.");
            }
            if (repository.referencedByMediaCourseMediaId(id)) {
                throw new InvalidStateException(
                        "The media is still referenced by library.media_course rows; remove those first.");
            }
            if (repository.referencedByMediaPublisherMediaId(id)) {
                throw new InvalidStateException(
                        "The media is still referenced by library.media_publisher rows; remove those first.");
            }
            if (repository.referencedByMediaSubjectMediaId(id)) {
                throw new InvalidStateException(
                        "The media is still referenced by library.media_subject rows; remove those first.");
            }
            if (repository.referencedByMediaUnitMediaId(id)) {
                throw new InvalidStateException(
                        "The media is still referenced by library.media_unit rows; remove those first.");
            }
            if (repository.referencedByReservationMediaId(id)) {
                throw new InvalidStateException(
                        "The media is still referenced by library.reservation rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LibraryEvents.DELETED, AGGREGATE, id, LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MediaEntity loadVisible(DataScope scope, UUID id) {
        MediaEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(MediaEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(MediaEntity entity, MediaUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("acquiredAcademicYearId", changes.getAcquiredAcademicYearId(), entity::setAcquiredAcademicYearId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("isbn", changes.getIsbn(), entity::setIsbn);
        patch.apply("internalCode", changes.getInternalCode(), entity::setInternalCode);
        patch.apply("edition", changes.getEdition(), entity::setEdition);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("mediaTypeId", changes.getMediaTypeId(), entity::setMediaTypeId);
        patch.apply("mediaCategoryId", changes.getMediaCategoryId(), entity::setMediaCategoryId);
        patch.apply("coverImageFileId", changes.getCoverImageFileId(), entity::setCoverImageFileId);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(MediaEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getMediaCategoryId() != null && !repository.existsMediaCategoryId(entity.getMediaCategoryId())) {
            errors.add(new FieldError("mediaCategoryId", "media category does not exist", "NOT_FOUND"));
        }
        if (entity.getMediaTypeId() != null && !repository.existsMediaTypeId(entity.getMediaTypeId())) {
            errors.add(new FieldError("mediaTypeId", "media type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(MediaEntity entity) {

    }

    // ---- MediaAuthor ----

    public MediaAuthor getMediaAuthor(UUID mediaId, UUID id) {
        return doInReadTransaction(() -> {
            return mediaAuthorMapper.toDto(loadVisibleMediaAuthor(DataScope.current(MEDIA_AUTHOR_PERMISSION_GET), mediaId, id));
        });
    }

    public Page<MediaAuthor> listMediaAuthors(UUID mediaId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, MediaAuthorRepository.SORT_FIELDS, MediaAuthorRepository.DEFAULT_ORDER,
                MediaAuthorRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MediaAuthorRepository.FILTER_FIELDS)
                .and(MediaAuthorRepository.ofParent(mediaId))
                        .and(MediaAuthorRepository.scope(DataScope.current(MEDIA_AUTHOR_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + mediaId, clock);
        return doInReadTransaction(() -> {
            Page<MediaAuthorEntity> rows = mediaAuthorRepository.list(page, where);
            return rows.map(mediaAuthorMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public MediaAuthor createMediaAuthor(UUID mediaId, MediaAuthor request, boolean validateOnly) {
        validator.validate(request);
        MediaAuthorEntity entity = mediaAuthorMapper.toEntity(request);
        entity.setMediaId(mediaId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(MEDIA_AUTHOR_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!mediaAuthorRepository.existsMediaId(entity.getMediaId())) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            if (!scope.canRead(institutionOfMediaAuthor(entity))) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            assertValidMediaAuthor(entity, true);
            scope.requireWritable(institutionOfMediaAuthor(entity));
            assertUniqueMediaAuthor(entity);
            if (validateOnly) {
                return mediaAuthorMapper.toDto(entity);
            }
            MediaAuthorEntity stored = mediaAuthorRepository.insert(entity);
            audit.recordCreate(MEDIA_AUTHOR_AGGREGATE, stored.getId());
            outbox.publish(MEDIA_AUTHOR_AGGREGATE + LibraryEvents.CREATED, MEDIA_AUTHOR_AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mediaAuthorMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public MediaAuthor updateMediaAuthor(UUID mediaId, UUID id, String ifMatch, MediaAuthorUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_AUTHOR_PERMISSION_UPDATE);
            MediaAuthorEntity entity = loadVisibleMediaAuthor(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaAuthor(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyMediaAuthor(entity, changes);
            validator.validate(mediaAuthorMapper.toDto(entity));
            assertValidMediaAuthor(entity, false);
            scope.requireWritable(institutionOfMediaAuthor(entity));
            assertUniqueMediaAuthor(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mediaAuthorMapper.toDto(entity);
            }
            if (mediaAuthorRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(MEDIA_AUTHOR_AGGREGATE, id, fields);
            outbox.publish(MEDIA_AUTHOR_AGGREGATE + LibraryEvents.UPDATED, MEDIA_AUTHOR_AGGREGATE, id, LibraryEvents.payload(id));
            return mediaAuthorMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMediaAuthor(UUID mediaId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_AUTHOR_PERMISSION_DELETE);
            MediaAuthorEntity entity = loadVisibleMediaAuthor(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaAuthor(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (mediaAuthorRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(MEDIA_AUTHOR_AGGREGATE, id);
            outbox.publish(MEDIA_AUTHOR_AGGREGATE + LibraryEvents.DELETED, MEDIA_AUTHOR_AGGREGATE, id, LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MediaAuthorEntity loadVisibleMediaAuthor(DataScope scope, UUID mediaId, UUID id) {
        MediaAuthorEntity entity = mediaAuthorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDIA_AUTHOR_AGGREGATE, id));
        if (!scope.canRead(institutionOfMediaAuthor(entity)) || !mediaId.equals(entity.getMediaId())) {
            throw new ResourceNotFoundException(MEDIA_AUTHOR_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the library.media the row belongs to. */
    private Optional<UUID> institutionOfMediaAuthor(MediaAuthorEntity entity) {
        return mediaAuthorRepository.institutionOf(entity.getMediaId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyMediaAuthor(MediaAuthorEntity entity, MediaAuthorUpdate changes) {
        Patches patch = new Patches();
        patch.apply("authorId", changes.getAuthorId(), entity::setAuthorId);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidMediaAuthor(MediaAuthorEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMediaId() == null) {
            errors.add(new FieldError("mediaId", "must not be null", "NotNull"));
        }
        if (entity.getAuthorId() == null) {
            errors.add(new FieldError("authorId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getAuthorId() != null && !mediaAuthorRepository.existsAuthorId(entity.getAuthorId())) {
            errors.add(new FieldError("authorId", "author does not exist", "NOT_FOUND"));
        }
        if (entity.getMediaId() != null && !mediaAuthorRepository.existsMediaId(entity.getMediaId())) {
            errors.add(new FieldError("mediaId", "media does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueMediaAuthor(MediaAuthorEntity entity) {
        if (mediaAuthorRepository.takenMediaIdAuthorId1(entity)) {
            throw new DuplicateResourceException("A media author with the same media id and author id already exists.");
        }
    }

    // ---- MediaCourse ----

    public MediaCourse getMediaCourse(UUID mediaId, UUID id) {
        return doInReadTransaction(() -> {
            return mediaCourseMapper.toDto(loadVisibleMediaCourse(DataScope.current(MEDIA_COURSE_PERMISSION_GET), mediaId, id));
        });
    }

    public Page<MediaCourse> listMediaCourses(UUID mediaId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, MediaCourseRepository.SORT_FIELDS, MediaCourseRepository.DEFAULT_ORDER,
                MediaCourseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MediaCourseRepository.FILTER_FIELDS)
                .and(MediaCourseRepository.ofParent(mediaId))
                        .and(MediaCourseRepository.scope(DataScope.current(MEDIA_COURSE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + mediaId, clock);
        return doInReadTransaction(() -> {
            Page<MediaCourseEntity> rows = mediaCourseRepository.list(page, where);
            return rows.map(mediaCourseMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public MediaCourse createMediaCourse(UUID mediaId, MediaCourse request, boolean validateOnly) {
        validator.validate(request);
        MediaCourseEntity entity = mediaCourseMapper.toEntity(request);
        entity.setMediaId(mediaId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(MEDIA_COURSE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!mediaCourseRepository.existsMediaId(entity.getMediaId())) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            if (!scope.canRead(institutionOfMediaCourse(entity))) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            assertValidMediaCourse(entity, true);
            scope.requireWritable(institutionOfMediaCourse(entity));
            assertUniqueMediaCourse(entity);
            if (validateOnly) {
                return mediaCourseMapper.toDto(entity);
            }
            MediaCourseEntity stored = mediaCourseRepository.insert(entity);
            audit.recordCreate(MEDIA_COURSE_AGGREGATE, stored.getId());
            outbox.publish(MEDIA_COURSE_AGGREGATE + LibraryEvents.CREATED, MEDIA_COURSE_AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mediaCourseMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public MediaCourse updateMediaCourse(UUID mediaId, UUID id, String ifMatch, MediaCourseUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_COURSE_PERMISSION_UPDATE);
            MediaCourseEntity entity = loadVisibleMediaCourse(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyMediaCourse(entity, changes);
            validator.validate(mediaCourseMapper.toDto(entity));
            assertValidMediaCourse(entity, false);
            scope.requireWritable(institutionOfMediaCourse(entity));
            assertUniqueMediaCourse(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mediaCourseMapper.toDto(entity);
            }
            if (mediaCourseRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(MEDIA_COURSE_AGGREGATE, id, fields);
            outbox.publish(MEDIA_COURSE_AGGREGATE + LibraryEvents.UPDATED, MEDIA_COURSE_AGGREGATE, id, LibraryEvents.payload(id));
            return mediaCourseMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMediaCourse(UUID mediaId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_COURSE_PERMISSION_DELETE);
            MediaCourseEntity entity = loadVisibleMediaCourse(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (mediaCourseRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(MEDIA_COURSE_AGGREGATE, id);
            outbox.publish(MEDIA_COURSE_AGGREGATE + LibraryEvents.DELETED, MEDIA_COURSE_AGGREGATE, id, LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MediaCourseEntity loadVisibleMediaCourse(DataScope scope, UUID mediaId, UUID id) {
        MediaCourseEntity entity = mediaCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDIA_COURSE_AGGREGATE, id));
        if (!scope.canRead(institutionOfMediaCourse(entity)) || !mediaId.equals(entity.getMediaId())) {
            throw new ResourceNotFoundException(MEDIA_COURSE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the library.media the row belongs to. */
    private Optional<UUID> institutionOfMediaCourse(MediaCourseEntity entity) {
        return mediaCourseRepository.institutionOf(entity.getMediaId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyMediaCourse(MediaCourseEntity entity, MediaCourseUpdate changes) {
        Patches patch = new Patches();
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidMediaCourse(MediaCourseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMediaId() == null) {
            errors.add(new FieldError("mediaId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getMediaId() != null && !mediaCourseRepository.existsMediaId(entity.getMediaId())) {
            errors.add(new FieldError("mediaId", "media does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueMediaCourse(MediaCourseEntity entity) {
        if (mediaCourseRepository.takenMediaIdCourseId1(entity)) {
            throw new DuplicateResourceException("A media course with the same media id and course id already exists.");
        }
    }

    // ---- MediaPublisher ----

    public MediaPublisher getMediaPublisher(UUID mediaId, UUID id) {
        return doInReadTransaction(() -> {
            return mediaPublisherMapper.toDto(loadVisibleMediaPublisher(DataScope.current(MEDIA_PUBLISHER_PERMISSION_GET),
                    mediaId, id));
        });
    }

    public Page<MediaPublisher> listMediaPublishers(UUID mediaId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, MediaPublisherRepository.SORT_FIELDS, MediaPublisherRepository.DEFAULT_ORDER,
                MediaPublisherRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MediaPublisherRepository.FILTER_FIELDS)
                .and(MediaPublisherRepository.ofParent(mediaId))
                        .and(MediaPublisherRepository.scope(DataScope.current(MEDIA_PUBLISHER_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + mediaId, clock);
        return doInReadTransaction(() -> {
            Page<MediaPublisherEntity> rows = mediaPublisherRepository.list(page, where);
            return rows.map(mediaPublisherMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public MediaPublisher createMediaPublisher(UUID mediaId, MediaPublisher request, boolean validateOnly) {
        validator.validate(request);
        MediaPublisherEntity entity = mediaPublisherMapper.toEntity(request);
        entity.setMediaId(mediaId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(MEDIA_PUBLISHER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!mediaPublisherRepository.existsMediaId(entity.getMediaId())) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            if (!scope.canRead(institutionOfMediaPublisher(entity))) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            assertValidMediaPublisher(entity, true);
            scope.requireWritable(institutionOfMediaPublisher(entity));
            assertUniqueMediaPublisher(entity);
            if (validateOnly) {
                return mediaPublisherMapper.toDto(entity);
            }
            MediaPublisherEntity stored = mediaPublisherRepository.insert(entity);
            audit.recordCreate(MEDIA_PUBLISHER_AGGREGATE, stored.getId());
            outbox.publish(MEDIA_PUBLISHER_AGGREGATE + LibraryEvents.CREATED, MEDIA_PUBLISHER_AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mediaPublisherMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public MediaPublisher updateMediaPublisher(UUID mediaId, UUID id, String ifMatch, MediaPublisherUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_PUBLISHER_PERMISSION_UPDATE);
            MediaPublisherEntity entity = loadVisibleMediaPublisher(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaPublisher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyMediaPublisher(entity, changes);
            validator.validate(mediaPublisherMapper.toDto(entity));
            assertValidMediaPublisher(entity, false);
            scope.requireWritable(institutionOfMediaPublisher(entity));
            assertUniqueMediaPublisher(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mediaPublisherMapper.toDto(entity);
            }
            if (mediaPublisherRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(MEDIA_PUBLISHER_AGGREGATE, id, fields);
            outbox.publish(MEDIA_PUBLISHER_AGGREGATE + LibraryEvents.UPDATED, MEDIA_PUBLISHER_AGGREGATE, id,
                    LibraryEvents.payload(id));
            return mediaPublisherMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMediaPublisher(UUID mediaId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_PUBLISHER_PERMISSION_DELETE);
            MediaPublisherEntity entity = loadVisibleMediaPublisher(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaPublisher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (mediaPublisherRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(MEDIA_PUBLISHER_AGGREGATE, id);
            outbox.publish(MEDIA_PUBLISHER_AGGREGATE + LibraryEvents.DELETED, MEDIA_PUBLISHER_AGGREGATE, id,
                    LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MediaPublisherEntity loadVisibleMediaPublisher(DataScope scope, UUID mediaId, UUID id) {
        MediaPublisherEntity entity = mediaPublisherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDIA_PUBLISHER_AGGREGATE, id));
        if (!scope.canRead(institutionOfMediaPublisher(entity)) || !mediaId.equals(entity.getMediaId())) {
            throw new ResourceNotFoundException(MEDIA_PUBLISHER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the library.media the row belongs to. */
    private Optional<UUID> institutionOfMediaPublisher(MediaPublisherEntity entity) {
        return mediaPublisherRepository.institutionOf(entity.getMediaId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyMediaPublisher(MediaPublisherEntity entity, MediaPublisherUpdate changes) {
        Patches patch = new Patches();
        patch.apply("publisherId", changes.getPublisherId(), entity::setPublisherId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidMediaPublisher(MediaPublisherEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMediaId() == null) {
            errors.add(new FieldError("mediaId", "must not be null", "NotNull"));
        }
        if (entity.getPublisherId() == null) {
            errors.add(new FieldError("publisherId", "must not be null", "NotNull"));
        }
        if (entity.getMediaId() != null && !mediaPublisherRepository.existsMediaId(entity.getMediaId())) {
            errors.add(new FieldError("mediaId", "media does not exist", "NOT_FOUND"));
        }
        if (entity.getPublisherId() != null && !mediaPublisherRepository.existsPublisherId(entity.getPublisherId())) {
            errors.add(new FieldError("publisherId", "publisher does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueMediaPublisher(MediaPublisherEntity entity) {
        if (mediaPublisherRepository.takenMediaIdPublisherId1(entity)) {
            throw new DuplicateResourceException(
                    "A media publisher with the same media id and publisher id already exists.");
        }
    }

    // ---- MediaSubject ----

    public MediaSubject getMediaSubject(UUID mediaId, UUID id) {
        return doInReadTransaction(() -> {
            return mediaSubjectMapper.toDto(loadVisibleMediaSubject(DataScope.current(MEDIA_SUBJECT_PERMISSION_GET), mediaId,
                    id));
        });
    }

    public Page<MediaSubject> listMediaSubjects(UUID mediaId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, MediaSubjectRepository.SORT_FIELDS, MediaSubjectRepository.DEFAULT_ORDER,
                MediaSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MediaSubjectRepository.FILTER_FIELDS)
                .and(MediaSubjectRepository.ofParent(mediaId))
                        .and(MediaSubjectRepository.scope(DataScope.current(MEDIA_SUBJECT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + mediaId, clock);
        return doInReadTransaction(() -> {
            Page<MediaSubjectEntity> rows = mediaSubjectRepository.list(page, where);
            return rows.map(mediaSubjectMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public MediaSubject createMediaSubject(UUID mediaId, MediaSubject request, boolean validateOnly) {
        validator.validate(request);
        MediaSubjectEntity entity = mediaSubjectMapper.toEntity(request);
        entity.setMediaId(mediaId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(MEDIA_SUBJECT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!mediaSubjectRepository.existsMediaId(entity.getMediaId())) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            if (!scope.canRead(institutionOfMediaSubject(entity))) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            assertValidMediaSubject(entity, true);
            scope.requireWritable(institutionOfMediaSubject(entity));
            assertUniqueMediaSubject(entity);
            if (validateOnly) {
                return mediaSubjectMapper.toDto(entity);
            }
            MediaSubjectEntity stored = mediaSubjectRepository.insert(entity);
            audit.recordCreate(MEDIA_SUBJECT_AGGREGATE, stored.getId());
            outbox.publish(MEDIA_SUBJECT_AGGREGATE + LibraryEvents.CREATED, MEDIA_SUBJECT_AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mediaSubjectMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public MediaSubject updateMediaSubject(UUID mediaId, UUID id, String ifMatch, MediaSubjectUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_SUBJECT_PERMISSION_UPDATE);
            MediaSubjectEntity entity = loadVisibleMediaSubject(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyMediaSubject(entity, changes);
            validator.validate(mediaSubjectMapper.toDto(entity));
            assertValidMediaSubject(entity, false);
            scope.requireWritable(institutionOfMediaSubject(entity));
            assertUniqueMediaSubject(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mediaSubjectMapper.toDto(entity);
            }
            if (mediaSubjectRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(MEDIA_SUBJECT_AGGREGATE, id, fields);
            outbox.publish(MEDIA_SUBJECT_AGGREGATE + LibraryEvents.UPDATED, MEDIA_SUBJECT_AGGREGATE, id,
                    LibraryEvents.payload(id));
            return mediaSubjectMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMediaSubject(UUID mediaId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_SUBJECT_PERMISSION_DELETE);
            MediaSubjectEntity entity = loadVisibleMediaSubject(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (mediaSubjectRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(MEDIA_SUBJECT_AGGREGATE, id);
            outbox.publish(MEDIA_SUBJECT_AGGREGATE + LibraryEvents.DELETED, MEDIA_SUBJECT_AGGREGATE, id,
                    LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MediaSubjectEntity loadVisibleMediaSubject(DataScope scope, UUID mediaId, UUID id) {
        MediaSubjectEntity entity = mediaSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDIA_SUBJECT_AGGREGATE, id));
        if (!scope.canRead(institutionOfMediaSubject(entity)) || !mediaId.equals(entity.getMediaId())) {
            throw new ResourceNotFoundException(MEDIA_SUBJECT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the library.media the row belongs to. */
    private Optional<UUID> institutionOfMediaSubject(MediaSubjectEntity entity) {
        return mediaSubjectRepository.institutionOf(entity.getMediaId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyMediaSubject(MediaSubjectEntity entity, MediaSubjectUpdate changes) {
        Patches patch = new Patches();
        patch.apply("subjectId", changes.getSubjectId(), entity::setSubjectId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidMediaSubject(MediaSubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMediaId() == null) {
            errors.add(new FieldError("mediaId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getMediaId() != null && !mediaSubjectRepository.existsMediaId(entity.getMediaId())) {
            errors.add(new FieldError("mediaId", "media does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueMediaSubject(MediaSubjectEntity entity) {
        if (mediaSubjectRepository.takenMediaIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A media subject with the same media id and subject id already exists.");
        }
    }

    // ---- MediaUnit ----

    public MediaUnit getMediaUnit(UUID mediaId, UUID id) {
        return doInReadTransaction(() -> {
            return mediaUnitMapper.toDto(loadVisibleMediaUnit(DataScope.current(MEDIA_UNIT_PERMISSION_GET), mediaId, id));
        });
    }

    public Page<MediaUnit> listMediaUnits(UUID mediaId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, MediaUnitRepository.SORT_FIELDS, MediaUnitRepository.DEFAULT_ORDER,
                MediaUnitRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MediaUnitRepository.FILTER_FIELDS)
                .and(MediaUnitRepository.ofParent(mediaId))
                        .and(MediaUnitRepository.scope(DataScope.current(MEDIA_UNIT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + mediaId, clock);
        return doInReadTransaction(() -> {
            Page<MediaUnitEntity> rows = mediaUnitRepository.list(page, where);
            return rows.map(mediaUnitMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public MediaUnit createMediaUnit(UUID mediaId, MediaUnit request, boolean validateOnly) {
        validator.validate(request);
        MediaUnitEntity entity = mediaUnitMapper.toEntity(request);
        entity.setMediaId(mediaId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(MEDIA_UNIT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!mediaUnitRepository.existsMediaId(entity.getMediaId())) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            if (!scope.canRead(institutionOfMediaUnit(entity))) {
                throw new ResourceNotFoundException("library.media", entity.getMediaId());
            }
            assertValidMediaUnit(entity, true);
            scope.requireWritable(institutionOfMediaUnit(entity));
            assertUniqueMediaUnit(entity);
            if (validateOnly) {
                return mediaUnitMapper.toDto(entity);
            }
            MediaUnitEntity stored = mediaUnitRepository.insert(entity);
            audit.recordCreate(MEDIA_UNIT_AGGREGATE, stored.getId());
            outbox.publish(MEDIA_UNIT_AGGREGATE + LibraryEvents.CREATED, MEDIA_UNIT_AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mediaUnitMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public MediaUnit updateMediaUnit(UUID mediaId, UUID id, String ifMatch, MediaUnitUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_UNIT_PERMISSION_UPDATE);
            MediaUnitEntity entity = loadVisibleMediaUnit(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaUnit(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyMediaUnit(entity, changes);
            validator.validate(mediaUnitMapper.toDto(entity));
            assertValidMediaUnit(entity, false);
            scope.requireWritable(institutionOfMediaUnit(entity));
            assertUniqueMediaUnit(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mediaUnitMapper.toDto(entity);
            }
            if (mediaUnitRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(MEDIA_UNIT_AGGREGATE, id, fields);
            outbox.publish(MEDIA_UNIT_AGGREGATE + LibraryEvents.UPDATED, MEDIA_UNIT_AGGREGATE, id, LibraryEvents.payload(id));
            return mediaUnitMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMediaUnit(UUID mediaId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(MEDIA_UNIT_PERMISSION_DELETE);
            MediaUnitEntity entity = loadVisibleMediaUnit(scope, mediaId, id);
            scope.requireWritable(institutionOfMediaUnit(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (mediaUnitRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(MEDIA_UNIT_AGGREGATE, id);
            outbox.publish(MEDIA_UNIT_AGGREGATE + LibraryEvents.DELETED, MEDIA_UNIT_AGGREGATE, id, LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MediaUnitEntity loadVisibleMediaUnit(DataScope scope, UUID mediaId, UUID id) {
        MediaUnitEntity entity = mediaUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MEDIA_UNIT_AGGREGATE, id));
        if (!scope.canRead(institutionOfMediaUnit(entity)) || !mediaId.equals(entity.getMediaId())) {
            throw new ResourceNotFoundException(MEDIA_UNIT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the library.media the row belongs to. */
    private Optional<UUID> institutionOfMediaUnit(MediaUnitEntity entity) {
        return mediaUnitRepository.institutionOf(entity.getMediaId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyMediaUnit(MediaUnitEntity entity, MediaUnitUpdate changes) {
        Patches patch = new Patches();
        patch.apply("accessionNumber", changes.getAccessionNumber(), entity::setAccessionNumber);
        patch.apply("barcode", changes.getBarcode(), entity::setBarcode);
        patch.apply("status", changes.getStatus(), v -> entity.setStatus(v == null ? null : v.getValue()));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidMediaUnit(MediaUnitEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMediaId() == null) {
            errors.add(new FieldError("mediaId", "must not be null", "NotNull"));
        }
        if (entity.getAccessionNumber() == null) {
            errors.add(new FieldError("accessionNumber", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getMediaId() != null && !mediaUnitRepository.existsMediaId(entity.getMediaId())) {
            errors.add(new FieldError("mediaId", "media does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueMediaUnit(MediaUnitEntity entity) {
        if (mediaUnitRepository.takenMediaIdAccessionNumber1(entity)) {
            throw new DuplicateResourceException(
                    "A media unit with the same media id and accession number already exists.");
        }
    }
}
