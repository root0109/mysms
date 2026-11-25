package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import tools.jackson.databind.JsonNode;

/** org: the demo school, its current academic year with two terms, and its departments. */
public final class OrgSeeder implements ModuleSeeder {

    public static final String INSTITUTION = "institution";
    public static final String YEAR = "academicYear";
    public static final String INSTITUTION_CODE = "SVPS";

    /** Department code and name; the staff seeder places people in them. */
    static final String[][] DEPARTMENTS = {
            {"PRI", "Primary Wing"}, {"SEC", "Secondary Wing"}, {"ADM", "Administration"}, {"ACC", "Accounts"},
            {"LIB", "Library"}};

    @Override
    public String module() {
        return "org";
    }

    @Override
    public void seed(SeedContext context) {
        JsonNode school = context.ensure(module(), "institution", "/v1/org/institutions",
                Filter.eq("code", INSTITUTION_CODE),
                context.object().put("code", INSTITUTION_CODE).put("name", "Sunrise Valley Public School")
                        .put("slug", "sunrise-valley").put("institutionType", "school")
                        .put("email", "office@" + DemoPeople.DOMAIN).put("addressLine1", "14 Lake View Road")
                        .put("timezone", "Asia/Kolkata").put("accreditation", "CBSE (demo)"));
        context.remember(INSTITUTION, school);
        String institution = school.path("id").asString();

        SchoolCalendar calendar = context.calendar();
        JsonNode year = context.ensure(module(), "academicYear", "/v1/org/academic-years",
                Filter.and(Filter.eq("institutionId", institution), Filter.eq("name", calendar.yearName())),
                context.object().put("institutionId", institution).put("name", calendar.yearName())
                        .put("startDate", calendar.yearStart().toString()).put("endDate", calendar.yearEnd().toString())
                        .put("termStructure", "two_semesters").put("isCurrent", true));
        context.remember(YEAR, year);
        String terms = "/v1/org/academic-years/" + year.path("id").asString() + "/terms";
        context.remember("term1", context.ensure(module(), "academicTerm", terms, Filter.eq("code", "T1"),
                context.object().put("academicYearId", year.path("id").asString()).put("code", "T1")
                        .put("name", "Term 1").put("sequence", 1).put("startDate", calendar.yearStart().toString())
                        .put("endDate", calendar.termOneEnd().toString())));
        context.remember("term2", context.ensure(module(), "academicTerm", terms, Filter.eq("code", "T2"),
                context.object().put("academicYearId", year.path("id").asString()).put("code", "T2")
                        .put("name", "Term 2").put("sequence", 2)
                        .put("startDate", calendar.termOneEnd().plusDays(1).toString())
                        .put("endDate", calendar.yearEnd().toString())));

        for (String[] department : DEPARTMENTS) {
            context.remember("department:" + department[0], context.ensure(module(), "department",
                    "/v1/org/departments",
                    Filter.and(Filter.eq("institutionId", institution), Filter.eq("code", department[0])),
                    context.object().put("institutionId", institution).put("code", department[0])
                            .put("name", department[1])));
        }
    }
}
