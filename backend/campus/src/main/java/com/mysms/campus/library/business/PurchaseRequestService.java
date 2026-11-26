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
import com.mysms.campus.library.api.model.AcceptPurchaseRequestRequest;
import com.mysms.campus.library.api.model.PurchaseRequest;
import com.mysms.campus.library.api.model.PurchaseRequestCourse;
import com.mysms.campus.library.api.model.PurchaseRequestCourseUpdate;
import com.mysms.campus.library.api.model.PurchaseRequestSubject;
import com.mysms.campus.library.api.model.PurchaseRequestSubjectUpdate;
import com.mysms.campus.library.api.model.PurchaseRequestUpdate;
import com.mysms.campus.library.api.model.RejectPurchaseRequestRequest;
import com.mysms.campus.library.api.model.SubmitPurchaseRequestRequest;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.config.LibraryEventsConfig;
import com.mysms.campus.library.domain.PurchaseRequestCourseEntity;
import com.mysms.campus.library.domain.PurchaseRequestEntity;
import com.mysms.campus.library.domain.PurchaseRequestSubjectEntity;
import com.mysms.campus.library.events.LibraryEvents;
import com.mysms.campus.library.mapper.PurchaseRequestCourseMapper;
import com.mysms.campus.library.mapper.PurchaseRequestMapper;
import com.mysms.campus.library.mapper.PurchaseRequestSubjectMapper;
import com.mysms.campus.library.repository.PurchaseRequestCourseRepository;
import com.mysms.campus.library.repository.PurchaseRequestRepository;
import com.mysms.campus.library.repository.PurchaseRequestSubjectRepository;
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
 * Business service of library.purchase_request and of the records that exist only as part of a PurchaseRequest
 * (PurchaseRequestCourse, PurchaseRequestSubject): audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class PurchaseRequestService extends AppTransactionManager {

    public static final String AGGREGATE = PurchaseRequestEntity.TABLE;
    static final String PERMISSION_LIST = "library.purchase_request.read";
    static final String PERMISSION_GET = "library.purchase_request.read";
    static final String PERMISSION_CREATE = "library.purchase_request.create";
    static final String PERMISSION_UPDATE = "library.purchase_request.update";
    static final String PERMISSION_DELETE = "library.purchase_request.delete";
    static final String PERMISSION_SUBMIT_PURCHASE_REQUEST = "library.purchase_request.submit_purchase_request";
    static final String PERMISSION_ACCEPT_PURCHASE_REQUEST = "library.purchase_request.accept_purchase_request";
    static final String PERMISSION_REJECT_PURCHASE_REQUEST = "library.purchase_request.reject_purchase_request";

    public static final String PURCHASE_REQUEST_COURSE_AGGREGATE = PurchaseRequestCourseEntity.TABLE;
    static final String PURCHASE_REQUEST_COURSE_PERMISSION_LIST = "library.purchase_request_course.read";
    static final String PURCHASE_REQUEST_COURSE_PERMISSION_GET = "library.purchase_request_course.read";
    static final String PURCHASE_REQUEST_COURSE_PERMISSION_CREATE = "library.purchase_request_course.create";
    static final String PURCHASE_REQUEST_COURSE_PERMISSION_UPDATE = "library.purchase_request_course.update";
    static final String PURCHASE_REQUEST_COURSE_PERMISSION_DELETE = "library.purchase_request_course.delete";

    public static final String PURCHASE_REQUEST_SUBJECT_AGGREGATE = PurchaseRequestSubjectEntity.TABLE;
    static final String PURCHASE_REQUEST_SUBJECT_PERMISSION_LIST = "library.purchase_request_subject.read";
    static final String PURCHASE_REQUEST_SUBJECT_PERMISSION_GET = "library.purchase_request_subject.read";
    static final String PURCHASE_REQUEST_SUBJECT_PERMISSION_CREATE = "library.purchase_request_subject.create";
    static final String PURCHASE_REQUEST_SUBJECT_PERMISSION_UPDATE = "library.purchase_request_subject.update";
    static final String PURCHASE_REQUEST_SUBJECT_PERMISSION_DELETE = "library.purchase_request_subject.delete";

    private final PurchaseRequestRepository repository;
    private final PurchaseRequestMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final PurchaseRequestCourseRepository purchaseRequestCourseRepository;
    private final PurchaseRequestCourseMapper purchaseRequestCourseMapper;
    private final PurchaseRequestSubjectRepository purchaseRequestSubjectRepository;
    private final PurchaseRequestSubjectMapper purchaseRequestSubjectMapper;

    public PurchaseRequestService(PurchaseRequestRepository repository,
            PurchaseRequestMapper mapper,
            @Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LibraryDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LibraryEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            PurchaseRequestCourseRepository purchaseRequestCourseRepository,
            PurchaseRequestCourseMapper purchaseRequestCourseMapper,
            PurchaseRequestSubjectRepository purchaseRequestSubjectRepository,
            PurchaseRequestSubjectMapper purchaseRequestSubjectMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.purchaseRequestCourseRepository = purchaseRequestCourseRepository;
        this.purchaseRequestCourseMapper = purchaseRequestCourseMapper;
        this.purchaseRequestSubjectRepository = purchaseRequestSubjectRepository;
        this.purchaseRequestSubjectMapper = purchaseRequestSubjectMapper;
    }

    public PurchaseRequest getPurchaseRequest(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<PurchaseRequest> listPurchaseRequests(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PurchaseRequestRepository.SORT_FIELDS, PurchaseRequestRepository.DEFAULT_ORDER,
                PurchaseRequestRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PurchaseRequestRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PurchaseRequestEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PurchaseRequest createPurchaseRequest(PurchaseRequest request, boolean validateOnly) {
        validator.validate(request);
        PurchaseRequestEntity entity = mapper.toEntity(request);
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
            PurchaseRequestEntity stored = repository.insert(entity);
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
    public PurchaseRequest updatePurchaseRequest(UUID id, String ifMatch, PurchaseRequestUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PurchaseRequestEntity entity = loadVisible(scope, id);
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
    public void deletePurchaseRequest(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PurchaseRequestEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByPurchaseRequestCoursePurchaseRequestId(id)) {
                throw new InvalidStateException(
                        "The purchase request is still referenced by library.purchase_request_course rows; remove "
                                + "those first.");
            }
            if (repository.referencedByPurchaseRequestSubjectPurchaseRequestId(id)) {
                throw new InvalidStateException(
                        "The purchase request is still referenced by library.purchase_request_subject rows; remove "
                                + "those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LibraryEvents.DELETED, AGGREGATE, id, LibraryEvents.payload(id));
        });
    }

    /**
     * library.SubmitPurchaseRequest: Send to the librarian. Allowed while status is draft, on a row the caller may see
     * and write; otherwise 409 and nothing changes.
     */
    public PurchaseRequest submitPurchaseRequest(UUID id, SubmitPurchaseRequestRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SUBMIT_PURCHASE_REQUEST);
        return doInWriteTransaction(() -> {
            PurchaseRequestEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The purchase request cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.submitPurchaseRequest(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "submit", List.of("status"), null);
            outbox.publish(AGGREGATE + ".submit", AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * library.AcceptPurchaseRequest: Accept. Allowed while status is requested, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public PurchaseRequest acceptPurchaseRequest(UUID id, AcceptPurchaseRequestRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ACCEPT_PURCHASE_REQUEST);
        return doInWriteTransaction(() -> {
            PurchaseRequestEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("requested").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The purchase request cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.acceptPurchaseRequest(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "accept", List.of("status"), null);
            outbox.publish(AGGREGATE + ".accept", AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * library.RejectPurchaseRequest: Reject. Allowed while status is requested, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public PurchaseRequest rejectPurchaseRequest(UUID id, RejectPurchaseRequestRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_PURCHASE_REQUEST);
        return doInWriteTransaction(() -> {
            PurchaseRequestEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("requested").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The purchase request cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectPurchaseRequest(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reject", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PurchaseRequestEntity loadVisible(DataScope scope, UUID id) {
        PurchaseRequestEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(PurchaseRequestEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(PurchaseRequestEntity entity, PurchaseRequestUpdate changes) {
        Patches patch = new Patches();
        patch.apply("requestNumber", changes.getRequestNumber(), entity::setRequestNumber);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("authorText", changes.getAuthorText(), entity::setAuthorText);
        patch.apply("edition", changes.getEdition(), entity::setEdition);
        patch.apply("publisherText", changes.getPublisherText(), entity::setPublisherText);
        patch.apply("mediaTypeId", changes.getMediaTypeId(), entity::setMediaTypeId);
        patch.apply("requestedByPersonId", changes.getRequestedByPersonId(), entity::setRequestedByPersonId);
        patch.apply("requestDate", changes.getRequestDate(), entity::setRequestDate);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PurchaseRequestEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getAuthorText() == null) {
            errors.add(new FieldError("authorText", "must not be null", "NotNull"));
        }
        if (entity.getMediaTypeId() != null && !repository.existsMediaTypeId(entity.getMediaTypeId())) {
            errors.add(new FieldError("mediaTypeId", "media type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PurchaseRequestEntity entity) {

    }

    // ---- PurchaseRequestCourse ----

    public PurchaseRequestCourse getPurchaseRequestCourse(UUID purchaseRequestId, UUID id) {
        return doInReadTransaction(() -> {
            return purchaseRequestCourseMapper.toDto(loadVisiblePurchaseRequestCourse(DataScope.current(
                    PURCHASE_REQUEST_COURSE_PERMISSION_GET), purchaseRequestId, id));
        });
    }

    public Page<PurchaseRequestCourse> listPurchaseRequestCourses(UUID purchaseRequestId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PurchaseRequestCourseRepository.SORT_FIELDS,
                        PurchaseRequestCourseRepository.DEFAULT_ORDER,
                PurchaseRequestCourseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PurchaseRequestCourseRepository.FILTER_FIELDS)
                .and(PurchaseRequestCourseRepository.ofParent(purchaseRequestId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + purchaseRequestId, clock);
        return doInReadTransaction(() -> {
            Page<PurchaseRequestCourseEntity> rows = purchaseRequestCourseRepository.list(page, where);
            return rows.map(purchaseRequestCourseMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PurchaseRequestCourse createPurchaseRequestCourse(UUID purchaseRequestId, PurchaseRequestCourse request,
            boolean validateOnly) {
        validator.validate(request);
        PurchaseRequestCourseEntity entity = purchaseRequestCourseMapper.toEntity(request);
        entity.setPurchaseRequestId(purchaseRequestId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PURCHASE_REQUEST_COURSE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!purchaseRequestCourseRepository.existsPurchaseRequestId(entity.getPurchaseRequestId())) {
                throw new ResourceNotFoundException("library.purchase_request", entity.getPurchaseRequestId());
            }
            assertValidPurchaseRequestCourse(entity, true);
            scope.requireWritable(institutionOfPurchaseRequestCourse(entity));
            assertUniquePurchaseRequestCourse(entity);
            if (validateOnly) {
                return purchaseRequestCourseMapper.toDto(entity);
            }
            PurchaseRequestCourseEntity stored = purchaseRequestCourseRepository.insert(entity);
            audit.recordCreate(PURCHASE_REQUEST_COURSE_AGGREGATE, stored.getId());
            outbox.publish(PURCHASE_REQUEST_COURSE_AGGREGATE + LibraryEvents.CREATED, PURCHASE_REQUEST_COURSE_AGGREGATE,
                    stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return purchaseRequestCourseMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public PurchaseRequestCourse updatePurchaseRequestCourse(UUID purchaseRequestId, UUID id, String ifMatch,
            PurchaseRequestCourseUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PURCHASE_REQUEST_COURSE_PERMISSION_UPDATE);
            PurchaseRequestCourseEntity entity = loadVisiblePurchaseRequestCourse(scope, purchaseRequestId, id);
            scope.requireWritable(institutionOfPurchaseRequestCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyPurchaseRequestCourse(entity, changes);
            validator.validate(purchaseRequestCourseMapper.toDto(entity));
            assertValidPurchaseRequestCourse(entity, false);
            scope.requireWritable(institutionOfPurchaseRequestCourse(entity));
            assertUniquePurchaseRequestCourse(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return purchaseRequestCourseMapper.toDto(entity);
            }
            if (purchaseRequestCourseRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PURCHASE_REQUEST_COURSE_AGGREGATE, id, fields);
            outbox.publish(PURCHASE_REQUEST_COURSE_AGGREGATE + LibraryEvents.UPDATED, PURCHASE_REQUEST_COURSE_AGGREGATE, id,
                    LibraryEvents.payload(id));
            return purchaseRequestCourseMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePurchaseRequestCourse(UUID purchaseRequestId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PURCHASE_REQUEST_COURSE_PERMISSION_DELETE);
            PurchaseRequestCourseEntity entity = loadVisiblePurchaseRequestCourse(scope, purchaseRequestId, id);
            scope.requireWritable(institutionOfPurchaseRequestCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (purchaseRequestCourseRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PURCHASE_REQUEST_COURSE_AGGREGATE, id);
            outbox.publish(PURCHASE_REQUEST_COURSE_AGGREGATE + LibraryEvents.DELETED, PURCHASE_REQUEST_COURSE_AGGREGATE, id,
                    LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PurchaseRequestCourseEntity loadVisiblePurchaseRequestCourse(DataScope scope, UUID purchaseRequestId, UUID id) {
        PurchaseRequestCourseEntity entity = purchaseRequestCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PURCHASE_REQUEST_COURSE_AGGREGATE, id));
        if (!scope.canRead(institutionOfPurchaseRequestCourse(entity))
                || !purchaseRequestId.equals(entity.getPurchaseRequestId())) {
            throw new ResourceNotFoundException(PURCHASE_REQUEST_COURSE_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfPurchaseRequestCourse(PurchaseRequestCourseEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyPurchaseRequestCourse(PurchaseRequestCourseEntity entity,
            PurchaseRequestCourseUpdate changes) {
        Patches patch = new Patches();
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPurchaseRequestCourse(PurchaseRequestCourseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPurchaseRequestId() == null) {
            errors.add(new FieldError("purchaseRequestId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getPurchaseRequestId() != null
                && !purchaseRequestCourseRepository.existsPurchaseRequestId(entity.getPurchaseRequestId())) {
            errors.add(new FieldError("purchaseRequestId", "purchase request does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePurchaseRequestCourse(PurchaseRequestCourseEntity entity) {
        if (purchaseRequestCourseRepository.takenPurchaseRequestIdCourseId1(entity)) {
            throw new DuplicateResourceException(
                    "A purchase request course with the same purchase request id and course id already exists.");
        }
    }

    // ---- PurchaseRequestSubject ----

    public PurchaseRequestSubject getPurchaseRequestSubject(UUID purchaseRequestId, UUID id) {
        return doInReadTransaction(() -> {
            return purchaseRequestSubjectMapper.toDto(loadVisiblePurchaseRequestSubject(DataScope.current(
                    PURCHASE_REQUEST_SUBJECT_PERMISSION_GET), purchaseRequestId, id));
        });
    }

    public Page<PurchaseRequestSubject> listPurchaseRequestSubjects(UUID purchaseRequestId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PurchaseRequestSubjectRepository.SORT_FIELDS,
                        PurchaseRequestSubjectRepository.DEFAULT_ORDER,
                PurchaseRequestSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PurchaseRequestSubjectRepository.FILTER_FIELDS)
                .and(PurchaseRequestSubjectRepository.ofParent(purchaseRequestId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + purchaseRequestId, clock);
        return doInReadTransaction(() -> {
            Page<PurchaseRequestSubjectEntity> rows = purchaseRequestSubjectRepository.list(page, where);
            return rows.map(purchaseRequestSubjectMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PurchaseRequestSubject createPurchaseRequestSubject(UUID purchaseRequestId, PurchaseRequestSubject request,
            boolean validateOnly) {
        validator.validate(request);
        PurchaseRequestSubjectEntity entity = purchaseRequestSubjectMapper.toEntity(request);
        entity.setPurchaseRequestId(purchaseRequestId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PURCHASE_REQUEST_SUBJECT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!purchaseRequestSubjectRepository.existsPurchaseRequestId(entity.getPurchaseRequestId())) {
                throw new ResourceNotFoundException("library.purchase_request", entity.getPurchaseRequestId());
            }
            assertValidPurchaseRequestSubject(entity, true);
            scope.requireWritable(institutionOfPurchaseRequestSubject(entity));
            assertUniquePurchaseRequestSubject(entity);
            if (validateOnly) {
                return purchaseRequestSubjectMapper.toDto(entity);
            }
            PurchaseRequestSubjectEntity stored = purchaseRequestSubjectRepository.insert(entity);
            audit.recordCreate(PURCHASE_REQUEST_SUBJECT_AGGREGATE, stored.getId());
            outbox.publish(PURCHASE_REQUEST_SUBJECT_AGGREGATE + LibraryEvents.CREATED, PURCHASE_REQUEST_SUBJECT_AGGREGATE,
                    stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return purchaseRequestSubjectMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public PurchaseRequestSubject updatePurchaseRequestSubject(UUID purchaseRequestId, UUID id, String ifMatch,
            PurchaseRequestSubjectUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PURCHASE_REQUEST_SUBJECT_PERMISSION_UPDATE);
            PurchaseRequestSubjectEntity entity = loadVisiblePurchaseRequestSubject(scope, purchaseRequestId, id);
            scope.requireWritable(institutionOfPurchaseRequestSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyPurchaseRequestSubject(entity, changes);
            validator.validate(purchaseRequestSubjectMapper.toDto(entity));
            assertValidPurchaseRequestSubject(entity, false);
            scope.requireWritable(institutionOfPurchaseRequestSubject(entity));
            assertUniquePurchaseRequestSubject(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return purchaseRequestSubjectMapper.toDto(entity);
            }
            if (purchaseRequestSubjectRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PURCHASE_REQUEST_SUBJECT_AGGREGATE, id, fields);
            outbox.publish(PURCHASE_REQUEST_SUBJECT_AGGREGATE + LibraryEvents.UPDATED, PURCHASE_REQUEST_SUBJECT_AGGREGATE,
                    id, LibraryEvents.payload(id));
            return purchaseRequestSubjectMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePurchaseRequestSubject(UUID purchaseRequestId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PURCHASE_REQUEST_SUBJECT_PERMISSION_DELETE);
            PurchaseRequestSubjectEntity entity = loadVisiblePurchaseRequestSubject(scope, purchaseRequestId, id);
            scope.requireWritable(institutionOfPurchaseRequestSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (purchaseRequestSubjectRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PURCHASE_REQUEST_SUBJECT_AGGREGATE, id);
            outbox.publish(PURCHASE_REQUEST_SUBJECT_AGGREGATE + LibraryEvents.DELETED, PURCHASE_REQUEST_SUBJECT_AGGREGATE,
                    id, LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PurchaseRequestSubjectEntity loadVisiblePurchaseRequestSubject(DataScope scope, UUID purchaseRequestId, UUID id) {
        PurchaseRequestSubjectEntity entity = purchaseRequestSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PURCHASE_REQUEST_SUBJECT_AGGREGATE, id));
        if (!scope.canRead(institutionOfPurchaseRequestSubject(entity))
                || !purchaseRequestId.equals(entity.getPurchaseRequestId())) {
            throw new ResourceNotFoundException(PURCHASE_REQUEST_SUBJECT_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfPurchaseRequestSubject(PurchaseRequestSubjectEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyPurchaseRequestSubject(PurchaseRequestSubjectEntity entity,
            PurchaseRequestSubjectUpdate changes) {
        Patches patch = new Patches();
        patch.apply("subjectId", changes.getSubjectId(), entity::setSubjectId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPurchaseRequestSubject(PurchaseRequestSubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPurchaseRequestId() == null) {
            errors.add(new FieldError("purchaseRequestId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getPurchaseRequestId() != null
                && !purchaseRequestSubjectRepository.existsPurchaseRequestId(entity.getPurchaseRequestId())) {
            errors.add(new FieldError("purchaseRequestId", "purchase request does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePurchaseRequestSubject(PurchaseRequestSubjectEntity entity) {
        if (purchaseRequestSubjectRepository.takenPurchaseRequestIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A purchase request subject with the same purchase request id and subject id already exists.");
        }
    }
}
