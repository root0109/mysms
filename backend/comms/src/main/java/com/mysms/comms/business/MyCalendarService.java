package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.CalendarItem;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.mapper.CalendarItemMapper;
import com.mysms.comms.repository.CalendarRow;
import com.mysms.comms.repository.MyCalendarRepository;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The caller's own calendar (comms.GetMyCalendar): events, occurrences and holidays comms holds, as
 * {@link MyCalendarRepository} admits them for the caller. Due dates, live classes and cohort sessions are lms's and
 * are not part of it (comms does not call lms).
 */
@Service
public class MyCalendarService extends AppTransactionManager {

    static final String PERMISSION_GET_MY_CALENDAR = "comms.event.get_my_calendar";
    /** The longest window one read covers. */
    static final Duration MAX_WINDOW = Duration.ofDays(366);
    /** The most items one export takes. */
    static final int MAX_EXPORTED = 5000;

    private final MyCalendarRepository repository;
    private final CalendarItemMapper mapper;
    private final Clock clock;

    public MyCalendarService(MyCalendarRepository repository, CalendarItemMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** One page of the caller's calendar between two instants (at most 366 days apart), in start order. */
    public Page<CalendarItem> getMyCalendar(OffsetDateTime from, OffsetDateTime to, UUID cohortId,
            List<String> itemTypes, Integer pageNumber, Integer pageSize, String cursor) {
        Set<CalendarItem.ItemTypeEnum> kinds = kinds(itemTypes);
        MyCalendarRepository.Window window = window(from, to, cohortId, kinds, PERMISSION_GET_MY_CALENDAR);
        SortSpec sort = SortSpec.parse(null, MyCalendarRepository.SORT_FIELDS, MyCalendarRepository.ORDER,
                MyCalendarRepository.ID_COLUMN);
        String view = "calendar|" + window.me() + "|" + from.toInstant() + "|" + to.toInstant() + "|" + cohortId
                + "|" + kinds;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, view, clock);
        int skipped = ((Number) page.limitParams().get("offset")).intValue();
        int wanted = ((Number) page.limitParams().get("limit")).intValue();
        return doInReadTransaction(() -> {
            List<CalendarRow> rows = repository.read(window, page.keysetPredicate(), page.orderByClause(),
                    skipped + wanted);
            List<CalendarRow> shown = rows.subList(Math.min(skipped, rows.size()), rows.size());
            return page.toPage(shown, row -> List.of(row.startAt()), CalendarRow::id).map(mapper::toDto);
        });
    }

    /** Every item of the caller's calendar in the window (at most {@value #MAX_EXPORTED}), for an export. */
    List<CalendarItem> items(OffsetDateTime from, OffsetDateTime to, String permission) {
        MyCalendarRepository.Window window = window(from, to, null, EnumSet.allOf(CalendarItem.ItemTypeEnum.class),
                permission);
        return doInReadTransaction(() -> repository.readAll(window, MAX_EXPORTED)
                .stream().map(mapper::toDto).toList());
    }

    private MyCalendarRepository.Window window(OffsetDateTime from, OffsetDateTime to, UUID cohortId,
            Set<CalendarItem.ItemTypeEnum> kinds, String permission) {
        if (!to.isAfter(from) || Duration.between(from, to).compareTo(MAX_WINDOW) > 0) {
            throw new InvalidArgumentException("to must be after from, at most 366 days later.");
        }
        DataScope scope = DataScope.current(permission);
        return new MyCalendarRepository.Window(OwnRows.caller(), scope.all(), scope.institutionArray(), from, to,
                cohortId, kinds.contains(CalendarItem.ItemTypeEnum.EVENT),
                kinds.contains(CalendarItem.ItemTypeEnum.HOLIDAY), kinds.contains(CalendarItem.ItemTypeEnum.OCCURRENCE));
    }

    /** The kinds asked for (all when none is named); an unknown kind is 400. */
    private static Set<CalendarItem.ItemTypeEnum> kinds(List<String> itemTypes) {
        if (itemTypes == null || itemTypes.isEmpty()) {
            return EnumSet.allOf(CalendarItem.ItemTypeEnum.class);
        }
        Set<CalendarItem.ItemTypeEnum> kinds = EnumSet.noneOf(CalendarItem.ItemTypeEnum.class);
        for (String type : itemTypes) {
            try {
                kinds.add(CalendarItem.ItemTypeEnum.fromValue(type.trim()));
            } catch (IllegalArgumentException e) {
                throw new InvalidArgumentException("itemTypes names an unknown kind of item.");
            }
        }
        return kinds;
    }
}
