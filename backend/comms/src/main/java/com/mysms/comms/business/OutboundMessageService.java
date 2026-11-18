package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.OutboundMessageEntity;
import com.mysms.comms.mapper.OutboundMessageMapper;
import com.mysms.comms.repository.OutboundMessageRepository;
import com.mysms.comms.api.model.OutboundMessage;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.outbound_message: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class OutboundMessageService extends AppTransactionManager {

    public static final String AGGREGATE = OutboundMessageEntity.TABLE;
    static final String PERMISSION_LIST = "comms.outbound_message.read";
    static final String PERMISSION_GET = "comms.outbound_message.read";


    private final OutboundMessageRepository repository;
    private final OutboundMessageMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public OutboundMessageService(OutboundMessageRepository repository, OutboundMessageMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public OutboundMessage getOutboundMessage(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<OutboundMessage> listOutboundMessages(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, OutboundMessageRepository.SORT_FIELDS, OutboundMessageRepository.DEFAULT_ORDER,
                OutboundMessageRepository.ID_COLUMN);
        // a partitioned table: the list reads only the partitions of the range the client names
        FilterParser.requireRange(filter, OutboundMessageRepository.FILTER_FIELDS, "createTime");
        SqlFragment where = FilterParser.parse(filter, OutboundMessageRepository.FILTER_FIELDS)
                .and(OutboundMessageRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<OutboundMessageEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private OutboundMessageEntity loadVisible(DataScope scope, UUID id) {
        OutboundMessageEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(OutboundMessageEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(OutboundMessageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getMessageKind() == null) {
            errors.add(new FieldError("messageKind", "must not be null", "NotNull"));
        }
        if (entity.getChannel() == null) {
            errors.add(new FieldError("channel", "must not be null", "NotNull"));
        }
        if (entity.getSender() == null) {
            errors.add(new FieldError("sender", "must not be null", "NotNull"));
        }
        if (entity.getRecipient() == null) {
            errors.add(new FieldError("recipient", "must not be null", "NotNull"));
        }
        if (entity.getBody() == null) {
            errors.add(new FieldError("body", "must not be null", "NotNull"));
        }
        if (entity.getTemplateId() != null && !repository.existsTemplateId(entity.getTemplateId())) {
            errors.add(new FieldError("templateId", "message template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(OutboundMessageEntity entity) {

    }
}
