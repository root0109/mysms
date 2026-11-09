package com.mysms.people.business;

import com.mysms.authclient.AuthContext;
import com.mysms.contracts.people.model.MyPersonUpdate;
import com.mysms.contracts.people.model.Person;
import com.mysms.core.context.Actor;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.config.PeopleEventsConfig;
import com.mysms.people.domain.SelfPersonEntity;
import com.mysms.people.events.PeopleEvents;
import com.mysms.people.mapper.SelfPersonMapper;
import com.mysms.people.repository.SelfPersonRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * The caller's own person record (people.GetMyPerson, people.UpdateMyPerson). Data rule: the record is the one the
 * token's `pid` claim names — never one chosen by the request. Users change only contact and self-description fields;
 * administrators' internal remarks are not shown to the person.
 */
@Service
public class MyPersonService {

    public static final String AGGREGATE = SelfPersonEntity.TABLE;
    /** The contract's MyPersonUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("primaryEmail", "primaryPhone", "alternatePhone", "workPhone",
            "websiteUrl", "aboutMe", "motherTongue");
    private static final int ABOUT_ME_MAX = 2000;
    private static final String STALE = "If-Match does not match the current ETag.";

    private final SelfPersonRepository repository;
    private final SelfPersonMapper mapper;
    private final ModuleDatabase database;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MyPersonService(SelfPersonRepository repository, SelfPersonMapper mapper,
                           @Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database,
                           @Qualifier(PeopleEventsConfig.OUTBOX) OutboxWriter outbox,
                           @Qualifier(PeopleEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator,
                           Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.database = database;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Person get() {
        UUID person = callerPerson();
        return database.tx().read(() -> selfView(load(person)));
    }

    /** Partial update under If-Match; with an updateMask the listed fields are set (absent ones cleared). */
    public Person update(String ifMatch, MyPersonUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        validator.validate(changes);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        UUID person = callerPerson();
        return database.tx().write(() -> {
            SelfPersonEntity entity = load(person);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException(STALE);
            }
            apply(entity, changes, fields);
            assertValid(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (!validateOnly) {
                if (repository.updateSelfEditable(entity, expected) == 0) {
                    throw new PreconditionFailedException(STALE);
                }
                entity.setRowVersion(expected + 1);
                audit.recordUpdate(AGGREGATE, person, fields);
                outbox.publish(AGGREGATE + PeopleEvents.UPDATED, AGGREGATE, person, PeopleEvents.payload(person));
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

    /** Explicit per field so a mask can clear values (no reflection). */
    private static void apply(SelfPersonEntity entity, MyPersonUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "primaryEmail" -> entity.setPrimaryEmail(trimToNull(changes.getPrimaryEmail()));
                case "primaryPhone" -> entity.setPrimaryPhone(trimToNull(changes.getPrimaryPhone()));
                case "alternatePhone" -> entity.setAlternatePhone(trimToNull(changes.getAlternatePhone()));
                case "workPhone" -> entity.setWorkPhone(trimToNull(changes.getWorkPhone()));
                case "websiteUrl" -> entity.setWebsiteUrl(trimToNull(changes.getWebsiteUrl()));
                case "aboutMe" -> entity.setAboutMe(trimToNull(changes.getAboutMe()));
                case "motherTongue" -> entity.setMotherTongue(trimToNull(changes.getMotherTongue()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
