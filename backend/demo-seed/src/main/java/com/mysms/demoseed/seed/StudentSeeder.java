package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import com.mysms.demoseed.seed.DemoPeople.Name;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * sis (with the people records): the students of every class section, each with a guardian of the same family,
 * enrolled in their class for the current year with a roll number.
 */
public final class StudentSeeder implements ModuleSeeder {

    private static final List<String> PROFESSIONS =
            List.of("business", "private_sector", "state_government", "central_government", "teacher", "home_maker");

    @Override
    public String module() {
        return "sis";
    }

    /** The group of a class section's students, in roll-number order. */
    public static String studentsOf(int grade, String section) {
        return "students:" + grade + "-" + section;
    }

    /** The key a student's guardian is remembered under. */
    public static String guardianOf(JsonNode student) {
        return "guardianOf:" + student.path("id").asString();
    }

    @Override
    public void seed(SeedContext context) {
        String institution = context.id(OrgSeeder.INSTITUTION);
        String year = context.id(OrgSeeder.YEAR);
        LocalDate yearStart = context.calendar().yearStart();
        int admitted = 0;
        for (int grade = 1; grade <= AcademicStructureSeeder.GRADES; grade++) {
            for (String section : AcademicStructureSeeder.SECTIONS) {
                String classSection = context.id(AcademicStructureSeeder.classKey(grade, section));
                for (int roll = 1; roll <= context.settings().studentsPerClass(); roll++) {
                    admitted++;
                    String admission = String.format("SV%d-%04d", yearStart.getYear(), admitted);
                    Name name = DemoPeople.name(context.random());
                    JsonNode student = student(context, institution, admission, grade, name);
                    context.add(studentsOf(grade, section), student);
                    context.remember(guardianOf(student), guardian(context, student, admission, name.family()));
                    context.ensure(module(), "studentEnrollment", "/v1/sis/student-enrollments",
                            Filter.and(Filter.eq("studentId", student.path("id").asString()),
                                    Filter.eq("academicYearId", year)),
                            context.object().put("studentId", student.path("id").asString())
                                    .put("academicYearId", year).put("classSectionId", classSection)
                                    .put("rollNumber", String.valueOf(roll)).put("enrollmentStatus", "enrolled")
                                    .put("changeReason", "initial").put("effectiveFrom", yearStart.toString())
                                    .put("enrolledOn", yearStart.toString()));
                }
            }
        }
    }

    private JsonNode student(SeedContext context, String institution, String admission, int grade, Name name) {
        LocalDate birth = LocalDate.of(context.calendar().yearStart().getYear() - 5 - grade,
                1 + context.random().nextInt(12), 1 + context.random().nextInt(28));
        LocalDate admitted = context.calendar().yearStart().minusYears(context.random().nextInt(grade));
        JsonNode person = DemoPeople.person(context, name, birth, name.email(admission));
        return context.ensure(module(), "student", "/v1/sis/students", Filter.eq("admissionNumber", admission),
                context.object().put("personId", person.path("id").asString()).put("institutionId", institution)
                        .put("admissionNumber", admission).put("admissionDate", admitted.toString())
                        .put("joiningDate", admitted.toString()));
    }

    private JsonNode guardian(SeedContext context, JsonNode student, String admission, String family) {
        Name name = DemoPeople.name(context.random(), family,
                context.random().nextBoolean() ? "female" : "male");
        LocalDate birth = LocalDate.of(1975 + context.random().nextInt(15), 1 + context.random().nextInt(12),
                1 + context.random().nextInt(28));
        String email = name.email("p" + admission);
        JsonNode person = DemoPeople.person(context, name, birth, email);
        JsonNode guardian = context.ensure(module(), "guardian", "/v1/sis/guardians",
                Filter.eq("personId", person.path("id").asString()),
                context.object().put("personId", person.path("id").asString()).put("email", email)
                        .put("profession", PROFESSIONS.get(context.random().nextInt(PROFESSIONS.size()))));
        String studentId = student.path("id").asString();
        context.ensure(module(), "studentGuardian", "/v1/sis/students/" + studentId + "/guardians",
                Filter.eq("guardianId", guardian.path("id").asString()),
                context.object().put("studentId", studentId).put("guardianId", guardian.path("id").asString())
                        .put("isPrimaryContact", true).put("receivesCommunication", true).put("canPickUp", true)
                        .put("isLegalGuardian", true).put("hasCustody", true));
        return guardian;
    }
}
