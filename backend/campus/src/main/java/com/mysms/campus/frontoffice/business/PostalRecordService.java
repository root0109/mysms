package com.mysms.campus.frontoffice.business;

import com.mysms.authclient.DataScope;
import com.mysms.campus.frontoffice.api.model.PostalRecord;
import com.mysms.campus.frontoffice.api.model.PostalRecordUpdate;
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
import com.mysms.core.web.Patches;
import com.mysms.campus.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.campus.frontoffice.config.FrontofficeEventsConfig;
import com.mysms.campus.frontoffice.domain.PostalRecordEntity;
import com.mysms.campus.frontoffice.events.FrontofficeEvents;
import com.mysms.campus.frontoffice.mapper.PostalRecordMapper;
import com.mysms.campus.frontoffice.repository.PostalRecordRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of frontoffice.postal_record: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class PostalRecordService extends AppTransactionManager {

    public static final String AGGREGATE = PostalRecordEntity.TABLE;
    static final String PERMISSION_LIST = "frontoffice.postal_record.read";
    static final String PERMISSION_GET = "frontoffice.postal_record.read";
    static final String PERMISSION_CREATE = "frontoffice.postal_record.create";
    static final String PERMISSION_UPDATE = "frontoffice.postal_record.update";
    static final String PERMISSION_DELETE = "frontoffice.postal_record.delete";


    private final PostalRecordRepository repository;
    private final PostalRecordMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public PostalRecordService(PostalRecordRepository repository, PostalRecordMapper mapper,
            @Qualifier(FrontofficeDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FrontofficeDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FrontofficeEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(FrontofficeEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public PostalRecord getPostalRecord(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<PostalRecord> listPostalRecords(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PostalRecordRepository.SORT_FIELDS, PostalRecordRepository.DEFAULT_ORDER,
                PostalRecordRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PostalRecordRepository.FILTER_FIELDS)
                .and(PostalRecordRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PostalRecordEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PostalRecord createPostalRecord(PostalRecord request, boolean validateOnly) {
        validator.validate(request);
        PostalRecordEntity entity = mapper.toEntity(request);
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
            PostalRecordEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FrontofficeEvents.CREATED, AGGREGATE, stored.getId(),
                    FrontofficeEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public PostalRecord updatePostalRecord(UUID id, String ifMatch, PostalRecordUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PostalRecordEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + FrontofficeEvents.UPDATED, AGGREGATE, id, FrontofficeEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePostalRecord(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PostalRecordEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + FrontofficeEvents.DELETED, AGGREGATE, id, FrontofficeEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PostalRecordEntity loadVisible(DataScope scope, UUID id) {
        PostalRecordEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(PostalRecordEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(PostalRecordEntity entity, PostalRecordUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("direction", changes.getDirection(), v -> entity.setDirection(v == null ? null : v.getValue()));
        patch.apply("postType", changes.getPostType(), entity::setPostType);
        patch.apply("referenceNumber", changes.getReferenceNumber(), entity::setReferenceNumber);
        patch.apply("isConfidential", changes.getIsConfidential(), entity::setIsConfidential);
        patch.apply("senderName", changes.getSenderName(), entity::setSenderName);
        patch.apply("senderAddress", changes.getSenderAddress(), entity::setSenderAddress);
        patch.apply("receiverName", changes.getReceiverName(), entity::setReceiverName);
        patch.apply("receiverAddress", changes.getReceiverAddress(), entity::setReceiverAddress);
        patch.apply("postalDate", changes.getPostalDate(), entity::setPostalDate);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("attachmentFileId", changes.getAttachmentFileId(), entity::setAttachmentFileId);
        patch.apply("loggedByUserId", changes.getLoggedByUserId(), entity::setLoggedByUserId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PostalRecordEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getDirection() == null) {
            errors.add(new FieldError("direction", "must not be null", "NotNull"));
        }
        if (entity.getPostType() == null) {
            errors.add(new FieldError("postType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsConfidential() == null) {
            errors.add(new FieldError("isConfidential", "must not be null", "NotNull"));
        }
        if (entity.getPostalDate() == null) {
            errors.add(new FieldError("postalDate", "must not be null", "NotNull"));
        }
        if (entity.getDescription() == null) {
            errors.add(new FieldError("description", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PostalRecordEntity entity) {

    }
}
