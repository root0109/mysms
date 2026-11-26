package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.learning.lms.api.model.ConferencingAccount;
import com.mysms.learning.lms.api.model.ConferencingAccountUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
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
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.ConferencingAccountEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.ConferencingAccountMapper;
import com.mysms.learning.lms.repository.ConferencingAccountRepository;
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
 * Business service of lms.conferencing_account: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ConferencingAccountService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = ConferencingAccountEntity.TABLE;
    static final String PERMISSION_LIST = "lms.conferencing_account.read";
    static final String PERMISSION_GET = "lms.conferencing_account.read";
    static final String PERMISSION_CREATE = "lms.conferencing_account.create";
    static final String PERMISSION_UPDATE = "lms.conferencing_account.update";
    static final String PERMISSION_DELETE = "lms.conferencing_account.delete";


    private final ConferencingAccountRepository repository;
    private final ConferencingAccountMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("clientSecret");
    private static final String SECRET_CLIENT_SECRET_ENC = "lms.conferencing_account.client_secret_enc";
    private final FieldCipher cipher;

    public ConferencingAccountService(ConferencingAccountRepository repository, ConferencingAccountMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, FieldCipher cipher) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.cipher = cipher;
    }

    /** Refuses to start without a master key: this resource stores secret columns. */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public ConferencingAccount getConferencingAccount(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ConferencingAccount> listConferencingAccounts(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ConferencingAccountRepository.SORT_FIELDS,
                        ConferencingAccountRepository.DEFAULT_ORDER,
                ConferencingAccountRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ConferencingAccountRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ConferencingAccountEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ConferencingAccount createConferencingAccount(ConferencingAccount request, boolean validateOnly) {
        validator.validate(request);
        ConferencingAccountEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealClientSecret(entity, request.getClientSecret());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            ConferencingAccountEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public ConferencingAccount updateConferencingAccount(UUID id, String ifMatch, ConferencingAccountUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ConferencingAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
    public void deleteConferencingAccount(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ConferencingAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCohortConferencingAccountId(id)) {
                throw new InvalidStateException(
                        "The conferencing account is still referenced by lms.cohort rows; remove those first.");
            }
            if (repository.referencedByConferencingTokenConferencingAccountId(id)) {
                throw new InvalidStateException(
                        "The conferencing account is still referenced by lms.conferencing_token rows; remove those "
                                + "first.");
            }
            if (repository.referencedByLiveClassConferencingAccountId(id)) {
                throw new InvalidStateException(
                        "The conferencing account is still referenced by lms.live_class rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** Stores clientSecret encrypted for the row's tenant and column; never the plaintext. */
    private void sealClientSecret(ConferencingAccountEntity entity, String plaintext) {
        entity.setClientSecretEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_CLIENT_SECRET_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private ConferencingAccount dto(ConferencingAccountEntity entity) {
        ConferencingAccount dto = mapper.toDto(entity);
        dto.setClientSecretMasked(
                cipher.maskStored(entity.getClientSecretEnc(), entity.getTenantId(), SECRET_CLIENT_SECRET_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ConferencingAccountEntity loadVisible(DataScope scope, UUID id) {
        ConferencingAccountEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(ConferencingAccountEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private List<String> apply(ConferencingAccountEntity entity, ConferencingAccountUpdate changes) {
        Patches patch = new Patches();
        patch.apply("provider", changes.getProvider(), v -> entity.setProvider(v == null ? null : v.getValue()));
        patch.apply("accountName", changes.getAccountName(), entity::setAccountName);
        patch.apply("ownerUserId", changes.getOwnerUserId(), entity::setOwnerUserId);
        patch.apply("isEnabled", changes.getIsEnabled(), entity::setIsEnabled);
        patch.apply("externalAccountId", changes.getExternalAccountId(), entity::setExternalAccountId);
        patch.apply("clientId", changes.getClientId(), entity::setClientId);
        patch.apply("clientSecret", changes.getClientSecret(), v -> sealClientSecret(entity, v));
        patch.apply("externalCalendarId", changes.getExternalCalendarId(), entity::setExternalCalendarId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ConferencingAccountEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getProvider() == null) {
            errors.add(new FieldError("provider", "must not be null", "NotNull"));
        }
        if (entity.getAccountName() == null) {
            errors.add(new FieldError("accountName", "must not be null", "NotNull"));
        }
        if (entity.getOwnerUserId() == null) {
            errors.add(new FieldError("ownerUserId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsEnabled() == null) {
            errors.add(new FieldError("isEnabled", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ConferencingAccountEntity entity) {
        if (repository.takenAccountName1(entity)) {
            throw new DuplicateResourceException("A conferencing account with the same account name already exists.");
        }
    }
}
