package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/**
 * timetable: six periods a day and the published (confirmed) sessions of every class, Monday to Saturday, for this
 * week and the past weeks attendance is taken for. A class's week repeats one pattern: each subject in turn, so every
 * subject gets the same share of periods, taught by the teacher the teaching assignments name.
 */
public final class TimetableSeeder implements ModuleSeeder {

    /** Group of the first-period sessions of past school days, the ones daily attendance is taken in. */
    public static final String ROLL_CALL = "rollCallSessions";

    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int MINUTES = 45;
    private static final List<LocalTime> STARTS = List.of(LocalTime.of(8, 0), LocalTime.of(8, 50),
            LocalTime.of(9, 40), LocalTime.of(10, 45), LocalTime.of(11, 35), LocalTime.of(12, 25));

    @Override
    public String module() {
        return "timetable";
    }

    @Override
    public void seed(SeedContext context) {
        String institution = context.id(OrgSeeder.INSTITUTION);
        for (int period = 1; period <= STARTS.size(); period++) {
            context.remember("period:" + period, context.ensure(module(), "period", "/v1/timetable/periods",
                    Filter.and(Filter.eq("institutionId", institution), Filter.eq("name", "Period " + period)),
                    context.object().put("institutionId", institution).put("name", "Period " + period)
                            .put("startTime", STARTS.get(period - 1).toString())
                            .put("durationMinutes", String.valueOf(MINUTES)).put("sequence", period)));
        }
        LocalDate monday = context.calendar().today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int week = context.settings().pastWeeks(); week >= 0; week--) {
            week(context, institution, monday.minusWeeks(week));
        }
    }

    private void week(SeedContext context, String institution, LocalDate monday) {
        Instant from = monday.atStartOfDay(SCHOOL_ZONE).toInstant();
        Instant to = monday.plusWeeks(1).atStartOfDay(SCHOOL_ZONE).toInstant();
        Map<String, JsonNode> existing = context.api().list("/v1/timetable/sessions", Filter.and(
                Filter.eq("institutionId", institution), "startAt >= \"" + from + "\"", "startAt < \"" + to + "\""))
                .stream().collect(Collectors.toMap(TimetableSeeder::slotKey, Function.identity(), (a, b) -> a));
        for (int day = 0; day < 6; day++) {
            LocalDate date = monday.plusDays(day);
            if (date.isBefore(context.calendar().yearStart())) {
                continue;
            }
            for (int grade = 1; grade <= AcademicStructureSeeder.GRADES; grade++) {
                List<AcademicStructureSeeder.Subject> subjects = TeachingSeeder.subjectsOf(grade);
                for (String section : AcademicStructureSeeder.SECTIONS) {
                    String classKey = AcademicStructureSeeder.classKey(grade, section);
                    for (int period = 1; period <= STARTS.size(); period++) {
                        AcademicStructureSeeder.Subject subject = subjects.get((day + period) % subjects.size());
                        JsonNode session = session(context, existing, institution, classKey, subject, date, period);
                        if (period == 1 && date.isBefore(context.calendar().today())) {
                            context.add(ROLL_CALL, session);
                        }
                    }
                }
            }
        }
    }

    private JsonNode session(SeedContext context, Map<String, JsonNode> existing, String institution,
                             String classKey, AcademicStructureSeeder.Subject subject, LocalDate date, int period) {
        String classSection = context.id(classKey);
        Instant start = date.atTime(STARTS.get(period - 1)).atZone(SCHOOL_ZONE).toInstant();
        JsonNode session = existing.get(classSection + "|" + start);
        if (session == null) {
            session = context.api().post("/v1/timetable/sessions", context.object().put("institutionId", institution)
                    .put("classSectionId", classSection).put("subjectId", context.id("subject:" + subject.code()))
                    .put("staffId", context.id(TeachingSeeder.teacherOf(classKey, subject.code())))
                    .put("periodId", context.id("period:" + period)).put("weekday", date.getDayOfWeek().getValue())
                    .put("startAt", start.atOffset(ZoneOffset.UTC).toString())
                    .put("endAt", start.plusSeconds(MINUTES * 60L).atOffset(ZoneOffset.UTC).toString())
                    .put("title", subject.name()));
        }
        if ("draft".equals(session.path("status").asString())) {
            String id = session.path("id").asString();
            session = context.api().post("/v1/timetable/sessions/" + id + ":confirm", context.object());
        }
        context.count(module(), "session");
        return session;
    }

    private static String slotKey(JsonNode session) {
        return session.path("classSectionId").asString() + "|" + Instant.parse(session.path("startAt").asString());
    }
}
