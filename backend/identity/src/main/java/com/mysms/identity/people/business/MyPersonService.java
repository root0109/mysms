package com.mysms.identity.people.business;

import com.mysms.authclient.AuthContext;
import com.mysms.identity.people.api.model.MyPersonUpdate;
import com.mysms.identity.people.api.model.Person;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.identity.people.config.PeopleDatabaseConfig;
import com.mysms.identity.people.config.PeopleEventsConfig;
import com.mysms.identity.people.domain.SelfPersonEntity;
import com.mysms.identity.people.events.PeopleEvents;
import com.mysms.identity.people.mapper.SelfPersonMapper;
import com.mysms.identity.people.repository.SelfPersonRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The caller's own person record (people.GetMyPerson, people.UpdateMyPerson). Data rule: the record is the one the
 * token's `pid` claim names — never one chosen by the request. Users change only contact and self-description fields;
 * administrators' internal remarks are not shown to the person.
 */
@Service
public class MyPersonService extends AppTransactionManager {

    public static final String AGGREGATE = SelfPersonEntity.TABLE;
    private static final int ABOUT_ME_MAX = 2000;
    private static final String STALE = "If-Match does not match the current ETag.";

    private final SelfPersonRepository repository;
    private final SelfPersonMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MyPersonService(SelfPersonRepository repository, SelfPersonMapper mapper,
                           @Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PeopleDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                           @Qualifier(PeopleEventsConfig.OUTBOX) OutboxWriter outbox,
                           @Qualifier(PeopleEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator,
                           Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Person get() {
        UUID person = callerPerson();
        return doInReadTransaction(() -> {
            return selfView(load(person));
        });
    }

    /** Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one cleared, a value set. */
    public Person update(String ifMatch, MyPersonUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        validator.validate(changes);
        UUID person = callerPerson();
        return doInWriteTransaction(() -> {
            SelfPersonEntity entity = load(person);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException(STALE);
            }
            List<String> fields = apply(entity, changes);
            assertValid(entity);
            if (fields.isEmpty()) {
                return selfView(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (!validateOnly) {
                if (repository.updateSelfEditable(entity, expected) == 0) {
                    throw new PreconditionFailedException(STALE);
                }
                entity.setRowVersion(expected + 1);
                audit.recordUpdate(AGGREGATE, person, fields);
                outbox.publish(AGGREGATE + PeopleEvents.UPDATED, AGGREGATE, person,
                        PeopleEvents.personPayload(person, entity.getDateOfBirth(), entity.getRowVersion()));
            }
            return selfView(entity);
        });
    }

    private static UUID callerPerson() {
        return AuthContext.require().personId()
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, "me"));
    }

    private SelfPersonEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, "me"));
    }

    private Person selfView(SelfPersonEntity entity) {
        Person dto = mapper.toDto(entity);
        dto.setRemarks(null);
        return dto;
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(SelfPersonEntity entity, MyPersonUpdate changes) {
        Patches patch = new Patches();
        patch.apply("primaryEmail", changes.getPrimaryEmail(), v -> entity.setPrimaryEmail(trimToNull(v)));
        patch.apply("primaryPhone", changes.getPrimaryPhone(), v -> entity.setPrimaryPhone(trimToNull(v)));
        patch.apply("alternatePhone", changes.getAlternatePhone(), v -> entity.setAlternatePhone(trimToNull(v)));
        patch.apply("workPhone", changes.getWorkPhone(), v -> entity.setWorkPhone(trimToNull(v)));
        patch.apply("websiteUrl", changes.getWebsiteUrl(), v -> entity.setWebsiteUrl(trimToNull(v)));
        patch.apply("aboutMe", changes.getAboutMe(), v -> entity.setAboutMe(trimToNull(v)));
        patch.apply("motherTongue", changes.getMotherTongue(), v -> entity.setMotherTongue(trimToNull(v)));
        return patch.applied();
    }

    /** Shapes beyond the contract's lengths: an e-mail address, phone numbers, a web link that is http(s) only. */
    static void assertValid(SelfPersonEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        String email = entity.getPrimaryEmail();
        if (email != null && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            errors.add(new FieldError("primaryEmail", "must be an e-mail address", "Email"));
        }
        phone(entity.getPrimaryPhone(), "primaryPhone", errors);
        phone(entity.getAlternatePhone(), "alternatePhone", errors);
        phone(entity.getWorkPhone(), "workPhone", errors);
        if (entity.getWebsiteUrl() != null && !webLink(entity.getWebsiteUrl())) {
            errors.add(new FieldError("websiteUrl", "must be an http or https address", "URL"));
        }
        if (entity.getAboutMe() != null && entity.getAboutMe().length() > ABOUT_ME_MAX) {
            errors.add(new FieldError("aboutMe", "must be at most " + ABOUT_ME_MAX + " characters", "Size"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private static void phone(String value, String field, List<FieldError> errors) {
        if (value != null && !value.matches("\\+?[0-9(][0-9 ()-]{3,28}")) {
            errors.add(new FieldError(field, "must be a phone number", "Phone"));
        }
    }

    private static boolean webLink(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return ("http".equals(scheme) || "https".equals(scheme)) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
