package com.mysms.privacy.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.MyNominee;
import com.mysms.privacy.api.model.RevokeMyNomineeRequest;
import com.mysms.privacy.api.model.SetMyNomineeRequest;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.NomineeEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.NomineeMapper;
import com.mysms.privacy.model.Majority;
import com.mysms.privacy.repository.NomineeRepository;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The nominee aggregate (privacy.nominee, DPDP Act s.14): the person an adult principal names to exercise their rights
 * on their death or incapacity. Self-service only — the principal is always the caller's own person from the token —
 * and only for an adult: a minor's rights are exercised by their legal guardian, and a person of unknown age counts
 * as a minor (403). Never while impersonating (403).
 *
 * <p>The name and contact are stored encrypted per tenant and column (core {@link FieldCipher}) and answered masked;
 * the plaintext is never returned, logged or put in an event. Naming a nominee replaces the active one (revoked in the
 * same transaction). The nominee acts for the principal only once the staff fulfil the nominee's nomination request
 * ({@link RightsRequestService}): that activates the row here. Every change writes its audit record and an id-only
 * event in the same transaction.
 */
@Service
public class NomineeService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = NomineeEntity.TABLE;
    static final String SECRET_NAME = "privacy.nominee.nominee_name_enc";
    static final String SECRET_CONTACT = "privacy.nominee.nominee_contact_enc";
    private static final String ACTIVE = "active";

    private final NomineeRepository repository;
    private final NomineeMapper mapper;
    private final SubjectRepository subjects;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final FieldCipher cipher;
    private final Clock clock;

    public NomineeService(NomineeRepository repository,
            NomineeMapper mapper,
            SubjectRepository subjects,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            FieldCipher cipher,
            Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.subjects = subjects;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.cipher = cipher;
        this.clock = clock;
    }

    /** Refuses to start without a master key: this aggregate stores secret columns. */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    /** privacy.GetMyNominee: the caller's active nominee, masked; 404 when they name none. */
    public MyNominee getMyNominee() {
        UUID principal = callerPerson(AuthContext.require());
        return doInReadTransaction(() -> repository.findActive(principal).map(this::mine)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, principal)));
    }

    /**
     * privacy.SetMyNominee: names the caller's nominee, replacing the active one (whose etag, when given, must match:
     * 412). Only an adult names a nominee (403), and never themself (403).
     */
    public MyNominee setMyNominee(SetMyNomineeRequest request) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        UUID principal = callerPerson(caller);
        UUID nomineePerson = request.getNomineePersonId();
        if (principal.equals(nomineePerson)) {
            throw new PermissionDeniedException("A person cannot be their own nominee.");
        }
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            requireAdult(subjects.lockStatus(principal, now));
            if (nomineePerson != null) {
                // a nominee from within the school is named by their person: it must be a person privacy knows, and
                // an adult (a nominee from outside the school is named by name and contact alone)
                String nomineeStatus = subjects.majority(nomineePerson)
                        .orElseThrow(() -> new ResourceNotFoundException("people.person", nomineePerson)).status();
                if (!Majority.ADULT.equals(nomineeStatus)) {
                    throw new PermissionDeniedException("A nominee must be an adult.");
                }
            }
            Optional<NomineeEntity> active = repository.findActive(principal);
            if (active.isPresent()) {
                NomineeEntity replaced = active.get();
                if (expected.isPresent() && expected.getAsInt() != replaced.getRowVersion()) {
                    throw new PreconditionFailedException("The etag does not match the active nominee.");
                }
                revoke(replaced, now);
            }
            NomineeEntity entity = NomineeEntity.builder()
                    .principalPersonId(principal)
                    .nomineePersonId(nomineePerson)
                    .status(ACTIVE)
                    .build();
            entity.onCreatingData(Actor.current(), clock);
            entity.setTenantId(tenant());
            entity.setNomineeNameEnc(cipher.encrypt(request.getNomineeName().strip(), entity.getTenantId(),
                    SECRET_NAME));
            entity.setNomineeContactEnc(cipher.encrypt(request.getNomineeContact().strip(), entity.getTenantId(),
                    SECRET_CONTACT));
            repository.insert(entity);
            audit.recordCreate(AGGREGATE, entity.getId());
            outbox.publish(AGGREGATE + PrivacyEvents.CREATED, AGGREGATE, entity.getId(),
                    PrivacyEvents.nominee(entity));
            return mine(entity);
        });
    }

    /** privacy.RevokeMyNominee: revokes the caller's active nominee (404 when none; 412 on a stale etag). */
    public void revokeMyNominee(RevokeMyNomineeRequest request) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        UUID principal = callerPerson(caller);
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        runInWriteTransaction(() -> {
            OffsetDateTime now = now();
            subjects.lockStatus(principal, now);
            NomineeEntity active = repository.findActive(principal)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, principal));
            if (expected.isPresent() && expected.getAsInt() != active.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the active nominee.");
            }
            revoke(active, now);
        });
    }

    /** Revokes the active row under its row_version guard (0 rows → 412), with its audit record and event. */
    private void revoke(NomineeEntity active, OffsetDateTime now) {
        UUID actor = Actor.current().orElse(null);
        if (repository.revoke(active.getId(), active.getRowVersion(), now, actor) == 0) {
            throw new PreconditionFailedException("The nominee changed since it was read.");
        }
        active.setStatus("revoked");
        active.setRevokedAt(now);
        audit.recordTransition(AGGREGATE, active.getId(), "revoke", List.of("status", "revokedAt"), null);
        outbox.publish(AGGREGATE + PrivacyEvents.REVOKED, AGGREGATE, active.getId(), PrivacyEvents.nominee(active));
    }

    /** The DTO with the name and contact masked; the plaintext opens in memory only. */
    private MyNominee mine(NomineeEntity entity) {
        MyNominee dto = mapper.toMine(entity);
        dto.setNomineeNameMasked(cipher.maskStored(entity.getNomineeNameEnc(), entity.getTenantId(), SECRET_NAME));
        dto.setNomineeContactMasked(
                cipher.maskStored(entity.getNomineeContactEnc(), entity.getTenantId(), SECRET_CONTACT));
        return dto;
    }

    /** Only an adult names a nominee: a minor's rights are their legal guardian's to exercise (unknown = minor). */
    private static void requireAdult(String status) {
        if (!Majority.ADULT.equals(status)) {
            throw new PermissionDeniedException("Only an adult names a nominee.");
        }
    }

    /** The caller's person (the token's pid): a nominee is always a person's (403 without one). */
    private static UUID callerPerson(AuthContext caller) {
        return caller.personId()
                .orElseThrow(() -> new PermissionDeniedException("The caller's account is linked to no person."));
    }

    /** Support staff acting as a user never name or revoke a nominee. */
    private static void refuseImpersonation(AuthContext caller) {
        if (caller.impersonatorId().isPresent()) {
            throw new PermissionDeniedException("A nominee cannot be named or revoked while acting as another user.");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static UUID tenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }
}
