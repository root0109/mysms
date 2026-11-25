package com.mysms.demoseed.seed;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The tenant roles the demo personas hold, each a set of permission-code patterns resolved against iam's catalogue:
 * {@code *} matches any run of characters, and a pattern starting with {@code !} takes matches away again. The
 * school administrator holds iam's own {@code administrator} role instead, at the school only. Whole modules are
 * granted where the persona runs them, because iam compacts a complete set into one claim ({@code sis.*}) and a token
 * listing hundreds of single permissions outgrows the request headers.
 */
public enum DemoRole {

    PRINCIPAL("demo_principal", "Principal", "academics.*", "sis.*", "attendance.*", "assessment.*", "timetable.*",
            "admission.*", "comms.*", "frontoffice.*", "hr.*", "lms.*", "facility.*", "finance.*.read",
            "library.*.read", "transport.*.read", "payroll.*.read", "org.*.read", "people.person.read"),
    TEACHER("demo_teacher", "Teacher", "academics.*.read", "academics.lesson_plan.*", "org.*.read", "hr.staff.read",
            "people.person.read", "sis.student.read", "sis.student.get_student_summary", "sis.guardian.read",
            "sis.student_guardian.read", "sis.student_enrollment.*", "!sis.student_enrollment.create",
            "!sis.student_enrollment.update", "!sis.student_enrollment.delete", "timetable.*.read",
            "timetable.session.get_timetable", "attendance.*", "assessment.*", "!assessment.*.approve*",
            "!assessment.*.reject*", "lms.*", "comms.notice.read", "library.*.read", "facility.*.read",
            "!academics.lesson_plan.approve*", "!academics.lesson_plan.reject*"),
    CLASS_TEACHER("demo_class_teacher", "Class teacher", "sis.student.update", "sis.student_leave_request.*",
            "sis.discipline_incident.*", "attendance.*"),
    ACCOUNTANT("demo_accountant", "Accountant", "finance.*", "payroll.*", "sis.student.read", "sis.guardian.read",
            "sis.student_guardian.read", "sis.student_enrollment.read", "people.person.read", "org.*.read",
            "academics.*.read", "hr.staff.read"),
    LIBRARIAN("demo_librarian", "Librarian", "library.*", "sis.student.read", "hr.staff.read", "people.person.read",
            "org.*.read", "academics.*.read"),
    STUDENT("student", "Student", "sis.student.get_student_summary", "timetable.session.get_timetable",
            "assessment.homework.read", "assessment.homework_submission.*", "lms.*.read", "comms.notice.read",
            "library.book.read", "academics.subject.read", "org.institution.read", "org.academic_year.read"),
    GUARDIAN("guardian", "Guardian", "sis.guardian.list_my_children", "sis.student.get_student_summary",
            "sis.student_leave_request.create", "timetable.session.get_timetable", "assessment.homework.read",
            "finance.invoice.read", "finance.invoice.get_fee_statement", "comms.notice.read");

    private final String code;
    private final String displayName;
    private final List<Pattern> included;
    private final List<Pattern> excluded;

    DemoRole(String code, String displayName, String... patterns) {
        this.code = code;
        this.displayName = displayName;
        this.included = Stream.of(patterns).filter(p -> !p.startsWith("!")).map(DemoRole::glob).toList();
        this.excluded = Stream.of(patterns).filter(p -> p.startsWith("!")).map(p -> glob(p.substring(1))).toList();
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    /** Whether the role holds the permission with this code: an exclusion wins over any inclusion. */
    public boolean grants(String permission) {
        return included.stream().anyMatch(p -> p.matcher(permission).matches())
                && excluded.stream().noneMatch(p -> p.matcher(permission).matches());
    }

    private static Pattern glob(String pattern) {
        return Pattern.compile(Stream.of(pattern.split("\\*", -1)).map(Pattern::quote)
                .collect(Collectors.joining(".*")));
    }
}
