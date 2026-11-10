package com.mysms.payroll.business;

import com.mysms.authclient.DataScope;
import com.mysms.payroll.api.model.StaffBankAccount;
import com.mysms.payroll.api.model.StaffBankAccountUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
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
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.config.PayrollEventsConfig;
import com.mysms.payroll.domain.StaffBankAccountEntity;
import com.mysms.payroll.events.PayrollEvents;
import com.mysms.payroll.mapper.StaffBankAccountMapper;
import com.mysms.payroll.repository.StaffBankAccountRepository;
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
 * Business service of payroll.staff_bank_account (JAVA-CODING-RULES §4): one plain class, explicit methods,
 * transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class StaffBankAccountService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = StaffBankAccountEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.staff_bank_account.read";
    static final String PERMISSION_GET = "payroll.staff_bank_account.read";
    static final String PERMISSION_CREATE = "payroll.staff_bank_account.create";
    static final String PERMISSION_UPDATE = "payroll.staff_bank_account.update";
    static final String PERMISSION_DELETE = "payroll.staff_bank_account.delete";
    /** The contract's StaffBankAccountUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("accountNumber", "staffId", "accountHolderName", "ifscCode", "bankName", "branchName", "isPrimary",
            "isActive");


    private final StaffBankAccountRepository repository;
    private final StaffBankAccountMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("accountNumber");
    private static final String SECRET_ACCOUNT_NUMBER_ENC = "payroll.staff_bank_account.account_number_enc";
    private final FieldCipher cipher;

    public StaffBankAccountService(StaffBankAccountRepository repository, StaffBankAccountMapper mapper,
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

    /** Refuses to start without a master key: this resource stores secret columns (DEPLOYMENT.md "Secrets"). */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public StaffBankAccount getStaffBankAccount(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StaffBankAccount> listStaffBankAccounts(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StaffBankAccountRepository.SORT_FIELDS,
                        StaffBankAccountRepository.DEFAULT_ORDER,
                StaffBankAccountRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StaffBankAccountRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StaffBankAccountEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StaffBankAccount createStaffBankAccount(StaffBankAccount request, boolean validateOnly) {
        validator.validate(request);
        StaffBankAccountEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealAccountNumber(entity, request.getAccountNumber());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            StaffBankAccountEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PayrollEvents.CREATED, AGGREGATE, stored.getId(),
                    PayrollEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StaffBankAccount updateStaffBankAccount(UUID id, String ifMatch, StaffBankAccountUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StaffBankAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validateExcept(mapper.toDto(entity), SECRETS);
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
            outbox.publish(AGGREGATE + PayrollEvents.UPDATED, AGGREGATE, id, PayrollEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStaffBankAccount(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StaffBankAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PayrollEvents.DELETED, AGGREGATE, id, PayrollEvents.payload(id));
        });
    }

    /**
     * Stores accountNumber encrypted for the row's tenant and column (database/README "Secrets"); never the plaintext.
     */
    private void sealAccountNumber(StaffBankAccountEntity entity, String plaintext) {
        entity.setAccountNumberEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_ACCOUNT_NUMBER_ENC));
        entity.setAccountNumberMasked(FieldCipher.mask(plaintext));
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StaffBankAccountEntity loadVisible(DataScope scope, UUID id) {
        StaffBankAccountEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(StaffBankAccountEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected StaffBankAccountUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private void apply(StaffBankAccountEntity entity, StaffBankAccountUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "accountNumber" -> sealAccountNumber(entity, changes.getAccountNumber());
                case "staffId" -> entity.setStaffId(changes.getStaffId());
                case "accountHolderName" -> entity.setAccountHolderName(changes.getAccountHolderName());
                case "ifscCode" -> entity.setIfscCode(changes.getIfscCode());
                case "bankName" -> entity.setBankName(changes.getBankName());
                case "branchName" -> entity.setBranchName(changes.getBranchName());
                case "isPrimary" -> entity.setIsPrimary(changes.getIsPrimary());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StaffBankAccountEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAccountNumberEnc() == null) {
            errors.add(new FieldError("accountNumber", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getAccountHolderName() == null) {
            errors.add(new FieldError("accountHolderName", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPrimary() == null) {
            errors.add(new FieldError("isPrimary", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StaffBankAccountEntity entity) {

    }
}
