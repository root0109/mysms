import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';
import { fakeRecords } from './records';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * A tenant's organisation as the administration screens see it: a trust with two schools under it (one inactive) and
 * a college whose parent is outside the caller's scope, the school type's two attributes (one required, one already
 * filled in), departments, a planned and a closed academic year with a term, a holiday, two roles (one a system role)
 * and a small permission catalogue; plus what the admin dashboard counts.
 */
export const orgFixture = {
  trustId: id('f01'),
  schoolId: id('f02'),
  annexId: id('f03'),
  collegeId: id('f04'),
  schoolTypeId: id('f11'),
  boardDefinitionId: id('f21'),
  hostelDefinitionId: id('f22'),
  plannedYearId: id('f41'),
  closedYearId: id('f42'),
  teacherRoleId: id('f51'),
  systemRoleId: id('f52'),
  permissions: [
    { id: id('f61'), code: 'sis.student.read', displayName: 'Read students', moduleCode: 'sis' },
    { id: id('f62'), code: 'sis.student.update', displayName: 'Change students', moduleCode: 'sis' },
    {
      id: id('f63'),
      code: 'hr.staff.read',
      displayName: 'Read staff',
      moduleCode: 'hr',
      isModifiable: false,
    },
  ],
} as const;

/** What a tenant administrator needs for the organisation screens (ADM-01…ADM-10). */
export const ORG_ADMIN = [
  'org.institution.read',
  'org.institution.create',
  'org.institution.update',
  'org.institution.delete',
  'org.institution_attribute.read',
  'org.institution_attribute.create',
  'org.institution_attribute.update',
  'org.institution_type_attribute.read',
  'org.attribute_definition.read',
  'org.department.read',
  'org.department.create',
  'org.academic_year.read',
  'org.academic_year.create',
  'org.academic_year.activate_academic_year',
  'org.academic_year.generate_terms',
  'org.academic_term.read',
  'org.academic_term.create',
  'org.holiday.read',
  'iam.role.read',
  'iam.role.create',
  'iam.role_permission.read',
  'iam.role_permission.create',
  'iam.role_permission.delete',
  'iam.permission.read',
  'admission.application.read',
  'hr.leave_application.read',
  'attendance.attendance_sheet.read',
  'comms.feedback_thread.read',
] as const;

/**
 * The organisation's records over the generic record handlers, the permission catalogue, and the two academic-year
 * steps with the server's rules for them: activating takes a planned year under its etag (412 stale, 409 not
 * planned), generating terms writes the year's two semesters. `day` is the dashboard's today.
 */
export function fakeOrg(day = '2026-03-02'): Record<string, Handler> {
  const years = fakeRecords({
    collection: '/v1/org/academic-years',
    item: '/v1/org/academic-years/{academicYearId}',
    required: ['institutionId', 'name', 'startDate', 'endDate'],
    rows: [
      {
        id: orgFixture.plannedYearId,
        institutionId: orgFixture.schoolId,
        name: '2026-27',
        startDate: '2026-04-01',
        endDate: '2027-03-31',
        termStructure: 'two_semesters',
        status: 'planned',
      },
      {
        id: orgFixture.closedYearId,
        institutionId: orgFixture.schoolId,
        name: '2025-26',
        startDate: '2025-04-01',
        endDate: '2026-03-31',
        status: 'closed',
      },
    ],
  });
  const terms = fakeRecords({
    collection: '/v1/org/academic-years/{academicYearId}/terms',
    item: '/v1/org/academic-years/{academicYearId}/terms/{academicTermId}',
    required: ['name', 'startDate', 'endDate'],
    rows: [
      {
        id: id('f45'),
        academicYearId: orgFixture.closedYearId,
        name: 'Whole year',
        startDate: '2025-04-01',
        endDate: '2026-03-31',
        sequence: 1,
      },
    ],
  });
  const readYear = years['GET /v1/org/academic-years/{academicYearId}'] as Handler;
  const patchYear = years['PATCH /v1/org/academic-years/{academicYearId}'] as Handler;
  const postTerm = terms['POST /v1/org/academic-years/{academicYearId}/terms'] as Handler;
  const rows = (resource: string, list: Record<string, unknown>[]) => ({ resource, list });
  const counted = [
    rows('/v1/admission/applications', [
      { id: id('f71'), status: 'submitted' },
      { id: id('f72'), status: 'confirmed' },
      { id: id('f73'), status: 'admitted' },
    ]),
    rows('/v1/hr/leave-applications', [{ id: id('f74'), status: 'pending' }]),
    rows('/v1/attendance/attendance-sheets', [
      { id: id('f75'), attendanceDate: day, status: 'in_progress' },
      { id: id('f76'), attendanceDate: day, status: 'done' },
      { id: id('f77'), attendanceDate: '2026-02-27', status: 'draft' },
    ]),
    rows('/v1/comms/feedback-threads', []),
  ];
  return {
    ...fakeRecords({
      collection: '/v1/org/institutions',
      item: '/v1/org/institutions/{institutionId}',
      required: ['code', 'name'],
      rows: [
        { id: orgFixture.trustId, code: 'TRUST', name: 'Green Valley Trust', status: 'active' },
        {
          id: orgFixture.schoolId,
          code: 'GVS',
          name: 'Green Valley School',
          parentInstitutionId: orgFixture.trustId,
          institutionTypeId: orgFixture.schoolTypeId,
          status: 'active',
        },
        {
          id: orgFixture.annexId,
          code: 'GVA',
          name: 'Green Valley Annexe',
          parentInstitutionId: orgFixture.trustId,
          status: 'inactive',
        },
        {
          id: orgFixture.collegeId,
          code: 'GVC',
          name: 'Green Valley College',
          parentInstitutionId: id('fff'),
          status: 'active',
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/org/institutions/{institutionId}/attributes',
      item: '/v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}',
      required: ['attrKey'],
      rows: [
        {
          id: id('f31'),
          institutionId: orgFixture.schoolId,
          attrKey: 'Board',
          attrValue: 'CBSE',
          attributeDefinitionId: orgFixture.boardDefinitionId,
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/org/institution-types/{institutionTypeId}/attributes',
      item: '/v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}',
      required: ['attributeDefinitionId'],
      rows: [
        {
          id: id('f12'),
          institutionTypeId: orgFixture.schoolTypeId,
          attributeDefinitionId: orgFixture.hostelDefinitionId,
          isRequired: true,
        },
        {
          id: id('f13'),
          institutionTypeId: orgFixture.schoolTypeId,
          attributeDefinitionId: orgFixture.boardDefinitionId,
        },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/org/attribute-definitions',
      item: '/v1/org/attribute-definitions/{attributeDefinitionId}',
      required: ['name'],
      rows: [
        { id: orgFixture.boardDefinitionId, name: 'Board', dataType: 'text', sortOrder: 1 },
        { id: orgFixture.hostelDefinitionId, name: 'Hostel beds', dataType: 'number', sortOrder: 2 },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/org/departments',
      item: '/v1/org/departments/{departmentId}',
      required: ['institutionId', 'code', 'name'],
      rows: [
        { id: id('f35'), institutionId: orgFixture.schoolId, code: 'SCI', name: 'Science' },
        { id: id('f36'), institutionId: orgFixture.annexId, code: 'ART', name: 'Arts' },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/org/holidays',
      item: '/v1/org/holidays/{holidayId}',
      required: ['title', 'startDate', 'endDate'],
      rows: [],
    }),
    ...years,
    ...terms,
    'POST /v1/org/academic-years/{academicYearId}:activate': async (req, p) => {
      const current = (await (await readYear(req, p)).json()) as Record<string, unknown>;
      if (!current.id) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { etag?: string };
      if (body.etag !== current.etag) return problem(412, 'PRECONDITION_FAILED');
      if (current.status !== 'planned') return problem(409, 'INVALID_STATE');
      const patch = new Request(req.url, {
        method: 'PATCH',
        headers: { 'If-Match': String(current.etag) },
        body: JSON.stringify({ status: 'active', isCurrent: true }),
      });
      return json(await (await patchYear(patch, p)).json());
    },
    'POST /v1/org/academic-years/{academicYearId}:generateTerms': async (req, p) => {
      const made = [];
      for (const [sequence, name, startDate, endDate] of [
        [1, 'Semester 1', '2026-04-01', '2026-09-30'],
        [2, 'Semester 2', '2026-10-01', '2027-03-31'],
      ] as const) {
        const post = new Request(req.url, {
          method: 'POST',
          body: JSON.stringify({ name, startDate, endDate, sequence }),
        });
        made.push(await (await postTerm(post, p)).json());
      }
      return json({ items: made });
    },
    ...fakeRecords({
      collection: '/v1/iam/roles',
      item: '/v1/iam/roles/{roleId}',
      required: ['code'],
      rows: [
        { id: orgFixture.teacherRoleId, code: 'teacher', displayName: 'Teacher', isActive: true },
        { id: orgFixture.systemRoleId, code: 'tenant_admin', displayName: 'Administrator', isSystem: true },
      ],
    }),
    ...fakeRecords({
      collection: '/v1/iam/roles/{roleId}/permissions',
      item: '/v1/iam/roles/{roleId}/permissions/{rolePermissionId}',
      required: ['permissionId'],
      rows: [
        { id: id('f65'), roleId: orgFixture.teacherRoleId, permissionId: orgFixture.permissions[0].id },
        { id: id('f66'), roleId: orgFixture.systemRoleId, permissionId: orgFixture.permissions[0].id },
      ],
    }),
    'GET /v1/iam/permissions': (req) => onePage(req, orgFixture.permissions),
    ...Object.fromEntries(
      counted.flatMap(({ resource, list }) =>
        Object.entries(
          fakeRecords({ collection: resource, item: `${resource}/{rowId}`, required: [], rows: list }),
        ),
      ),
    ),
  };
}
