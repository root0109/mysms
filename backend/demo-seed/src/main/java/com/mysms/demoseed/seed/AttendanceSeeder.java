package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * attendance: each class's daily register, the absence reasons, and the roll call of every class in the first period
 * of each past school day — taken for the session, a few students absent or late, the sheet completed.
 */
public final class AttendanceSeeder implements ModuleSeeder {

    private static final String[][] REASONS = {{"SICK", "Unwell"}, {"FAMILY", "Family function"},
            {"TRAVEL", "Out of station"}};
    /** Out of a hundred: absent below the first bound, late below the second. */
    private static final int ABSENT = 6;
    private static final int LATE = 9;

    @Override
    public String module() {
        return "attendance";
    }

    @Override
    public void seed(SeedContext context) {
        String institution = context.id(OrgSeeder.INSTITUTION);
        for (int i = 0; i < REASONS.length; i++) {
            context.remember("absenceReason:" + REASONS[i][0], context.ensure(module(), "absenceReason",
                    "/v1/attendance/absence-reasons", Filter.eq("code", REASONS[i][0]),
                    context.object().put("code", REASONS[i][0]).put("title", REASONS[i][1]).put("sortOrder", i + 1)));
        }
        Map<String, List<JsonNode>> students = new HashMap<>();
        for (int grade = 1; grade <= AcademicStructureSeeder.GRADES; grade++) {
            for (String section : AcademicStructureSeeder.SECTIONS) {
                String classSection = context.id(AcademicStructureSeeder.classKey(grade, section));
                String code = "DAILY-" + grade + section;
                context.ensure(module(), "attendanceRegister", "/v1/attendance/attendance-registers",
                        Filter.and(Filter.eq("institutionId", institution), Filter.eq("code", code)),
                        context.object().put("institutionId", institution).put("code", code)
                                .put("name", "Daily attendance " + grade + "-" + section)
                                .put("registerScope", "class_section").put("classSectionId", classSection)
                                .put("isActive", true));
                students.put(classSection, context.group(StudentSeeder.studentsOf(grade, section)));
            }
        }
        for (JsonNode session : context.group(TimetableSeeder.ROLL_CALL)) {
            rollCall(context, session, students.get(session.path("classSectionId").asString()));
        }
    }

    private void rollCall(SeedContext context, JsonNode session, List<JsonNode> students) {
        ObjectNode request = context.object();
        ArrayNode marks = request.putArray("marks");
        for (JsonNode student : students) {
            int draw = context.random().nextInt(100);
            if (draw < ABSENT) {
                marks.addObject().put("studentId", student.path("id").asString()).put("status", "absent")
                        .put("absenceReasonId", context.id("absenceReason:"
                                + REASONS[context.random().nextInt(REASONS.length)][0]));
            } else if (draw < LATE) {
                marks.addObject().put("studentId", student.path("id").asString()).put("status", "late");
            }
        }
        request.put("complete", true);
        JsonNode sheet = context.api().post("/v1/attendance/attendance-sheets:forSession",
                context.object().put("timetableSessionId", session.path("id").asString()));
        context.count(module(), "attendanceSheet");
        if ("done".equals(sheet.path("status").asString())) {
            return;
        }
        String path = "/v1/attendance/attendance-sheets/" + sheet.path("id").asString();
        if (marks.isEmpty()) {
            // everyone present: the sheet opened with every student present, so it only needs completing
            context.api().post(path + ":complete", context.object());
        } else {
            context.api().post(path + ":recordAttendance", request);
        }
    }
}
