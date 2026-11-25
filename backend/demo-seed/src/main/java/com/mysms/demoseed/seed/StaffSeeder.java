package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import com.mysms.demoseed.seed.DemoPeople.Name;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * hr (with their people records): the principal, twenty-two teachers — the first twenty each the class teacher of one
 * class section (primary teachers for grades 1–5, subject teachers in the secondary wing), the last two subject
 * teachers only — and the school administrator, office assistant, accountant and librarian.
 */
public final class StaffSeeder implements ModuleSeeder {

    public static final int TEACHERS = 22;
    /** Teachers 0…9 teach in the primary wing. */
    public static final int PRIMARY_TEACHERS = 10;
    /** Group of the teachers in order; teacher {@code i} is class teacher of the {@code i}-th class section. */
    public static final String TEACHER_GROUP = "teachers";
    /** The subject each secondary teacher (10…21) teaches. */
    public static final List<String> SECONDARY_SUBJECTS =
            List.of("ENG", "HIN", "MAT", "SCI", "SST", "COM", "MAT", "SCI", "ENG", "SST", "COM", "HIN");

    /** Designation code, name and staff category. */
    private static final String[][] DESIGNATIONS = {
            {"PRN", "Principal", "teaching"}, {"PRT", "Primary Teacher", "teaching"},
            {"TGT", "Trained Graduate Teacher", "teaching"}, {"ADM", "School Administrator", "non_teaching"},
            {"OFA", "Office Assistant", "non_teaching"}, {"ACC", "Accountant", "non_teaching"},
            {"LIB", "Librarian", "non_teaching"}};

    @Override
    public String module() {
        return "hr";
    }

    public static String staffKey(String role) {
        return "staff:" + role;
    }

    @Override
    public void seed(SeedContext context) {
        for (String[] designation : DESIGNATIONS) {
            context.remember("designation:" + designation[0], context.ensure(module(), "designation",
                    "/v1/hr/designations", Filter.eq("code", designation[0]),
                    context.object().put("code", designation[0]).put("name", designation[1])
                            .put("staffCategory", designation[2])));
        }
        context.remember(staffKey("principal"), staff(context, "SV-PRN-01", "PRN", "ADM", null));
        for (int i = 0; i < TEACHERS; i++) {
            boolean primary = i < PRIMARY_TEACHERS;
            String subject = primary ? null : SECONDARY_SUBJECTS.get(i - PRIMARY_TEACHERS);
            JsonNode teacher = staff(context, String.format("SV-T%02d", i + 1), primary ? "PRT" : "TGT",
                    primary ? "PRI" : "SEC", subject);
            context.add(TEACHER_GROUP, teacher);
            List<JsonNode> classes = context.group(AcademicStructureSeeder.CLASSES);
            if (i < classes.size()) {
                classTeacher(context, classes.get(i), teacher);
            }
        }
        context.remember(staffKey("admin"), staff(context, "SV-ADM-01", "ADM", "ADM", null));
        context.remember(staffKey("office"), staff(context, "SV-OFA-01", "OFA", "ADM", null));
        context.remember(staffKey("accountant"), staff(context, "SV-ACC-01", "ACC", "ACC", null));
        context.remember(staffKey("librarian"), staff(context, "SV-LIB-01", "LIB", "LIB", null));
    }

    private JsonNode staff(SeedContext context, String code, String designation, String department, String subject) {
        Name name = DemoPeople.name(context.random());
        LocalDate birth = LocalDate.of(1968 + context.random().nextInt(28), 1 + context.random().nextInt(12),
                1 + context.random().nextInt(28));
        LocalDate joined = context.calendar().yearStart().minusYears(context.random().nextInt(15)).plusDays(60);
        JsonNode person = DemoPeople.person(context, name, birth, name.email(code));
        var body = context.object().put("personId", person.path("id").asString())
                .put("institutionId", context.id(OrgSeeder.INSTITUTION)).put("employeeCode", code)
                .put("designationId", context.id("designation:" + designation))
                .put("primaryDepartmentId", context.id("department:" + department))
                .put("employmentType", "full_time").put("joiningDate", joined.toString()).put("maxPeriodsPerWeek", 30);
        if (subject != null) {
            body.put("specialization", AcademicStructureSeeder.SUBJECTS.stream()
                    .filter(s -> s.code().equals(subject)).findFirst().orElseThrow().name());
        }
        return context.ensure(module(), "staff", "/v1/hr/staff", Filter.eq("employeeCode", code), body);
    }

    /** Names the teacher on the class section, unless an earlier run did. */
    private static void classTeacher(SeedContext context, JsonNode classSection, JsonNode teacher) {
        String staffId = teacher.path("id").asString();
        if (staffId.equals(classSection.path("classTeacherStaffId").asString(null))) {
            return;
        }
        context.api().patch("/v1/academics/class-sections/" + classSection.path("id").asString(),
                context.object().put("classTeacherStaffId", staffId), classSection.path("etag").asString());
    }
}
