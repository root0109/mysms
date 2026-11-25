package com.mysms.comms.business;

import com.mysms.comms.api.model.CalendarItem;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Writes calendar items as an iCalendar file (RFC 5545): one VEVENT per item, its UID stable across exports so a
 * calendar that imports the file again updates the event instead of duplicating it, times in UTC, all-day items as
 * dates, cancelled occurrences marked cancelled; text escaped and lines folded at 75 octets.
 */
final class ICalendar {

    static final String MIME_TYPE = "text/calendar";
    private static final String CRLF = "\r\n";
    private static final int LINE_OCTETS = 75;
    private static final DateTimeFormatter INSTANT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ICalendar() {
    }

    /** The UID an item is exported under. */
    static String uid(CalendarItem item) {
        return item.getItemType().getValue() + "-" + item.getId() + "@mysms";
    }

    /** The file, UTF-8. */
    static byte[] write(List<CalendarItem> items, OffsetDateTime stamp) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//MySMS//Calendar export//EN");
        line(out, "CALSCALE:GREGORIAN");
        for (CalendarItem item : items) {
            line(out, "BEGIN:VEVENT");
            line(out, "UID:" + uid(item));
            line(out, "DTSTAMP:" + utc(stamp).format(INSTANT));
            boolean allDay = Boolean.TRUE.equals(item.getAllDay());
            OffsetDateTime start = Objects.requireNonNull(item.getStartAt(), "a calendar item starts");
            line(out, allDay ? "DTSTART;VALUE=DATE:" + utc(start).format(DATE)
                    : "DTSTART:" + utc(start).format(INSTANT));
            OffsetDateTime end = item.getEndAt();
            if (end != null) {
                line(out, allDay ? "DTEND;VALUE=DATE:" + utc(end).plusDays(1).format(DATE)
                        : "DTEND:" + utc(end).format(INSTANT));
            }
            line(out, "SUMMARY:" + text(item.getTitle()));
            if (Boolean.TRUE.equals(item.getIsCancelled())) {
                line(out, "STATUS:CANCELLED");
            }
            line(out, "END:VEVENT");
        }
        line(out, "END:VCALENDAR");
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static OffsetDateTime utc(OffsetDateTime time) {
        return time.withOffsetSameInstant(ZoneOffset.UTC);
    }

    /** TEXT values escape backslash, semicolon, comma and line breaks. */
    static String text(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }

    /** Appends a content line, folded so no physical line exceeds 75 octets (never inside a character). */
    static void line(StringBuilder out, String content) {
        int octets = 0;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            int width = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (octets + width > LINE_OCTETS) {
                out.append(CRLF).append(' ');
                octets = 1;
            }
            out.appendCodePoint(codePoint);
            octets += width;
            i += Character.charCount(codePoint);
        }
        out.append(CRLF);
    }
}
