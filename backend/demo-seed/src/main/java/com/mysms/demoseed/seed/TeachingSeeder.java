package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * academics: who teaches what. In grades 1–5 the class teacher teaches every subject of the class; in grades 6–10
 * each subject goes to one of the secondary teachers of that subject, spread over the classes.
 */
public final class TeachingSeeder implements ModuleSeeder {

    /** Periods a class has in a week: six a day, Monday to Saturday. */
    static final int PERIODS_PER_WEEK = 36;

    @Override
    public String module() {
        return "academics";
    }

    /** The key under which the teacher of a class subject is remembered. */
    public static String teacherOf(String classKey, String subjectCode) {
        return "teacherOf:" + classKey + ":" + subjectCode;
    }

    @Override
    public void seed(SeedContext context) {
        List<JsonNode> teachers = context.group(StaffSeeder.TEACHER_GROUP);
        int classIndex = 0;
        for (int grade = 1; grade <= AcademicStructureSeeder.GRADES; grade++) {
            for (String section : AcademicStructureSeeder.SECTIONS) {
                String classKey = AcademicStructureSeeder.classKey(grade, section);
                List<AcademicStructureSeeder.Subject> subjects = subjectsOf(grade);
                for (AcademicStructureSeeder.Subject subject : subjects) {
                    JsonNode teacher = grade <= 5 ? teachers.get(classIndex)
                            : secondaryTeacher(teachers, subject.code(), grade + AcademicStructureSeeder.SECTIONS
                                    .indexOf(section));
                    JsonNode classSubject = context.row(classKey + ":" + subject.code());
                    context.ensure(module(), "teachingAssignment", "/v1/academics/teaching-assignments",
                            Filter.and(Filter.eq("classSubjectId", classSubject.path("id").asString()),
                                    Filter.eq("staffId", teacher.path("id").asString())),
                            context.object().put("classSubjectId", classSubject.path("id").asString())
                                    .put("staffId", teacher.path("id").asString())
                                    .put("periodsPerWeek", PERIODS_PER_WEEK / subjects.size())
                                    .put("teacherRole", "subject_teacher"));
                    context.remember(teacherOf(classKey, subject.code()), teacher);
                }
                classIndex++;
            }
        }
    }

    /** The subjects a grade takes, in the order of the subject list. */
    static List<AcademicStructureSeeder.Subject> subjectsOf(int grade) {
        return AcademicStructureSeeder.SUBJECTS.stream().filter(subject -> subject.taughtIn(grade)).toList();
    }

    private static JsonNode secondaryTeacher(List<JsonNode> teachers, String subject, int spread) {
        List<JsonNode> candidates = new ArrayList<>();
        for (int i = 0; i < StaffSeeder.SECONDARY_SUBJECTS.size(); i++) {
            if (StaffSeeder.SECONDARY_SUBJECTS.get(i).equals(subject)) {
                candidates.add(teachers.get(StaffSeeder.PRIMARY_TEACHERS + i));
            }
        }
        return candidates.get(spread % candidates.size());
    }
}
