import type { sis } from '@mysms/sdk';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;
const EXIT = ['withdrawn', 'transferred', 'graduated', 'alumni'];

/**
 * A school with one student (Diya Menon) and her mother as guardian, an incident shared with guardians, a class of
 * two students for the promotion wizard, and the next year, grade and section it can lead to.
 */
export const sisFixture = {
  institutionId: id('d1'),
  studentId: id('d11'),
  personId: id('d12'),
  guardianId: id('d21'),
  guardianPersonId: id('d22'),
  linkId: id('d23'),
  incidentId: id('d31'),
  enrollmentId: id('d41'),
  year: id('d51'),
  nextYear: id('d52'),
  classSectionId: id('d61'),
  grade: id('d62'),
  nextGrade: id('d63'),
  section: id('d64'),
  classmateId: id('d13'),
  classmatePersonId: id('d14'),
  registrationId: id('da1'),
  courseId: id('da2'),
  compulsoryLine: id('da3'),
  electiveLine: id('da4'),
  physics: id('da5'),
  art: id('da6'),
} as const;

/** What the office's role needs for the student 360° and its steps. */
export const SIS_OFFICE = [
  'sis.student.read',
  'sis.student.get_student_summary',
  'sis.student.change_student_status',
  'sis.student.issue_bonafide_certificate',
  'sis.student.create_student_portal_account',
  'sis.guardian.read',
  'sis.guardian.create_portal_account',
  'sis.student_enrollment.read',
  'sis.discipline_incident.read',
  'sis.discipline_incident.mark_incident_action_taken',
  'sis.discipline_incident.mark_incident_seen',
  'people.person.read',
] as const;

/** What the promotion wizard needs, besides reading names. */
export const SIS_PROMOTER = [
  'sis.promotion.read',
  'sis.promotion.propose_promotions',
  'sis.promotion.approve_promotion',
  'sis.promotion.reject_promotion',
  'sis.promotion.apply_promotions',
  'sis.student_enrollment.read',
  'org.institution.read',
  'org.academic_year.read',
  'academics.class_section.read',
  'academics.grade_level.read',
  'academics.section.read',
] as const;

type Mutable<T> = { -readonly [K in keyof T]: T[K] };

/**
 * The sis slice the student screens use, with the server's rules: the body etag on every step (412), a status
 * change to the status the student has (409) or an exit without its date (422), a portal account only once (409),
 * a step on an incident only from the state that allows it (409), promotions proposed for the class and applied only
 * when approved (409).
 */
export function fakeSis(
  options: {
    /** The class's two students already have proposals (made by someone else), to review without proposing. */
    proposed?: boolean;
    /** Approving the classmate's proposal is refused (409), to exercise a partial "approve all". */
    refuseClassmate?: boolean;
  } = {},
): Record<string, Handler> {
  const f = sisFixture;
  let version = 1;
  let sequence = 0;
  const student: Mutable<sis.Student> = {
    id: f.studentId,
    personId: f.personId,
    institutionId: f.institutionId,
    admissionNumber: 'A-104',
    admissionDate: '2022-06-01',
    status: 'active',
  };
  const guardian: Mutable<sis.Guardian> = {
    id: f.guardianId,
    personId: f.guardianPersonId,
    mobile: '+91 98450 12345',
    email: 'lakshmi@example.test',
    etag: '"1"',
  };
  const incident: Mutable<sis.DisciplineIncident> & { version: number } = {
    id: f.incidentId,
    institutionId: f.institutionId,
    academicYearId: f.year,
    studentId: f.studentId,
    incidentAt: '2026-03-02T09:30:00Z',
    reportedByUserId: id('d91'),
    details: 'Left the lab without permission.',
    actionTaken: false,
    notifyGuardians: true,
    seenByGuardian: false,
    version: 1,
  };
  const promotions = new Map<string, Mutable<sis.Promotion> & { version: number }>();
  const studentOut = () => ({ ...student, etag: `"${version}"` });
  const incidentOut = () => {
    const { version: v, ...rest } = incident;
    return { ...rest, etag: `"${v}"` };
  };
  const promotionOut = ({ version: v, ...rest }: sis.Promotion & { version: number }) => ({
    ...rest,
    etag: `"${v}"`,
  });
  const etagOf = async (req: Request) =>
    (await req.json()) as { etag?: string; response?: string; status?: string };
  const promotionStep =
    (to: 'approved' | 'rejected'): Handler =>
    async (req, p) => {
      const row = promotions.get(p.promotionId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = await etagOf(req);
      if (body.etag !== undefined && body.etag !== `"${row.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (row.status !== 'proposed') return problem(409, 'INVALID_STATE_TRANSITION');
      if (to === 'approved' && options.refuseClassmate && row.studentId === f.classmateId)
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The promotion cannot be changed this way in its current state.',
        });
      row.status = to;
      row.version += 1;
      return json(promotionOut(row));
    };
  const propose = (body: Pick<sis.Promotion, 'toAcademicYearId' | 'toGradeLevelId' | 'toSectionId'>) => {
    for (const [index, studentId] of [f.studentId, f.classmateId].entries()) {
      if ([...promotions.values()].some((row) => row.studentId === studentId)) continue;
      const promotionId = id(`e${index + 1}`);
      promotions.set(promotionId, {
        id: promotionId,
        studentId,
        institutionId: f.institutionId,
        fromAcademicYearId: f.year,
        fromGradeLevelId: f.grade,
        ...body,
        status: 'proposed',
        version: 1,
      });
    }
  };
  if (options.proposed) propose({ toAcademicYearId: f.nextYear, toGradeLevelId: f.nextGrade });
  const persons = [
    { id: f.personId, displayName: 'Diya Menon', dateOfBirth: '2014-08-21' },
    { id: f.classmatePersonId, displayName: 'Arjun Das' },
    { id: f.guardianPersonId, displayName: 'Lakshmi Menon' },
  ];
  const idsIn = (req: Request) =>
    [...(new URL(req.url).searchParams.get('filter') ?? '').matchAll(/"([0-9a-f-]{36})"/g)].map((m) => m[1]);
  const registration: Mutable<sis.SubjectRegistration> & { version: number } = {
    id: f.registrationId,
    studentId: f.studentId,
    courseId: f.courseId,
    academicYearId: f.year,
    programEnrollmentId: id('da7'),
    referenceNo: 'SR-2026-001',
    status: 'draft',
    version: 1,
  };
  const lines = new Map<string, sis.SubjectRegistrationLine & { version: number }>([
    [
      f.compulsoryLine,
      {
        id: f.compulsoryLine,
        subjectRegistrationId: f.registrationId,
        subjectId: f.physics,
        courseSubjectId: id('da8'),
        selectionType: 'compulsory',
        version: 1,
      },
    ],
  ]);
  const registrationOut = () => {
    const { version: v, ...rest } = registration;
    return { ...rest, etag: `"${v}"` };
  };
  const registrationStep =
    (from: string, to: sis.SubjectRegistration['status']): Handler =>
    async (req) => {
      const body = await etagOf(req);
      if (body.etag !== undefined && body.etag !== `"${registration.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (registration.status !== from) return problem(409, 'INVALID_STATE_TRANSITION');
      registration.status = to;
      registration.version += 1;
      return json(registrationOut());
    };
  return {
    'GET /v1/sis/students/{studentId}:summary': (_req, p) =>
      p.studentId !== f.studentId
        ? problem(404, 'NOT_FOUND')
        : json({
            student: studentOut(),
            person: persons[0],
            currentEnrollment: {
              id: f.enrollmentId,
              studentId: f.studentId,
              academicYearId: f.year,
              classSectionId: f.classSectionId,
              rollNumber: '7',
              enrollmentStatus: EXIT.includes(student.status ?? '') ? 'left' : 'enrolled',
              effectiveFrom: '2025-06-01',
            },
            programEnrollments: [],
            guardians: [
              {
                id: f.linkId,
                studentId: f.studentId,
                guardianId: f.guardianId,
                isPrimaryContact: true,
                status: 'active',
              },
            ],
            openIncidents: incident.actionTaken ? 0 : 1,
          }),
    'POST /v1/sis/students/{studentId}:changeStatus': async (req) => {
      const body = await etagOf(req);
      const exit = (body as { exitDate?: string }).exitDate;
      if (body.etag !== undefined && body.etag !== `"${version}"`) return problem(412, 'PRECONDITION_FAILED');
      if (body.status === student.status)
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The student already has this status.',
        });
      if (EXIT.includes(body.status ?? '') && !exit)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [{ field: 'exitDate', message: 'is required when the student leaves', code: 'NotNull' }],
        });
      student.status = body.status as sis.Student['status'];
      if (exit) student.exitDate = exit;
      else delete student.exitDate;
      version += 1;
      return json(studentOut());
    },
    'POST /v1/sis/students/{studentId}:issueBonafideCertificate': () => {
      if (student.status !== 'active') return problem(409, 'INVALID_STATE_TRANSITION');
      student.bonafideCertificateNumber = `BC/2026/${String(++sequence).padStart(6, '0')}`;
      version += 1;
      return json(
        { operationId: id('d81'), module: 'sis', kind: 'sis.IssueBonafideCertificate', status: 'succeeded' },
        { status: 202 },
      );
    },
    'POST /v1/sis/students/{studentId}:createPortalAccount': () => {
      if (student.userId) return problem(409, 'INVALID_STATE_TRANSITION');
      student.userId = id('d71');
      version += 1;
      return json(studentOut());
    },
    'POST /v1/sis/guardians/{guardianId}:createPortalAccount': () => {
      if (guardian.userId) return problem(409, 'INVALID_STATE_TRANSITION');
      guardian.userId = id('d72');
      return json(guardian);
    },
    'GET /v1/sis/guardians': (req) => onePage(req, idsIn(req).includes(f.guardianId) ? [guardian] : []),
    'GET /v1/people/persons': (req) =>
      onePage(
        req,
        persons.filter((person) => idsIn(req).includes(person.id)),
      ),
    'GET /v1/sis/students': (req) =>
      onePage(
        req,
        [
          { id: f.studentId, personId: f.personId, institutionId: f.institutionId },
          { id: f.classmateId, personId: f.classmatePersonId, institutionId: f.institutionId },
        ].filter((row) => idsIn(req).includes(row.id)),
      ),
    'GET /v1/sis/student-enrollments': (req) =>
      onePage(req, [
        {
          id: f.enrollmentId,
          studentId: f.studentId,
          academicYearId: f.year,
          classSectionId: f.classSectionId,
          rollNumber: '7',
          enrollmentStatus: 'enrolled',
          effectiveFrom: '2025-06-01',
        },
      ]),
    'GET /v1/sis/discipline-incidents': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const open = filter.includes('actionTaken = false');
      return onePage(req, open && incident.actionTaken ? [] : [incidentOut()]);
    },
    'POST /v1/sis/discipline-incidents/{disciplineIncidentId}:markActionTaken': async (req) => {
      const body = await etagOf(req);
      if (body.etag !== undefined && body.etag !== `"${incident.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (incident.actionTaken) return problem(409, 'INVALID_STATE_TRANSITION');
      incident.actionTaken = true;
      if (body.response) incident.response = body.response;
      incident.version += 1;
      return json(incidentOut());
    },
    'POST /v1/sis/discipline-incidents/{disciplineIncidentId}:markSeen': async (req) => {
      const body = await etagOf(req);
      if (body.etag !== undefined && body.etag !== `"${incident.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (incident.seenByGuardian || !incident.notifyGuardians)
        return problem(409, 'INVALID_STATE_TRANSITION');
      incident.seenByGuardian = true;
      incident.version += 1;
      return json(incidentOut());
    },
    'GET /v1/sis/me/children': (req) =>
      onePage(req, [
        {
          student: studentOut(),
          person: persons[0],
          institutionName: 'Main School',
          classSectionLabel: 'Grade 5-A',
          relationship: 'Mother',
        },
      ]),
    'GET /v1/sis/student-health-records': (req) =>
      onePage(req, [
        {
          id: id('db1'),
          studentId: f.studentId,
          academicYearId: f.year,
          heightCm: '138.5',
          foodAllergies: 'Peanuts',
        },
      ]),
    'GET /v1/sis/student-activities': (req) =>
      onePage(req, [
        {
          id: id('db2'),
          studentId: f.studentId,
          activityTypeId: id('db3'),
          activityDate: '2026-02-14',
          description: 'Inter-school debate',
        },
      ]),
    'GET /v1/sis/subject-registrations': (req) =>
      onePage(req, idsIn(req).includes(registration.studentId) ? [registrationOut()] : []),
    'GET /v1/sis/subject-registrations/{subjectRegistrationId}/lines': (req) =>
      onePage(
        req,
        [...lines.values()].map(({ version: v, ...rest }) => ({ ...rest, etag: `"${v}"` })),
      ),
    'DELETE /v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}': (
      req,
      p,
    ) => {
      const line = lines.get(p.subjectRegistrationLineId ?? '');
      if (!line) return problem(404, 'NOT_FOUND');
      if (req.headers.get('If-Match') !== `"${line.version}"`) return problem(412, 'PRECONDITION_FAILED');
      lines.delete(line.id ?? '');
      return new Response(null, { status: 204 });
    },
    'POST /v1/sis/subject-registrations/{subjectRegistrationId}:loadCourseSubjects': () => {
      if (registration.status !== 'draft') return problem(409, 'INVALID_STATE_TRANSITION');
      if (![...lines.values()].some((line) => line.subjectId === f.art))
        lines.set(f.electiveLine, {
          id: f.electiveLine,
          subjectRegistrationId: f.registrationId,
          subjectId: f.art,
          courseSubjectId: id('da9'),
          selectionType: 'elective',
          version: 1,
        });
      return json(registrationOut());
    },
    'POST /v1/sis/subject-registrations/{subjectRegistrationId}:submit': registrationStep(
      'draft',
      'submitted',
    ),
    'POST /v1/sis/subject-registrations/{subjectRegistrationId}:approve': registrationStep(
      'submitted',
      'approved',
    ),
    'POST /v1/sis/subject-registrations/{subjectRegistrationId}:reject': registrationStep(
      'submitted',
      'rejected',
    ),
    'POST /v1/sis/subject-registrations/{subjectRegistrationId}:resetToDraft': registrationStep(
      'rejected',
      'draft',
    ),
    // the promotion wizard
    'GET /v1/org/institutions': (req) =>
      onePage(req, [{ id: f.institutionId, code: 'MAIN', name: 'Main School', institutionType: 'school' }]),
    'GET /v1/org/academic-years': (req) =>
      onePage(req, [
        {
          id: f.year,
          institutionId: f.institutionId,
          name: '2025-26',
          startDate: '2025-06-01',
          endDate: '2026-03-31',
        },
        {
          id: f.nextYear,
          institutionId: f.institutionId,
          name: '2026-27',
          startDate: '2026-06-01',
          endDate: '2027-03-31',
        },
      ]),
    'GET /v1/academics/class-sections': (req) =>
      onePage(req, [
        {
          id: f.classSectionId,
          institutionId: f.institutionId,
          academicYearId: f.year,
          gradeLevelId: f.grade,
          sectionId: f.section,
          isActive: true,
        },
      ]),
    'GET /v1/academics/grade-levels': (req) =>
      onePage(req, [
        { id: f.grade, institutionId: f.institutionId, name: 'Grade 5', slug: 'g5', sortOrder: 5 },
        { id: f.nextGrade, institutionId: f.institutionId, name: 'Grade 6', slug: 'g6', sortOrder: 6 },
      ]),
    'GET /v1/academics/sections': (req) =>
      onePage(req, [{ id: f.section, institutionId: f.institutionId, name: 'A' }]),
    'POST /v1/sis/promotions:propose': async (req) => {
      const body = (await req.json()) as sis.ProposePromotionsRequest;
      if (body.fromClassSectionId !== f.classSectionId) return problem(422, 'VALIDATION_FAILED');
      propose({
        toAcademicYearId: body.toAcademicYearId,
        toGradeLevelId: body.toGradeLevelId,
        ...(body.toSectionId ? { toSectionId: body.toSectionId } : {}),
      });
      return json({ items: [...promotions.values()].map(promotionOut) });
    },
    'GET /v1/sis/promotions': (req) =>
      onePage(
        req,
        [...promotions.values()]
          .filter((row) =>
            (new URL(req.url).searchParams.get('filter') ?? '').startsWith('institutionId')
              ? row.status === 'proposed' || row.status === 'approved'
              : idsIn(req).includes(row.id ?? ''),
          )
          .map(promotionOut),
      ),
    'POST /v1/sis/promotions/{promotionId}:approve': promotionStep('approved'),
    'POST /v1/sis/promotions/{promotionId}:reject': promotionStep('rejected'),
    'POST /v1/sis/promotions:apply': async (req) => {
      const body = (await req.json()) as sis.ApplyPromotionsRequest;
      const rows = body.promotionIds.map((promotionId) => promotions.get(promotionId));
      if (rows.some((row) => !row)) return problem(404, 'NOT_FOUND');
      if (rows.some((row) => row?.status !== 'approved')) return problem(409, 'INVALID_STATE_TRANSITION');
      for (const row of rows) {
        if (!row) continue;
        row.status = 'applied';
        row.version += 1;
      }
      return json(
        { operationId: id('d82'), module: 'sis', kind: 'sis.ApplyPromotions', status: 'succeeded' },
        { status: 202 },
      );
    },
  };
}
