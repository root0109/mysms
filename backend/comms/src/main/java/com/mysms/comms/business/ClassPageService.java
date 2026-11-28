package com.mysms.comms.business;

import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.comms.api.model.FollowClassPageRequest;
import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.ClassPage;
import com.mysms.comms.api.model.ClassPageAttachment;
import com.mysms.comms.api.model.ClassPageAttachmentUpdate;
import com.mysms.comms.api.model.ClassPageMember;
import com.mysms.comms.api.model.ClassPageMemberUpdate;
import com.mysms.comms.api.model.ClassPageUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.ClassPageAttachmentEntity;
import com.mysms.comms.domain.ClassPageEntity;
import com.mysms.comms.domain.ClassPageMemberEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.ClassPageAttachmentMapper;
import com.mysms.comms.mapper.ClassPageMapper;
import com.mysms.comms.mapper.ClassPageMemberMapper;
import com.mysms.comms.repository.ClassPageAttachmentRepository;
import com.mysms.comms.repository.ClassPageMemberRepository;
import com.mysms.comms.repository.ClassPageRepository;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.class_page and of the records that exist only as part of a ClassPage (ClassPageAttachment,
 * ClassPageMember): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ClassPageService extends AppTransactionManager {

    public static final String AGGREGATE = ClassPageEntity.TABLE;
    static final String PERMISSION_LIST = "comms.class_page.read";
    static final String PERMISSION_GET = "comms.class_page.read";
    static final String PERMISSION_CREATE = "comms.class_page.create";
    static final String PERMISSION_UPDATE = "comms.class_page.update";
    static final String PERMISSION_DELETE = "comms.class_page.delete";
    static final String PERMISSION_FOLLOW_CLASS_PAGE = "comms.class_page.follow_class_page";

    public static final String CLASS_PAGE_ATTACHMENT_AGGREGATE = ClassPageAttachmentEntity.TABLE;
    static final String CLASS_PAGE_ATTACHMENT_PERMISSION_LIST = "comms.class_page_attachment.read";
    static final String CLASS_PAGE_ATTACHMENT_PERMISSION_GET = "comms.class_page_attachment.read";
    static final String CLASS_PAGE_ATTACHMENT_PERMISSION_CREATE = "comms.class_page_attachment.create";
    static final String CLASS_PAGE_ATTACHMENT_PERMISSION_UPDATE = "comms.class_page_attachment.update";
    static final String CLASS_PAGE_ATTACHMENT_PERMISSION_DELETE = "comms.class_page_attachment.delete";

    public static final String CLASS_PAGE_MEMBER_AGGREGATE = ClassPageMemberEntity.TABLE;
    static final String CLASS_PAGE_MEMBER_PERMISSION_LIST = "comms.class_page_member.read";
    static final String CLASS_PAGE_MEMBER_PERMISSION_GET = "comms.class_page_member.read";
    static final String CLASS_PAGE_MEMBER_PERMISSION_CREATE = "comms.class_page_member.create";
    static final String CLASS_PAGE_MEMBER_PERMISSION_UPDATE = "comms.class_page_member.update";
    static final String CLASS_PAGE_MEMBER_PERMISSION_DELETE = "comms.class_page_member.delete";

    private final ClassPageRepository repository;
    private final ClassPageMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ClassPageAttachmentRepository classPageAttachmentRepository;
    private final ClassPageAttachmentMapper classPageAttachmentMapper;
    private final ClassPageMemberRepository classPageMemberRepository;
    private final ClassPageMemberMapper classPageMemberMapper;

    public ClassPageService(ClassPageRepository repository,
            ClassPageMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ClassPageAttachmentRepository classPageAttachmentRepository,
            ClassPageAttachmentMapper classPageAttachmentMapper,
            ClassPageMemberRepository classPageMemberRepository,
            ClassPageMemberMapper classPageMemberMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.classPageAttachmentRepository = classPageAttachmentRepository;
        this.classPageAttachmentMapper = classPageAttachmentMapper;
        this.classPageMemberRepository = classPageMemberRepository;
        this.classPageMemberMapper = classPageMemberMapper;
    }

    public ClassPage getClassPage(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ClassPage> listClassPages(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ClassPageRepository.SORT_FIELDS, ClassPageRepository.DEFAULT_ORDER,
                ClassPageRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ClassPageRepository.FILTER_FIELDS)
                .and(ClassPageRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ClassPageEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ClassPage createClassPage(ClassPage request, boolean validateOnly) {
        validator.validate(request);
        ClassPageEntity entity = mapper.toEntity(request);
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
            ClassPageEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public ClassPage updateClassPage(UUID id, String ifMatch, ClassPageUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ClassPageEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
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
    public void deleteClassPage(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ClassPageEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByClassPageAttachmentClassPageId(id)) {
                throw new InvalidStateException(
                        "The class page is still referenced by comms.class_page_attachment rows; remove those first.");
            }
            if (repository.referencedByClassPageMemberClassPageId(id)) {
                throw new InvalidStateException(
                        "The class page is still referenced by comms.class_page_member rows; remove those first.");
            }
            if (repository.referencedByPostClassPageId(id)) {
                throw new InvalidStateException(
                        "The class page is still referenced by comms.post rows; remove those first.");
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
     * comms.FollowClassPage: a member sets whether they follow the page. Following is a preference of an existing
     * member, never a way in: the page of someone who is not an active member of it is 404, like an absent page.
     */
    public ClassPageMember followClassPage(UUID id, FollowClassPageRequest request) {
        DataScope scope = DataScope.current(PERMISSION_FOLLOW_CLASS_PAGE);
        UUID caller = OwnRows.caller();
        boolean follow = Boolean.TRUE.equals(request.getFollow());
        return doInWriteTransaction(() -> {
            ClassPageEntity page = loadVisible(scope, id);
            ClassPageMemberEntity member = Boolean.TRUE.equals(page.getIsActive())
                    ? classPageMemberRepository.findMembership(id, caller).orElse(null) : null;
            if (member == null) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            if (Boolean.valueOf(follow).equals(member.getIsFollowing())) {
                return classPageMemberMapper.toDto(member);
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (classPageMemberRepository.setFollowing(member.getId(), member.getRowVersion(), follow, now,
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The membership changed since it was read.");
            }
            String step = follow ? "follow" : "unfollow";
            audit.recordTransition(CLASS_PAGE_MEMBER_AGGREGATE, member.getId(), step, List.of("isFollowing"), null);
            outbox.publish(CLASS_PAGE_MEMBER_AGGREGATE + "." + step, CLASS_PAGE_MEMBER_AGGREGATE, member.getId(),
                    CommsEvents.payload(member.getId()));
            return classPageMemberMapper.toDto(classPageMemberRepository.findById(member.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(CLASS_PAGE_MEMBER_AGGREGATE, member.getId())));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ClassPageEntity loadVisible(DataScope scope, UUID id) {
        ClassPageEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(ClassPageEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(ClassPageEntity entity, ClassPageUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("pageCategoryId", changes.getPageCategoryId(), entity::setPageCategoryId);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("coverImageFileId", changes.getCoverImageFileId(), entity::setCoverImageFileId);
        patch.apply("ownerUserId", changes.getOwnerUserId(), entity::setOwnerUserId);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ClassPageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getPageCategoryId() == null) {
            errors.add(new FieldError("pageCategoryId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getDescription() == null) {
            errors.add(new FieldError("description", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getPageCategoryId() != null && !repository.existsPageCategoryId(entity.getPageCategoryId())) {
            errors.add(new FieldError("pageCategoryId", "page category does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ClassPageEntity entity) {

    }

    // ---- ClassPageAttachment ----

    public ClassPageAttachment getClassPageAttachment(UUID classPageId, UUID id) {
        return doInReadTransaction(() -> {
            return classPageAttachmentMapper.toDto(loadVisibleClassPageAttachment(DataScope.current(
                    CLASS_PAGE_ATTACHMENT_PERMISSION_GET), classPageId, id));
        });
    }

    public Page<ClassPageAttachment> listClassPageAttachments(UUID classPageId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ClassPageAttachmentRepository.SORT_FIELDS,
                        ClassPageAttachmentRepository.DEFAULT_ORDER,
                ClassPageAttachmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ClassPageAttachmentRepository.FILTER_FIELDS)
                .and(ClassPageAttachmentRepository.ofParent(classPageId))
                        .and(ClassPageAttachmentRepository.scope(DataScope.current(CLASS_PAGE_ATTACHMENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + classPageId, clock);
        return doInReadTransaction(() -> {
            Page<ClassPageAttachmentEntity> rows = classPageAttachmentRepository.list(page, where);
            return rows.map(classPageAttachmentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ClassPageAttachment createClassPageAttachment(UUID classPageId, ClassPageAttachment request,
            boolean validateOnly) {
        validator.validate(request);
        ClassPageAttachmentEntity entity = classPageAttachmentMapper.toEntity(request);
        entity.setClassPageId(classPageId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CLASS_PAGE_ATTACHMENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!classPageAttachmentRepository.existsClassPageId(entity.getClassPageId())) {
                throw new ResourceNotFoundException("comms.class_page", entity.getClassPageId());
            }
            if (!scope.canRead(institutionOfClassPageAttachment(entity))) {
                throw new ResourceNotFoundException("comms.class_page", entity.getClassPageId());
            }
            assertValidClassPageAttachment(entity, true);
            scope.requireWritable(institutionOfClassPageAttachment(entity));
            assertUniqueClassPageAttachment(entity);
            if (validateOnly) {
                return classPageAttachmentMapper.toDto(entity);
            }
            ClassPageAttachmentEntity stored = classPageAttachmentRepository.insert(entity);
            audit.recordCreate(CLASS_PAGE_ATTACHMENT_AGGREGATE, stored.getId());
            outbox.publish(CLASS_PAGE_ATTACHMENT_AGGREGATE + CommsEvents.CREATED, CLASS_PAGE_ATTACHMENT_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return classPageAttachmentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public ClassPageAttachment updateClassPageAttachment(UUID classPageId, UUID id, String ifMatch,
            ClassPageAttachmentUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CLASS_PAGE_ATTACHMENT_PERMISSION_UPDATE);
            ClassPageAttachmentEntity entity = loadVisibleClassPageAttachment(scope, classPageId, id);
            scope.requireWritable(institutionOfClassPageAttachment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyClassPageAttachment(entity, changes);
            validator.validate(classPageAttachmentMapper.toDto(entity));
            assertValidClassPageAttachment(entity, false);
            scope.requireWritable(institutionOfClassPageAttachment(entity));
            assertUniqueClassPageAttachment(entity);
            if (fields.isEmpty()) {
                return classPageAttachmentMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return classPageAttachmentMapper.toDto(entity);
            }
            if (classPageAttachmentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CLASS_PAGE_ATTACHMENT_AGGREGATE, id, fields);
            outbox.publish(CLASS_PAGE_ATTACHMENT_AGGREGATE + CommsEvents.UPDATED, CLASS_PAGE_ATTACHMENT_AGGREGATE, id,
                    CommsEvents.payload(id));
            return classPageAttachmentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteClassPageAttachment(UUID classPageId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CLASS_PAGE_ATTACHMENT_PERMISSION_DELETE);
            ClassPageAttachmentEntity entity = loadVisibleClassPageAttachment(scope, classPageId, id);
            scope.requireWritable(institutionOfClassPageAttachment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (classPageAttachmentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CLASS_PAGE_ATTACHMENT_AGGREGATE, id);
            outbox.publish(CLASS_PAGE_ATTACHMENT_AGGREGATE + CommsEvents.DELETED, CLASS_PAGE_ATTACHMENT_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ClassPageAttachmentEntity loadVisibleClassPageAttachment(DataScope scope, UUID classPageId, UUID id) {
        ClassPageAttachmentEntity entity = classPageAttachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_PAGE_ATTACHMENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfClassPageAttachment(entity)) || !classPageId.equals(entity.getClassPageId())) {
            throw new ResourceNotFoundException(CLASS_PAGE_ATTACHMENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the comms.class_page the row belongs to. */
    private Optional<UUID> institutionOfClassPageAttachment(ClassPageAttachmentEntity entity) {
        return classPageAttachmentRepository.institutionOf(entity.getClassPageId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyClassPageAttachment(ClassPageAttachmentEntity entity, ClassPageAttachmentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("fileId", changes.getFileId(), entity::setFileId);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidClassPageAttachment(ClassPageAttachmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getClassPageId() == null) {
            errors.add(new FieldError("classPageId", "must not be null", "NotNull"));
        }
        if (entity.getFileId() == null) {
            errors.add(new FieldError("fileId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getClassPageId() != null && !classPageAttachmentRepository.existsClassPageId(entity.getClassPageId())) {
            errors.add(new FieldError("classPageId", "class page does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueClassPageAttachment(ClassPageAttachmentEntity entity) {

    }

    // ---- ClassPageMember ----

    public ClassPageMember getClassPageMember(UUID classPageId, UUID id) {
        return doInReadTransaction(() -> {
            return classPageMemberMapper.toDto(loadVisibleClassPageMember(DataScope.current(
                    CLASS_PAGE_MEMBER_PERMISSION_GET), classPageId, id));
        });
    }

    public Page<ClassPageMember> listClassPageMembers(UUID classPageId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ClassPageMemberRepository.SORT_FIELDS, ClassPageMemberRepository.DEFAULT_ORDER,
                ClassPageMemberRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ClassPageMemberRepository.FILTER_FIELDS)
                .and(ClassPageMemberRepository.ofParent(classPageId))
                        .and(ClassPageMemberRepository.scope(DataScope.current(CLASS_PAGE_MEMBER_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + classPageId, clock);
        return doInReadTransaction(() -> {
            Page<ClassPageMemberEntity> rows = classPageMemberRepository.list(page, where);
            return rows.map(classPageMemberMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ClassPageMember createClassPageMember(UUID classPageId, ClassPageMember request, boolean validateOnly) {
        validator.validate(request);
        ClassPageMemberEntity entity = classPageMemberMapper.toEntity(request);
        entity.setClassPageId(classPageId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CLASS_PAGE_MEMBER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!classPageMemberRepository.existsClassPageId(entity.getClassPageId())) {
                throw new ResourceNotFoundException("comms.class_page", entity.getClassPageId());
            }
            if (!scope.canRead(institutionOfClassPageMember(entity))) {
                throw new ResourceNotFoundException("comms.class_page", entity.getClassPageId());
            }
            assertValidClassPageMember(entity, true);
            scope.requireWritable(institutionOfClassPageMember(entity));
            assertUniqueClassPageMember(entity);
            if (validateOnly) {
                return classPageMemberMapper.toDto(entity);
            }
            ClassPageMemberEntity stored = classPageMemberRepository.insert(entity);
            audit.recordCreate(CLASS_PAGE_MEMBER_AGGREGATE, stored.getId());
            outbox.publish(CLASS_PAGE_MEMBER_AGGREGATE + CommsEvents.CREATED, CLASS_PAGE_MEMBER_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return classPageMemberMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public ClassPageMember updateClassPageMember(UUID classPageId, UUID id, String ifMatch,
            ClassPageMemberUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CLASS_PAGE_MEMBER_PERMISSION_UPDATE);
            ClassPageMemberEntity entity = loadVisibleClassPageMember(scope, classPageId, id);
            scope.requireWritable(institutionOfClassPageMember(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyClassPageMember(entity, changes);
            validator.validate(classPageMemberMapper.toDto(entity));
            assertValidClassPageMember(entity, false);
            scope.requireWritable(institutionOfClassPageMember(entity));
            assertUniqueClassPageMember(entity);
            if (fields.isEmpty()) {
                return classPageMemberMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return classPageMemberMapper.toDto(entity);
            }
            if (classPageMemberRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CLASS_PAGE_MEMBER_AGGREGATE, id, fields);
            outbox.publish(CLASS_PAGE_MEMBER_AGGREGATE + CommsEvents.UPDATED, CLASS_PAGE_MEMBER_AGGREGATE, id,
                    CommsEvents.payload(id));
            return classPageMemberMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteClassPageMember(UUID classPageId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CLASS_PAGE_MEMBER_PERMISSION_DELETE);
            ClassPageMemberEntity entity = loadVisibleClassPageMember(scope, classPageId, id);
            scope.requireWritable(institutionOfClassPageMember(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (classPageMemberRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CLASS_PAGE_MEMBER_AGGREGATE, id);
            outbox.publish(CLASS_PAGE_MEMBER_AGGREGATE + CommsEvents.DELETED, CLASS_PAGE_MEMBER_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ClassPageMemberEntity loadVisibleClassPageMember(DataScope scope, UUID classPageId, UUID id) {
        ClassPageMemberEntity entity = classPageMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_PAGE_MEMBER_AGGREGATE, id));
        if (!scope.canRead(institutionOfClassPageMember(entity)) || !classPageId.equals(entity.getClassPageId())) {
            throw new ResourceNotFoundException(CLASS_PAGE_MEMBER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the comms.class_page the row belongs to. */
    private Optional<UUID> institutionOfClassPageMember(ClassPageMemberEntity entity) {
        return classPageMemberRepository.institutionOf(entity.getClassPageId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyClassPageMember(ClassPageMemberEntity entity, ClassPageMemberUpdate changes) {
        Patches patch = new Patches();
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("isFollowing", changes.getIsFollowing(), entity::setIsFollowing);
        patch.apply("reaction", changes.getReaction(), v -> entity.setReaction(v == null ? null : v.getValue()));
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidClassPageMember(ClassPageMemberEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getClassPageId() == null) {
            errors.add(new FieldError("classPageId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsFollowing() == null) {
            errors.add(new FieldError("isFollowing", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getClassPageId() != null && !classPageMemberRepository.existsClassPageId(entity.getClassPageId())) {
            errors.add(new FieldError("classPageId", "class page does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueClassPageMember(ClassPageMemberEntity entity) {
        if (classPageMemberRepository.takenClassPageIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A class page member with the same class page id and user id already exists.");
        }
    }
}
