package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * academics: grades 1 to 10 with sections A and B in the current year, the subjects of the primary (1–5) and
 * secondary (6–10) wings, and each class's subjects.
 */
public final class AcademicStructureSeeder implements ModuleSeeder {

    public static final int GRADES = 10;
    public static final List<String> SECTIONS = List.of("A", "B");
    /** Group of every class section, in grade then section order. */
    public static final String CLASSES = "classSections";
    /** Group of every class subject. */
    public static final String CLASS_SUBJECTS = "classSubjects";

    /** Subject code, name and the grades that take it. */
    static final List<Subject> SUBJECTS = List.of(
            new Subject("ENG", "English", 1, 10), new Subject("HIN", "Hindi", 1, 10),
            new Subject("MAT", "Mathematics", 1, 10), new Subject("EVS", "Environmental Studies", 1, 5),
            new Subject("ART", "Art and Craft", 1, 5), new Subject("SCI", "Science", 6, 10),
            new Subject("SST", "Social Science", 6, 10), new Subject("COM", "Computer Science", 1, 10));

    /** A subject and the grades (inclusive) that take it. */
    record Subject(String code, String name, int fromGrade, int toGrade) {

        boolean taughtIn(int grade) {
            return grade >= fromGrade && grade <= toGrade;
        }
    }

    @Override
    public String module() {
        return "academics";
    }

    /** The key a class section is remembered under, e.g. {@code class:7-B}. */
    public static String classKey(int grade, String section) {
        return "class:" + grade + "-" + section;
    }

    @Override
    public void seed(SeedContext context) {
        String institution = context.id(OrgSeeder.INSTITUTION);
        String year = context.id(OrgSeeder.YEAR);
        for (String section : SECTIONS) {
            context.remember("section:" + section, context.ensure(module(), "section", "/v1/academics/sections",
                    Filter.and(Filter.eq("institutionId", institution), Filter.eq("code", section)),
                    context.object().put("institutionId", institution).put("code", section)
                            .put("name", "Section " + section).put("sortOrder", SECTIONS.indexOf(section) + 1)));
        }
        for (Subject subject : SUBJECTS) {
            context.remember("subject:" + subject.code(), context.ensure(module(), "subject",
                    "/v1/academics/subjects",
                    Filter.and(Filter.eq("institutionId", institution), Filter.eq("code", subject.code())),
                    context.object().put("institutionId", institution).put("code", subject.code())
                            .put("name", subject.name()).put("deliveryType", "theory")
                            .put("defaultCategory", "compulsory")));
        }
        for (int grade = 1; grade <= GRADES; grade++) {
            JsonNode level = context.ensure(module(), "gradeLevel", "/v1/academics/grade-levels",
                    Filter.and(Filter.eq("institutionId", institution), Filter.eq("slug", "grade-" + grade)),
                    context.object().put("institutionId", institution).put("name", "Grade " + grade)
                            .put("slug", "grade-" + grade).put("sortOrder", grade));
            for (String section : SECTIONS) {
                JsonNode classSection = context.ensure(module(), "classSection", "/v1/academics/class-sections",
                        Filter.and(Filter.eq("academicYearId", year),
                                Filter.eq("gradeLevelId", level.path("id").asString()),
                                Filter.eq("sectionId", context.id("section:" + section))),
                        context.object().put("institutionId", institution).put("academicYearId", year)
                                .put("gradeLevelId", level.path("id").asString())
                                .put("sectionId", context.id("section:" + section)).put("capacity", 40));
                context.remember(classKey(grade, section), classSection);
                context.add(CLASSES, classSection);
                classSubjects(context, grade, section, classSection.path("id").asString());
            }
        }
    }

    private void classSubjects(SeedContext context, int grade, String section, String classSection) {
        String path = "/v1/academics/class-sections/" + classSection + "/subjects";
        for (Subject subject : SUBJECTS) {
            if (!subject.taughtIn(grade)) {
                continue;
            }
            String subjectId = context.id("subject:" + subject.code());
            JsonNode classSubject = context.ensure(module(), "classSubject", path, Filter.eq("subjectId", subjectId),
                    context.object().put("classSectionId", classSection).put("subjectId", subjectId)
                            .put("offeringType", "core"));
            context.remember(classKey(grade, section) + ":" + subject.code(), classSubject);
            context.add(CLASS_SUBJECTS, classSubject);
        }
    }
}
