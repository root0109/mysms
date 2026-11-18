package com.mysms.finance.business;

import com.mysms.authclient.DataScope;
import com.mysms.finance.api.model.PaymentGatewayAccount;
import com.mysms.finance.api.model.PaymentGatewayAccountUpdate;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.PaymentGatewayAccountEntity;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.PaymentGatewayAccountMapper;
import com.mysms.finance.repository.PaymentGatewayAccountRepository;
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
 * Business service of finance.payment_gateway_account: audit and domain event in the same transaction as every change;
 * rows outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class PaymentGatewayAccountService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = PaymentGatewayAccountEntity.TABLE;
    static final String PERMISSION_LIST = "finance.payment_gateway_account.read";
    static final String PERMISSION_GET = "finance.payment_gateway_account.read";
    static final String PERMISSION_CREATE = "finance.payment_gateway_account.create";
    static final String PERMISSION_UPDATE = "finance.payment_gateway_account.update";
    static final String PERMISSION_DELETE = "finance.payment_gateway_account.delete";
    /** The contract's PaymentGatewayAccountUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("keySecret", "webhookSecret", "institutionId", "gatewayName", "displayName", "merchantId", "keyId",
            "currencyId", "isDefault", "isActive");


    private final PaymentGatewayAccountRepository repository;
    private final PaymentGatewayAccountMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("keySecret", "webhookSecret");
    private static final String SECRET_KEY_SECRET_ENC = "finance.payment_gateway_account.key_secret_enc";
    private static final String SECRET_WEBHOOK_SECRET_ENC = "finance.payment_gateway_account.webhook_secret_enc";
    private final FieldCipher cipher;

    public PaymentGatewayAccountService(PaymentGatewayAccountRepository repository, PaymentGatewayAccountMapper mapper,
            @Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit,
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

    public PaymentGatewayAccount getPaymentGatewayAccount(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<PaymentGatewayAccount> listPaymentGatewayAccounts(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PaymentGatewayAccountRepository.SORT_FIELDS,
                        PaymentGatewayAccountRepository.DEFAULT_ORDER,
                PaymentGatewayAccountRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PaymentGatewayAccountRepository.FILTER_FIELDS)
                .and(PaymentGatewayAccountRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PaymentGatewayAccountEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PaymentGatewayAccount createPaymentGatewayAccount(PaymentGatewayAccount request, boolean validateOnly) {
        validator.validate(request);
        PaymentGatewayAccountEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealKeySecret(entity, request.getKeySecret());
        sealWebhookSecret(entity, request.getWebhookSecret());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            PaymentGatewayAccountEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, stored.getId(),
                    FinanceEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PaymentGatewayAccount updatePaymentGatewayAccount(UUID id, String ifMatch,
            PaymentGatewayAccountUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PaymentGatewayAccountEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + FinanceEvents.UPDATED, AGGREGATE, id, FinanceEvents.payload(id));
            return dto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePaymentGatewayAccount(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PaymentGatewayAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByGatewayEventGatewayAccountId(id)) {
                throw new InvalidStateException(
                        "The payment gateway account is still referenced by finance.gateway_event rows; remove those "
                                + "first.");
            }
            if (repository.referencedByPaymentGatewayAccountId(id)) {
                throw new InvalidStateException(
                        "The payment gateway account is still referenced by finance.payment rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + FinanceEvents.DELETED, AGGREGATE, id, FinanceEvents.payload(id));
        });
    }

    /** Stores keySecret encrypted for the row's tenant and column; never the plaintext. */
    private void sealKeySecret(PaymentGatewayAccountEntity entity, String plaintext) {
        entity.setKeySecretEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_KEY_SECRET_ENC));
    }

    /** Stores webhookSecret encrypted for the row's tenant and column; never the plaintext. */
    private void sealWebhookSecret(PaymentGatewayAccountEntity entity, String plaintext) {
        entity.setWebhookSecretEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_WEBHOOK_SECRET_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private PaymentGatewayAccount dto(PaymentGatewayAccountEntity entity) {
        PaymentGatewayAccount dto = mapper.toDto(entity);
        dto.setKeySecretMasked(
                cipher.maskStored(entity.getKeySecretEnc(), entity.getTenantId(), SECRET_KEY_SECRET_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PaymentGatewayAccountEntity loadVisible(DataScope scope, UUID id) {
        PaymentGatewayAccountEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(PaymentGatewayAccountEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected PaymentGatewayAccountUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private void apply(PaymentGatewayAccountEntity entity, PaymentGatewayAccountUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "keySecret" -> sealKeySecret(entity, changes.getKeySecret());
                case "webhookSecret" -> sealWebhookSecret(entity, changes.getWebhookSecret());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "gatewayName" -> entity.setGatewayName(changes.getGatewayName());
                case "displayName" -> entity.setDisplayName(changes.getDisplayName());
                case "merchantId" -> entity.setMerchantId(changes.getMerchantId());
                case "keyId" -> entity.setKeyId(changes.getKeyId());
                case "currencyId" -> entity.setCurrencyId(changes.getCurrencyId());
                case "isDefault" -> entity.setIsDefault(changes.getIsDefault());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PaymentGatewayAccountEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getGatewayName() == null) {
            errors.add(new FieldError("gatewayName", "must not be null", "NotNull"));
        }
        if (entity.getDisplayName() == null) {
            errors.add(new FieldError("displayName", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsDefault() == null) {
            errors.add(new FieldError("isDefault", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PaymentGatewayAccountEntity entity) {
        if (repository.takenGatewayNameDisplayName1(entity)) {
            throw new DuplicateResourceException(
                    "A payment gateway account with the same gateway name and display name already exists.");
        }
    }
}
