package com.mysms.people.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.RateLimitedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.validation.DtoValidator;
import com.mysms.people.api.model.BirthdayEntry;
import com.mysms.people.api.model.SendGreetingsRequest;
import com.mysms.people.api.model.SendResult;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.config.PeopleEventsConfig;
import com.mysms.people.domain.GreetingEntity;
import com.mysms.people.repository.GreetingRepository;
import com.mysms.people.repository.GreetingTemplateRepository;
import com.mysms.people.repository.MemberLinkRepository;
import com.mysms.people.repository.MemberLinkRepository.Member;
import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Birthdays and work anniversaries of an institution (people.ListBirthdays) and greetings for them
 * (people.SendGreetings). Only the members of the caller's institutions are listed or greeted, and only what a
 * directory shows: the name and the day of the occasion — never the year of birth, the age or any contact detail
 * (years are given for work anniversaries only). Greetings are delivered by comms from the
 * {@code people.greeting.requested} event; people may not call comms, which depends on it.
 */
@Service
public class GreetingService extends AppTransactionManager {

    public static final String AGGREGATE = GreetingEntity.TABLE;
    /** The event comms delivers a greeting from (ids, the template and the channel only). */
    public static final String REQUESTED = AGGREGATE + ".requested";
    static final String PERMISSION_LIST = "people.person.list_birthdays";
    static final String PERMISSION_SEND = "people.person.send_greetings";
    /** The longest window one listing covers (422 beyond). */
    static final int MAX_WINDOW_DAYS = 62;
    /** The most persons one request greets, and the most greetings one sender asks for in a day (429 beyond). */
    static final int MAX_PER_REQUEST = 100;
    static final int MAX_PER_DAY = 300;
    private static final Set<String> KINDS = Set.of("birthday", "work_anniversary");
    private static final Set<String> ROLES = Set.of("student", "staff", "guardian");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMdd");
    private static final String OFFSET_CURSOR = "o";

    private final MemberLinkRepository members;
    private final GreetingRepository greetings;
    private final GreetingTemplateRepository templates;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public GreetingService(MemberLinkRepository members, GreetingRepository greetings,
            GreetingTemplateRepository templates,
            @Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PeopleDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PeopleEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PeopleEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.members = members;
        this.greetings = greetings;
        this.templates = templates;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * The birthdays and work anniversaries of an institution's students, staff and guardians between two days (at
     * most 62), by date then name, page by page. An institution outside the caller's reach is 404.
     */
    public Page<BirthdayEntry> listBirthdays(UUID institutionId, LocalDate from, LocalDate to, List<String> kinds,
            List<String> roles, Integer pageNumber, Integer pageSize, String cursor) {
        List<FieldError> errors = new ArrayList<>();
        if (to.isBefore(from)) {
            errors.add(new FieldError("toDate", "must not be before fromDate", "DATE_RANGE"));
        } else if (ChronoUnit.DAYS.between(from, to) >= MAX_WINDOW_DAYS) {
            errors.add(new FieldError("toDate", "at most " + MAX_WINDOW_DAYS + " days at a time", "DATE_RANGE"));
        }
        Set<String> wantedKinds = chosen(kinds, KINDS, "kinds", errors);
        Set<String> wantedRoles = chosen(roles, ROLES, "roles", errors);
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        int page = pageNumber == null ? 1 : pageNumber;
        int size = pageSize == null ? 50 : pageSize;
        int offset = offset(cursor, page, size);
        if (!DataScope.current(PERMISSION_LIST).canRead(Optional.of(institutionId))) {
            throw new ResourceNotFoundException("org.institution", institutionId);
        }
        boolean birthdays = wantedKinds.contains("birthday");
        boolean anniversaries = wantedKinds.contains("work_anniversary");
        List<Member> found = doInReadTransaction(() -> {
            List<Member> rows = new ArrayList<>();
            for (String[] span : monthDaySpans(from, to)) {
                rows.addAll(members.members(institutionId, List.copyOf(wantedRoles), birthdays, anniversaries,
                        span[0], span[1]));
            }
            return rows;
        });
        List<BirthdayEntry> entries = new ArrayList<>(entries(found, from, to, birthdays, anniversaries));
        entries.sort(Comparator.comparing(BirthdayEntry::getDate).thenComparing(BirthdayEntry::getDisplayName)
                .thenComparing(entry -> entry.getPersonId().toString()).thenComparing(entry -> entry.getKind().getValue())
                .thenComparing(entry -> entry.getRole().getValue()));
        int end = Math.min(entries.size(), offset + size);
        List<BirthdayEntry> slice = offset >= entries.size() ? List.of() : entries.subList(offset, end);
        return new Page<>(slice, page, size, end < entries.size() ? OFFSET_CURSOR + end : null);
    }

    /**
     * Asks comms to greet up to 100 persons on a channel with a template. Each must exist and belong to one of the
     * caller's institutions (a tenant-wide grant reaches every person; 422 {@code personIds[i]} otherwise, the same for
     * an unknown and an unreachable person), and the template must be one comms delivers — active, tenant-wide or of
     * a school the caller reaches (422 {@code templateId}). A person is greeted at most once a day on a channel (later
     * asks are skipped), and a sender asks for at most 300 greetings a day (429); a greeting comms could not deliver
     * is taken back and counts for neither.
     */
    public SendResult sendGreetings(SendGreetingsRequest request) {
        validator.validate(request);
        List<UUID> personIds = List.copyOf(new LinkedHashSet<>(request.getPersonIds()));
        if (personIds.isEmpty() || personIds.size() > MAX_PER_REQUEST) {
            throw new ValidationFailedException(List.of(new FieldError("personIds",
                    "name between 1 and " + MAX_PER_REQUEST + " persons", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_SEND);
        UUID sender = AuthContext.require().userId();
        String channel = request.getChannel().getValue();
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return doInWriteTransaction(() -> {
            List<FieldError> outside = new ArrayList<>();
            Set<UUID> live = greetings.livePersons(personIds);
            for (int i = 0; i < personIds.size(); i++) {
                if (!live.contains(personIds.get(i)) || !reaches(scope, personIds.get(i))) {
                    outside.add(new FieldError("personIds[" + i + "]", "person is not in the caller's institutions",
                            "NOT_FOUND"));
                }
            }
            boolean usable = templates.usable(request.getTemplateId())
                    .filter(template -> template.institutionId().map(i -> scope.canRead(Optional.of(i))).orElse(true))
                    .isPresent();
            if (!usable) {
                outside.add(new FieldError("templateId", "is not a template comms delivers in the caller's school",
                        "NOT_FOUND"));
            }
            if (!outside.isEmpty()) {
                throw new ValidationFailedException(outside);
            }
            Set<UUID> greeted = greetings.greeted(personIds, today, channel);
            List<UUID> fresh = personIds.stream().filter(person -> !greeted.contains(person)).toList();
            if (greetings.sentBy(sender, today) + fresh.size() > MAX_PER_DAY) {
                throw new RateLimitedException("At most " + MAX_PER_DAY + " greetings a day; try again tomorrow.");
            }
            int queued = 0;
            for (UUID person : fresh) {
                GreetingEntity greeting = new GreetingEntity();
                greeting.onCreatingData(Actor.current(), clock);
                greeting.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
                greeting.setPersonId(person);
                greeting.setGreetedOn(today);
                greeting.setChannel(channel);
                if (greetings.insert(greeting)) {
                    queued++;
                    audit.recordCreate(AGGREGATE, greeting.getId());
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", greeting.getId().toString());
                    payload.put("personId", person.toString());
                    payload.put("templateId", request.getTemplateId().toString());
                    payload.put("channel", channel);
                    outbox.publish(REQUESTED, AGGREGATE, greeting.getId(), payload);
                }
            }
            return new SendResult().queued(queued).skipped(personIds.size() - queued);
        });
    }

    /**
     * Takes back a greeting comms could not deliver (people.greeting soft-deleted, audited), so it no longer counts
     * for the person's day or the sender's allowance; whether it was still counted. Joins the inbox transaction.
     */
    public boolean withdrawGreeting(UUID greetingId) {
        return doInWriteTransaction(() -> {
            if (!greetings.withdraw(greetingId, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS))) {
                return false;
            }
            audit.recordDelete(AGGREGATE, greetingId);
            return true;
        });
    }

    /**
     * Whether the scope reaches the person through one of the person's institutions (persons are tenant-level rows,
     * so the institutions come from people.member_link); a tenant-wide grant reaches everyone.
     */
    private boolean reaches(DataScope scope, UUID personId) {
        return scope.all() || members.institutionsOf(personId).stream().anyMatch(i -> scope.canRead(Optional.of(i)));
    }

    /** The occasions of the members that fall in the window, one entry per person, occasion and role. */
    private static List<BirthdayEntry> entries(List<Member> found, LocalDate from, LocalDate to, boolean birthdays,
            boolean anniversaries) {
        Map<String, BirthdayEntry> entries = new LinkedHashMap<>();
        for (Member member : found) {
            BirthdayEntry.RoleEnum role = BirthdayEntry.RoleEnum.fromValue(member.role());
            if (birthdays && member.dateOfBirth() != null) {
                occurrence(member.dateOfBirth(), from, to).ifPresent(day -> entries.putIfAbsent(
                        member.personId() + "/birthday/" + role, new BirthdayEntry(member.personId(),
                                member.displayName(), day, BirthdayEntry.KindEnum.BIRTHDAY, role)));
            }
            if (anniversaries && member.joinedOn() != null) {
                occurrence(member.joinedOn(), from, to).filter(day -> day.getYear() > member.joinedOn().getYear())
                        .ifPresent(day -> entries.putIfAbsent(member.personId() + "/work_anniversary/" + role,
                                new BirthdayEntry(member.personId(), member.displayName(), day,
                                        BirthdayEntry.KindEnum.WORK_ANNIVERSARY, role)
                                        .years(day.getYear() - member.joinedOn().getYear())));
            }
        }
        return List.copyOf(entries.values());
    }

    /** The day in the window that falls on the date's month and day (29 February on the 28th in other years). */
    static Optional<LocalDate> occurrence(LocalDate date, LocalDate from, LocalDate to) {
        MonthDay monthDay = MonthDay.from(date);
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            LocalDate day = monthDay.isValidYear(year) ? monthDay.atYear(year) : LocalDate.of(year, 2, 28);
            if (!day.isBefore(from) && !day.isAfter(to)) {
                return Optional.of(day);
            }
        }
        return Optional.empty();
    }

    /** The window as month-day spans ({@code MMDD}): one, or two when it crosses the year's end. */
    static List<String[]> monthDaySpans(LocalDate from, LocalDate to) {
        String start = from.format(MONTH_DAY);
        String end = widened(to);
        if (from.getYear() == to.getYear()) {
            return List.<String[]>of(new String[] {start, end});
        }
        return List.of(new String[] {start, "1231"}, new String[] {"0101", end});
    }

    /** A window that ends on 28 February of a common year also takes in the birthdays of 29 February. */
    private static String widened(LocalDate to) {
        boolean commonYearFebruaryEnd = to.getMonthValue() == 2 && to.getDayOfMonth() == 28 && !to.isLeapYear();
        return commonYearFebruaryEnd ? "0229" : to.format(MONTH_DAY);
    }

    private static Set<String> chosen(List<String> given, Set<String> allowed, String field, List<FieldError> errors) {
        if (given == null || given.isEmpty()) {
            return allowed;
        }
        Set<String> chosen = new LinkedHashSet<>();
        for (String value : given) {
            for (String part : value.split(",")) {
                String trimmed = part.trim();
                if (!allowed.contains(trimmed)) {
                    errors.add(new FieldError(field, "must be one of " + String.join(", ", allowed.stream().sorted()
                            .toList()), "ENUM"));
                    return allowed;
                }
                chosen.add(trimmed);
            }
        }
        return chosen;
    }

    /** Where the page starts: the cursor of a next link, or the page number. */
    private static int offset(String cursor, int page, int size) {
        if (cursor == null || cursor.isEmpty()) {
            return (page - 1) * size;
        }
        try {
            if (!cursor.startsWith(OFFSET_CURSOR)) {
                throw new NumberFormatException(cursor);
            }
            int offset = Integer.parseInt(cursor.substring(OFFSET_CURSOR.length()));
            if (offset < 0) {
                throw new NumberFormatException(cursor);
            }
            return offset;
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("The cursor is not one this listing issued.");
        }
    }
}
