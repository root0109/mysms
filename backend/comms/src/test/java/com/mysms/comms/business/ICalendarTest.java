package com.mysms.comms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.comms.api.model.CalendarItem;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ICalendarTest {

    private static final OffsetDateTime STAMP = OffsetDateTime.parse("2026-03-01T10:15:30+05:30");

    private static CalendarItem item(CalendarItem.ItemTypeEnum type, String title, boolean allDay) {
        CalendarItem item = new CalendarItem();
        item.setItemType(type);
        item.setId(UUID.fromString("0196f000-0000-7000-8000-00000000f001"));
        item.setTitle(title);
        item.setAllDay(allDay);
        item.setStartAt(OffsetDateTime.parse("2026-03-10T09:00:00+05:30"));
        item.setEndAt(OffsetDateTime.parse("2026-03-10T10:30:00+05:30"));
        return item;
    }

    private static List<String> lines(byte[] file) {
        return Arrays.asList(new String(file, StandardCharsets.UTF_8).split("\r\n"));
    }

    @Test
    void writesOneEventPerItemInUtcWithAStableUid() {
        List<String> lines = lines(ICalendar.write(List.of(item(CalendarItem.ItemTypeEnum.EVENT, "Sports day",
                false)), STAMP));

        assertThat(lines).startsWith("BEGIN:VCALENDAR", "VERSION:2.0").endsWith("END:VCALENDAR");
        assertThat(lines).contains("UID:event-0196f000-0000-7000-8000-00000000f001@mysms",
                "DTSTAMP:20260301T044530Z", "DTSTART:20260310T033000Z", "DTEND:20260310T050000Z",
                "SUMMARY:Sports day");
        assertThat(lines).doesNotContain("STATUS:CANCELLED");
    }

    @Test
    void writesAnAllDayItemAsDatesAndACancelledOneAsCancelled() {
        CalendarItem holiday = item(CalendarItem.ItemTypeEnum.HOLIDAY, "Holi", true);
        holiday.setIsCancelled(true);

        List<String> lines = lines(ICalendar.write(List.of(holiday), STAMP));

        assertThat(lines).contains("DTSTART;VALUE=DATE:20260310", "DTEND;VALUE=DATE:20260311", "STATUS:CANCELLED");
        assertThat(ICalendar.uid(holiday)).startsWith("holiday-");
    }

    @Test
    void escapesTextAndFoldsLongLinesWithoutSplittingACharacter() {
        assertThat(ICalendar.text("A, B; C\\D\nE")).isEqualTo("A\\, B\\; C\\\\D\\nE");
        assertThat(ICalendar.text(null)).isEmpty();

        StringBuilder out = new StringBuilder();
        ICalendar.line(out, "SUMMARY:" + "भारत".repeat(10));
        String[] physical = out.toString().split("\r\n");
        assertThat(physical).hasSizeGreaterThan(1);
        for (String line : physical) {
            assertThat(line.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
        StringBuilder unfolded = new StringBuilder(physical[0]);
        for (int i = 1; i < physical.length; i++) {
            assertThat(physical[i]).startsWith(" ");
            unfolded.append(physical[i].substring(1));
        }
        assertThat(unfolded).hasToString("SUMMARY:" + "भारत".repeat(10));
    }
}
