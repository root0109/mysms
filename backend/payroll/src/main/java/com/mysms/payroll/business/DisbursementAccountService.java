package com.mysms.payroll.business;

import com.mysms.authclient.DataScope;
import com.mysms.payroll.api.model.DisbursementAccount;
import com.mysms.payroll.api.model.DisbursementAccountUpdate;
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
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.config.PayrollEventsConfig;
import com.mysms.payroll.domain.DisbursementAccountEntity;
import com.mysms.payroll.events.PayrollEvents;
import com.mysms.payroll.mapper.DisbursementAccountMapper;
import com.mysms.payroll.repository.DisbursementAccountRepository;
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
 * Business service of payroll.disbursement_account: audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class DisbursementAccountService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = DisbursementAccountEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.disbursement_account.read";
    static final String PERMISSION_GET = "payroll.disbursement_account.read";
    static final String PERMISSION_CREATE = "payroll.disbursement_account.create";
    static final String PERMISSION_UPDATE = "payroll.disbursement_account.update";
    static final String PERMISSION_DELETE = "payroll.disbursement_account.delete";
    /** The contract's DisbursementAccountUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "managedByUserId", "name", "accountKey", "accountNumber", "bankRoutingCode",
                    "branchName",
            "extraReference");


    private final DisbursementAccountRepository repository;
    private final DisbursementAccountMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("accountNumber");
    private static final String SECRET_ACCOUNT_NUMBER_ENC = "payroll.disbursement_account.account_number_enc";
    private final FieldCipher cipher;

    public DisbursementAccountService(DisbursementAccountRepository repository, DisbursementAccountMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
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

    public DisbursementAccount getDisbursementAccount(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<DisbursementAccount> listDisbursementAccounts(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, DisbursementAccountRepository.SORT_FIELDS,
                        DisbursementAccountRepository.DEFAULT_ORDER,
                DisbursementAccountRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, DisbursementAccountRepository.FILTER_FIELDS)
                .and(DisbursementAccountRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<DisbursementAccountEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public DisbursementAccount createDisbursementAccount(DisbursementAccount request, boolean validateOnly) {
        validator.validate(request);
        DisbursementAccountEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealAccountNumber(entity, request.getAccountNumber());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            DisbursementAccountEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PayrollEvents.CREATED, AGGREGATE, stored.getId(),
                    PayrollEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public DisbursementAccount updateDisbursementAccount(UUID id, String ifMatch, DisbursementAccountUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            DisbursementAccountEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + PayrollEvents.UPDATED, AGGREGATE, id, PayrollEvents.payload(id));
            return dto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteDisbursementAccount(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            DisbursementAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByPayrollPaymentDisbursementAccountId(id)) {
                throw new InvalidStateException(
                        "The disbursement account is still referenced by payroll.payroll_payment rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PayrollEvents.DELETED, AGGREGATE, id, PayrollEvents.payload(id));
        });
    }

    /** Stores accountNumber encrypted for the row's tenant and column; never the plaintext. */
    private void sealAccountNumber(DisbursementAccountEntity entity, String plaintext) {
        entity.setAccountNumberEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_ACCOUNT_NUMBER_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private DisbursementAccount dto(DisbursementAccountEntity entity) {
        DisbursementAccount dto = mapper.toDto(entity);
        dto.setAccountNumberMasked(
                cipher.maskStored(entity.getAccountNumberEnc(), entity.getTenantId(), SECRET_ACCOUNT_NUMBER_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private DisbursementAccountEntity loadVisible(DataScope scope, UUID id) {
        DisbursementAccountEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(DisbursementAccountEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected DisbursementAccountUpdate fields; explicit per field so a mask can clear values. */
    private void apply(DisbursementAccountEntity entity, DisbursementAccountUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "managedByUserId" -> entity.setManagedByUserId(changes.getManagedByUserId());
                case "name" -> entity.setName(changes.getName());
                case "accountKey" -> entity.setAccountKey(changes.getAccountKey());
                case "accountNumber" -> sealAccountNumber(entity, changes.getAccountNumber());
                case "bankRoutingCode" -> entity.setBankRoutingCode(changes.getBankRoutingCode());
                case "branchName" -> entity.setBranchName(changes.getBranchName());
                case "extraReference" -> entity.setExtraReference(changes.getExtraReference());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(DisbursementAccountEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getManagedByUserId() == null) {
            errors.add(new FieldError("managedByUserId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getAccountKey() == null) {
            errors.add(new FieldError("accountKey", "must not be null", "NotNull"));
        }
        if (entity.getAccountNumberEnc() == null) {
            errors.add(new FieldError("accountNumber", "must not be null", "NotNull"));
        }
        if (entity.getBankRoutingCode() == null) {
            errors.add(new FieldError("bankRoutingCode", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(DisbursementAccountEntity entity) {
        if (repository.takenInstitutionIdAccountKey1(entity)) {
            throw new DuplicateResourceException(
                    "A disbursement account with the same institution id and account key already exists.");
        }
    }
}
