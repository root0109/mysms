package com.mysms.frontoffice.business;

import com.mysms.authclient.DataScope;
import com.mysms.frontoffice.api.model.PostalRecord;
import com.mysms.frontoffice.api.model.PostalRecordUpdate;
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
import com.mysms.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.frontoffice.config.FrontofficeEventsConfig;
import com.mysms.frontoffice.domain.PostalRecordEntity;
import com.mysms.frontoffice.events.FrontofficeEvents;
import com.mysms.frontoffice.mapper.PostalRecordMapper;
import com.mysms.frontoffice.repository.PostalRecordRepository;
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
    /** The contract's PostalRecordUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "direction", "postType", "referenceNumber", "isConfidential",
            "senderName", "senderAddress", "receiverName", "receiverAddress", "postalDate", "description",
            "attachmentFileId", "loggedByUserId");


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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public PostalRecord updatePostalRecord(UUID id, String ifMatch, PostalRecordUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PostalRecordEntity entity = loadVisible(scope, id);
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

    /** Applies the selected PostalRecordUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(PostalRecordEntity entity, PostalRecordUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "direction" -> entity.setDirection(Optional.ofNullable(changes.getDirection())
                        .map(v -> v.getValue()).orElse(null));
                case "postType" -> entity.setPostType(changes.getPostType());
                case "referenceNumber" -> entity.setReferenceNumber(changes.getReferenceNumber());
                case "isConfidential" -> entity.setIsConfidential(changes.getIsConfidential());
                case "senderName" -> entity.setSenderName(changes.getSenderName());
                case "senderAddress" -> entity.setSenderAddress(changes.getSenderAddress());
                case "receiverName" -> entity.setReceiverName(changes.getReceiverName());
                case "receiverAddress" -> entity.setReceiverAddress(changes.getReceiverAddress());
                case "postalDate" -> entity.setPostalDate(changes.getPostalDate());
                case "description" -> entity.setDescription(changes.getDescription());
                case "attachmentFileId" -> entity.setAttachmentFileId(changes.getAttachmentFileId());
                case "loggedByUserId" -> entity.setLoggedByUserId(changes.getLoggedByUserId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
