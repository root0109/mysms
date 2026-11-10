package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
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
import com.mysms.lms.api.model.PlagiarismAccount;
import com.mysms.lms.api.model.PlagiarismAccountUpdate;
import com.mysms.lms.api.model.PlagiarismUserLink;
import com.mysms.lms.api.model.PlagiarismUserLinkUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.PlagiarismAccountEntity;
import com.mysms.lms.domain.PlagiarismUserLinkEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.PlagiarismAccountMapper;
import com.mysms.lms.mapper.PlagiarismUserLinkMapper;
import com.mysms.lms.repository.PlagiarismAccountRepository;
import com.mysms.lms.repository.PlagiarismUserLinkRepository;
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
 * Business service of lms.plagiarism_account (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a PlagiarismAccount
 * are served here too — PlagiarismUserLink.
 */
@Service
public class PlagiarismAccountService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = PlagiarismAccountEntity.TABLE;
    static final String PERMISSION_LIST = "lms.plagiarism_account.read";
    static final String PERMISSION_GET = "lms.plagiarism_account.read";
    static final String PERMISSION_CREATE = "lms.plagiarism_account.create";
    static final String PERMISSION_UPDATE = "lms.plagiarism_account.update";
    static final String PERMISSION_DELETE = "lms.plagiarism_account.delete";
    /** The contract's PlagiarismAccountUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("provider", "accountRef", "subAccountRef", "apiUrl", "sharedSecret", "isDefault",
                    "defaultTeacherUserId",
            "defaultTeacherEmail", "defaultTeacherFirstName", "defaultTeacherLastName");

    public static final String PLAGIARISM_USER_LINK_AGGREGATE = PlagiarismUserLinkEntity.TABLE;
    static final String PLAGIARISM_USER_LINK_PERMISSION_LIST = "lms.plagiarism_user_link.read";
    static final String PLAGIARISM_USER_LINK_PERMISSION_GET = "lms.plagiarism_user_link.read";
    static final String PLAGIARISM_USER_LINK_PERMISSION_CREATE = "lms.plagiarism_user_link.create";
    static final String PLAGIARISM_USER_LINK_PERMISSION_UPDATE = "lms.plagiarism_user_link.update";
    static final String PLAGIARISM_USER_LINK_PERMISSION_DELETE = "lms.plagiarism_user_link.delete";
    /** The contract's PlagiarismUserLinkUpdate fields, in apply order. */
    static final List<String> PLAGIARISM_USER_LINK_WRITABLE = List.of("userId", "accountRef", "providerUserRef");

    private final PlagiarismAccountRepository repository;
    private final PlagiarismAccountMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("sharedSecret");
    private static final String SECRET_SHARED_SECRET_ENC = "lms.plagiarism_account.shared_secret_enc";
    private final FieldCipher cipher;
    private final PlagiarismUserLinkRepository plagiarismUserLinkRepository;
    private final PlagiarismUserLinkMapper plagiarismUserLinkMapper;

    public PlagiarismAccountService(PlagiarismAccountRepository repository,
            PlagiarismAccountMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            FieldCipher cipher,
            PlagiarismUserLinkRepository plagiarismUserLinkRepository,
            PlagiarismUserLinkMapper plagiarismUserLinkMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.cipher = cipher;
        this.plagiarismUserLinkRepository = plagiarismUserLinkRepository;
        this.plagiarismUserLinkMapper = plagiarismUserLinkMapper;
    }

    /** Refuses to start without a master key: this resource stores secret columns (DEPLOYMENT.md "Secrets"). */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public PlagiarismAccount getPlagiarismAccount(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<PlagiarismAccount> listPlagiarismAccounts(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PlagiarismAccountRepository.SORT_FIELDS,
                        PlagiarismAccountRepository.DEFAULT_ORDER,
                PlagiarismAccountRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PlagiarismAccountRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PlagiarismAccountEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PlagiarismAccount createPlagiarismAccount(PlagiarismAccount request, boolean validateOnly) {
        validator.validate(request);
        PlagiarismAccountEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealSharedSecret(entity, request.getSharedSecret());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            PlagiarismAccountEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PlagiarismAccount updatePlagiarismAccount(UUID id, String ifMatch, PlagiarismAccountUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PlagiarismAccountEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return dto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePlagiarismAccount(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PlagiarismAccountEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByPlagiarismCohortLinkPlagiarismAccountId(id)) {
                throw new InvalidStateException(
                        "The plagiarism account is still referenced by lms.plagiarism_cohort_link rows; remove those "
                                + "first.");
            }
            if (repository.referencedByPlagiarismReportPlagiarismAccountId(id)) {
                throw new InvalidStateException(
                        "The plagiarism account is still referenced by lms.plagiarism_report rows; remove those "
                                + "first.");
            }
            if (repository.referencedByPlagiarismUserLinkPlagiarismAccountId(id)) {
                throw new InvalidStateException(
                        "The plagiarism account is still referenced by lms.plagiarism_user_link rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /**
     * Stores sharedSecret encrypted for the row's tenant and column (database/README "Secrets"); never the plaintext.
     */
    private void sealSharedSecret(PlagiarismAccountEntity entity, String plaintext) {
        entity.setSharedSecretEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_SHARED_SECRET_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private PlagiarismAccount dto(PlagiarismAccountEntity entity) {
        PlagiarismAccount dto = mapper.toDto(entity);
        dto.setSharedSecretMasked(
                cipher.maskStored(entity.getSharedSecretEnc(), entity.getTenantId(), SECRET_SHARED_SECRET_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PlagiarismAccountEntity loadVisible(DataScope scope, UUID id) {
        PlagiarismAccountEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(PlagiarismAccountEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected PlagiarismAccountUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private void apply(PlagiarismAccountEntity entity, PlagiarismAccountUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "provider" -> entity.setProvider(changes.getProvider());
                case "accountRef" -> entity.setAccountRef(changes.getAccountRef());
                case "subAccountRef" -> entity.setSubAccountRef(changes.getSubAccountRef());
                case "apiUrl" -> entity.setApiUrl(changes.getApiUrl());
                case "sharedSecret" -> sealSharedSecret(entity, changes.getSharedSecret());
                case "isDefault" -> entity.setIsDefault(changes.getIsDefault());
                case "defaultTeacherUserId" -> entity.setDefaultTeacherUserId(changes.getDefaultTeacherUserId());
                case "defaultTeacherEmail" -> entity.setDefaultTeacherEmail(changes.getDefaultTeacherEmail());
                case "defaultTeacherFirstName" -> entity.setDefaultTeacherFirstName(
                        changes.getDefaultTeacherFirstName());
                case "defaultTeacherLastName" -> entity.setDefaultTeacherLastName(changes.getDefaultTeacherLastName());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PlagiarismAccountEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getProvider() == null) {
            errors.add(new FieldError("provider", "must not be null", "NotNull"));
        }
        if (entity.getAccountRef() == null) {
            errors.add(new FieldError("accountRef", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsDefault() == null) {
            errors.add(new FieldError("isDefault", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PlagiarismAccountEntity entity) {

    }

    // ---- PlagiarismUserLink: a record of the PlagiarismAccount aggregate ----

    public PlagiarismUserLink getPlagiarismUserLink(UUID plagiarismAccountId, UUID id) {
        return doInReadTransaction(() -> {
            return plagiarismUserLinkMapper.toDto(loadVisiblePlagiarismUserLink(DataScope.current(
                    PLAGIARISM_USER_LINK_PERMISSION_GET), plagiarismAccountId, id));
        });
    }

    public Page<PlagiarismUserLink> listPlagiarismUserLinks(UUID plagiarismAccountId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PlagiarismUserLinkRepository.SORT_FIELDS,
                        PlagiarismUserLinkRepository.DEFAULT_ORDER,
                PlagiarismUserLinkRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PlagiarismUserLinkRepository.FILTER_FIELDS)
                .and(PlagiarismUserLinkRepository.ofParent(plagiarismAccountId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + plagiarismAccountId, clock);
        return doInReadTransaction(() -> {
            Page<PlagiarismUserLinkEntity> rows = plagiarismUserLinkRepository.list(page, where);
            return rows.map(plagiarismUserLinkMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PlagiarismUserLink createPlagiarismUserLink(UUID plagiarismAccountId, PlagiarismUserLink request,
            boolean validateOnly) {
        validator.validate(request);
        PlagiarismUserLinkEntity entity = plagiarismUserLinkMapper.toEntity(request);
        entity.setPlagiarismAccountId(plagiarismAccountId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PLAGIARISM_USER_LINK_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!plagiarismUserLinkRepository.existsPlagiarismAccountId(entity.getPlagiarismAccountId())) {
                throw new ResourceNotFoundException("lms.plagiarism_account", entity.getPlagiarismAccountId());
            }
            assertValidPlagiarismUserLink(entity, true);
            scope.requireWritable(institutionOfPlagiarismUserLink(entity));
            assertUniquePlagiarismUserLink(entity);
            if (validateOnly) {
                return plagiarismUserLinkMapper.toDto(entity);
            }
            PlagiarismUserLinkEntity stored = plagiarismUserLinkRepository.insert(entity);
            audit.recordCreate(PLAGIARISM_USER_LINK_AGGREGATE, stored.getId());
            outbox.publish(PLAGIARISM_USER_LINK_AGGREGATE + LmsEvents.CREATED, PLAGIARISM_USER_LINK_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return plagiarismUserLinkMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PlagiarismUserLink updatePlagiarismUserLink(UUID plagiarismAccountId, UUID id, String ifMatch,
            PlagiarismUserLinkUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PLAGIARISM_USER_LINK_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PLAGIARISM_USER_LINK_PERMISSION_UPDATE);
            PlagiarismUserLinkEntity entity = loadVisiblePlagiarismUserLink(scope, plagiarismAccountId, id);
            scope.requireWritable(institutionOfPlagiarismUserLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPlagiarismUserLink(entity, changes, fields);
            validator.validate(plagiarismUserLinkMapper.toDto(entity));
            assertValidPlagiarismUserLink(entity, false);
            scope.requireWritable(institutionOfPlagiarismUserLink(entity));
            assertUniquePlagiarismUserLink(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return plagiarismUserLinkMapper.toDto(entity);
            }
            if (plagiarismUserLinkRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PLAGIARISM_USER_LINK_AGGREGATE, id, fields);
            outbox.publish(PLAGIARISM_USER_LINK_AGGREGATE + LmsEvents.UPDATED, PLAGIARISM_USER_LINK_AGGREGATE, id,
                    LmsEvents.payload(id));
            return plagiarismUserLinkMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePlagiarismUserLink(UUID plagiarismAccountId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PLAGIARISM_USER_LINK_PERMISSION_DELETE);
            PlagiarismUserLinkEntity entity = loadVisiblePlagiarismUserLink(scope, plagiarismAccountId, id);
            scope.requireWritable(institutionOfPlagiarismUserLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (plagiarismUserLinkRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PLAGIARISM_USER_LINK_AGGREGATE, id);
            outbox.publish(PLAGIARISM_USER_LINK_AGGREGATE + LmsEvents.DELETED, PLAGIARISM_USER_LINK_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PlagiarismUserLinkEntity loadVisiblePlagiarismUserLink(DataScope scope, UUID plagiarismAccountId, UUID id) {
        PlagiarismUserLinkEntity entity = plagiarismUserLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PLAGIARISM_USER_LINK_AGGREGATE, id));
        if (!scope.canRead(institutionOfPlagiarismUserLink(entity))
                || !plagiarismAccountId.equals(entity.getPlagiarismAccountId())) {
            throw new ResourceNotFoundException(PLAGIARISM_USER_LINK_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfPlagiarismUserLink(PlagiarismUserLinkEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected PlagiarismUserLinkUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyPlagiarismUserLink(PlagiarismUserLinkEntity entity, PlagiarismUserLinkUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "accountRef" -> entity.setAccountRef(changes.getAccountRef());
                case "providerUserRef" -> entity.setProviderUserRef(changes.getProviderUserRef());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPlagiarismUserLink(PlagiarismUserLinkEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getPlagiarismAccountId() == null) {
            errors.add(new FieldError("plagiarismAccountId", "must not be null", "NotNull"));
        }
        if (entity.getProviderUserRef() == null) {
            errors.add(new FieldError("providerUserRef", "must not be null", "NotNull"));
        }
        if (entity.getPlagiarismAccountId() != null
                && !plagiarismUserLinkRepository.existsPlagiarismAccountId(entity.getPlagiarismAccountId())) {
            errors.add(new FieldError("plagiarismAccountId", "plagiarism account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePlagiarismUserLink(PlagiarismUserLinkEntity entity) {
        if (plagiarismUserLinkRepository.takenUserIdPlagiarismAccountId1(entity)) {
            throw new DuplicateResourceException(
                    "A plagiarism user link with the same user id and plagiarism account id already exists.");
        }
    }
}
