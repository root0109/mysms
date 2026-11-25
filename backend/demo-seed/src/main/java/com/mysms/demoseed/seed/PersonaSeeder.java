package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * iam: the demo roles with their permissions, and one sign-in per persona — school administrator, principal,
 * teacher, class teacher, accountant, librarian, student and guardian — linked to that person's staff, student or
 * guardian record so each signs in to their own data. Passwords are set once, when the account is created.
 */
public final class PersonaSeeder implements ModuleSeeder {

    /** iam's system role holding every permission; the school administrator holds it at the school only. */
    static final String ADMINISTRATOR = "administrator";
    /** Index of the teacher who is class teacher of 10-A, and of a teacher with no class of their own. */
    static final int CLASS_TEACHER = 18;
    static final int SUBJECT_TEACHER = 20;

    /** A sign-in: who it is, the record it belongs to, and what it may do where. */
    private record Persona(String key, String username, String userType, JsonNode record, String recordPath,
                           List<Grant> grants) {
    }

    /** A role at a scope ({@code institution} or {@code class_section}). */
    private record Grant(String roleCode, String scopeType, String scopeId) {
    }

    @Override
    public String module() {
        return "iam";
    }

    @Override
    public void seed(SeedContext context) {
        roles(context);
        String school = context.id(OrgSeeder.INSTITUTION);
        List<JsonNode> teachers = context.group(StaffSeeder.TEACHER_GROUP);
        JsonNode student = context.group(StudentSeeder.studentsOf(AcademicStructureSeeder.GRADES, "A")).get(0);
        String tenthA = context.id(AcademicStructureSeeder.classKey(AcademicStructureSeeder.GRADES, "A"));
        List<Persona> personas = List.of(
                staff(context, "school administrator", "schooladmin", "admin", "admin", ADMINISTRATOR, school),
                staff(context, "principal", "principal", "principal", "staff", DemoRole.PRINCIPAL.code(), school),
                new Persona("teacher", "teacher", "teacher", teachers.get(SUBJECT_TEACHER), "/v1/hr/staff/",
                        List.of(new Grant(DemoRole.TEACHER.code(), "institution", school))),
                new Persona("class teacher", "classteacher", "teacher", teachers.get(CLASS_TEACHER), "/v1/hr/staff/",
                        List.of(new Grant(DemoRole.TEACHER.code(), "institution", school),
                                new Grant(DemoRole.CLASS_TEACHER.code(), "class_section", tenthA))),
                staff(context, "accountant", "accountant", "accountant", "staff", DemoRole.ACCOUNTANT.code(), school),
                staff(context, "librarian", "librarian", "librarian", "librarian", DemoRole.LIBRARIAN.code(), school),
                new Persona("student", "student", "student", student, "/v1/sis/students/",
                        List.of(new Grant(DemoRole.STUDENT.code(), "institution", school))),
                new Persona("guardian", "parent", "guardian", context.row(StudentSeeder.guardianOf(student)),
                        "/v1/sis/guardians/", List.of(new Grant(DemoRole.GUARDIAN.code(), "institution", school))));
        for (Persona persona : personas) {
            account(context, persona);
            context.persona(persona.key(), persona.username());
        }
    }

    private static Persona staff(SeedContext context, String key, String username, String staffRole, String userType,
                                 String role, String school) {
        return new Persona(key, username, userType, context.row(StaffSeeder.staffKey(staffRole)), "/v1/hr/staff/",
                List.of(new Grant(role, "institution", school)));
    }

    /** The demo roles, each holding exactly the catalogue's permissions its patterns select, no more. */
    private void roles(SeedContext context) {
        List<JsonNode> catalogue = context.api().list("/v1/iam/permissions", null);
        for (DemoRole role : DemoRole.values()) {
            JsonNode row = context.ensure(module(), "role", "/v1/iam/roles", Filter.eq("code", role.code()),
                    context.object().put("code", role.code()).put("displayName", role.displayName())
                            .put("description", "Demo role of the pilot school"));
            String path = "/v1/iam/roles/" + row.path("id").asString() + "/permissions";
            Map<String, JsonNode> held = context.api().list(path, null).stream().collect(Collectors.toMap(
                    permission -> permission.path("permissionId").asString(), permission -> permission));
            for (JsonNode permission : catalogue) {
                String id = permission.path("id").asString();
                if (!role.grants(permission.path("code").asString())) {
                    continue;
                }
                if (held.remove(id) == null) {
                    context.api().post(path, context.object().put("permissionId", id));
                }
                context.count(module(), "rolePermission");
            }
            // a permission the role no longer selects (an earlier version of the seeder granted it) is taken away
            held.values().forEach(stale -> context.api().delete(path + "/" + stale.path("id").asString(),
                    stale.path("etag").asString()));
        }
    }

    private void account(SeedContext context, Persona persona) {
        String personId = persona.record().path("personId").asString();
        String email = context.api().get("/v1/people/persons/" + personId).path("primaryEmail").asString();
        var ensured = context.api().ensure("/v1/iam/users", Filter.eq("username", persona.username()),
                context.object().put("username", persona.username()).put("personId", personId)
                        .put("userType", persona.userType()).put("email", email));
        context.count(module(), "userAccount");
        String user = ensured.id();
        if (ensured.created()) {
            context.api().post("/v1/iam/users/" + user + ":setCredentials", context.object()
                    .put("temporaryPassword", context.settings().personaPassword()).put("mustResetPassword", false));
        }
        ObjectNode roles = context.object();
        ArrayNode grants = roles.putArray("grants");
        persona.grants().forEach(grant -> grants.addObject().put("roleCode", grant.roleCode())
                .put("scopeType", grant.scopeType()).put("scopeId", grant.scopeId()));
        context.api().post("/v1/iam/users/" + user + ":setRoles", roles);
        link(context, persona, user);
    }

    /**
     * Links the staff, student or guardian record to the account through the record's own portal-account operation,
     * which takes the account iam already keeps for the person, so the person's own reads find their record.
     */
    private static void link(SeedContext context, Persona persona, String user) {
        String path = persona.recordPath() + persona.record().path("id").asString();
        String linked = context.api().get(path).path("userId").asString(null);
        if (linked == null) {
            linked = context.api().post(path + ":createPortalAccount", context.object()).path("userId").asString();
        }
        if (!user.equals(linked)) {
            throw new IllegalStateException(path + " belongs to another login account than " + persona.username());
        }
    }
}
