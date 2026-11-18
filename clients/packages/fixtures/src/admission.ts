import type { admission } from '@mysms/sdk';
import { attendanceFixture } from './attendance';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;
const INSTITUTION = attendanceFixture.institutionId;

/**
 * One school's admissions: a cycle open for applications (and one still a draft), and applications at several
 * stages — handed in with a parent and a contact, approved, admitted with a class section waiting for enrolment, a
 * rejected one, and an applicant's own draft.
 */
export const admissionFixture = {
  institutionId: INSTITUTION,
  academicYearId: id('a1'),
  openCycleId: id('a11'),
  draftCycleId: id('a12'),
  submittedId: id('a21'),
  approvedId: id('a22'),
  admittedId: id('a23'),
  rejectedId: id('a24'),
  draftId: id('a25'),
  classSectionId: id('a31'),
  otherClassSectionId: id('a32'),
  studentId: id('a41'),
} as const;

/** The admissions office: every admission permission in its institution. */
export const ADMISSION_OFFICE = [
  'admission.admission_cycle.read',
  'admission.admission_cycle.create',
  'admission.admission_cycle.update',
  'admission.admission_cycle.confirm_admission_cycle',
  'admission.admission_cycle.open_applications',
  'admission.admission_cycle.start_admission',
  'admission.admission_cycle.close_admission_cycle',
  'admission.admission_cycle.cancel_admission_cycle',
  'admission.admission_cycle.reset_admission_cycle',
  'admission.application.read',
  'admission.application.create',
  'admission.application.update',
  'admission.application.submit_application',
  'admission.application.confirm_application',
  'admission.application.mark_application_pending',
  'admission.application.reject_application',
  'admission.application.cancel_application',
  'admission.application.reset_application',
  'admission.application.approve_application',
  'admission.application.confirm_admission',
  'admission.application.enroll_applicant',
  'admission.application_guardian.read',
  'admission.application_emergency_contact.read',
] as const;

/** An applicant's account: their own form, start to hand-in, and nothing of the office's. */
export const ADMISSION_APPLICANT = [
  'admission.admission_cycle.read',
  'admission.application.read',
  'admission.application.create',
  'admission.application.update',
  'admission.application.submit_application',
  'admission.application_guardian.read',
  'admission.application_guardian.create',
  'admission.application_guardian.delete',
  'admission.application_emergency_contact.read',
  'admission.application_emergency_contact.create',
  'admission.application_emergency_contact.delete',
] as const;

type Mutable<T> = { -readonly [K in keyof T]: T[K] };
type Row = Mutable<admission.Application> & { id: string; version: number };
type Part<T> = Mutable<T> & { id: string; applicationId: string; version: number };

/** The step table of the server (api/state-machines.md "admission.Application"). */
const STEPS: Record<string, { from: readonly string[]; to: admission.Application['status'] }> = {
  submit: { from: ['draft'], to: 'submitted' },
  confirm: { from: ['submitted', 'pending'], to: 'confirmed' },
  markPending: { from: ['submitted', 'confirmed'], to: 'pending' },
  approve: { from: ['confirmed'], to: 'approved' },
  confirmAdmission: { from: ['approved'], to: 'admitted' },
  reject: { from: ['submitted', 'pending', 'confirmed', 'approved'], to: 'rejected' },
  cancel: { from: ['draft', 'submitted', 'pending', 'confirmed', 'approved', 'admitted'], to: 'cancelled' },
  resetToDraft: { from: ['rejected', 'cancelled'], to: 'draft' },
};

export interface FakeAdmissionOptions {
  /** Enrolment answers 503 (people or sis unavailable), to show the retry path. */
  enrolmentUnavailable?: boolean;
  /** The applicant has not started a form yet (no draft in the open cycle). */
  withoutDraft?: boolean;
}

/**
 * Admissions over in-memory rows with the server's rules for the slice the screens use: the step table (409 from
 * another state), the body etag of a step and If-Match of a change (412), a parent or guardian before hand-in
 * (422), changes to a draft only, enrolment of an admitted application only, with an Idempotency-Key and a class
 * section or a batch (422), answering the student and — when asked — the portal accounts as a finished operation.
 */
export function fakeAdmission(options: FakeAdmissionOptions = {}): Record<string, Handler> {
  const f = admissionFixture;
  let sequence = 0;
  const application = (appId: string, number: string, name: string, more: Partial<Row>): Row => ({
    id: appId,
    institutionId: INSTITUTION,
    admissionCycleId: f.openCycleId,
    academicYearId: f.academicYearId,
    applicationNumber: number,
    applicationDate: '2026-03-02T09:30:00Z',
    fullName: name,
    dateOfBirth: '2020-06-14',
    gender: 'female',
    paymentStatus: 'unpaid',
    status: 'submitted',
    version: 1,
    ...more,
  });
  const rows = new Map<string, Row>(
    [
      application(f.submittedId, 'A-0101', 'Asha Rao', {
        mobile: '+919800000101',
        previousInstitution: 'Little Steps Play School',
      }),
      application(f.approvedId, 'A-0102', 'Kabir Menon', { gender: 'male', status: 'approved' }),
      application(f.admittedId, 'A-0103', 'Diya Shah', {
        status: 'admitted',
        sectionId: f.classSectionId,
        admissionDate: '2026-04-01',
        paymentStatus: 'paid',
      }),
      application(f.rejectedId, 'A-0104', 'Rohan Das', { gender: 'male', status: 'rejected' }),
      ...(options.withoutDraft
        ? []
        : [application(f.draftId, 'W-20260301-ABCDEF', 'Isha Nair', { status: 'draft' })]),
    ].map((row) => [row.id, row]),
  );
  const guardians = new Map<string, Part<admission.ApplicationGuardian>>([
    [
      id('a51'),
      {
        id: id('a51'),
        applicationId: f.submittedId,
        relationship: 'mother',
        name: 'Meera Rao',
        mobile: '+919800000001',
        occupation: 'Engineer',
        version: 1,
      },
    ],
  ]);
  const contacts = new Map<string, Part<admission.ApplicationEmergencyContact>>([
    [
      id('a61'),
      {
        id: id('a61'),
        applicationId: f.submittedId,
        priority: 1,
        contactNumber: '+919800000002',
        relationship: 'Grandmother',
        version: 1,
      },
    ],
  ]);
  const out = ({ version, ...row }: { version: number }) => ({ ...row, etag: `"${version}"` });
  const withEtag = (row: { version: number }, status = 200) =>
    json(out(row), { status, headers: { ETag: `"${row.version}"` } });
  const cycles: admission.AdmissionCycle[] = [
    {
      id: f.openCycleId,
      institutionId: INSTITUTION,
      name: 'Grade 1, 2026–27',
      admissionBasis: 'grade_level',
      academicYearId: f.academicYearId,
      startDate: '2026-02-01',
      endDate: '2026-04-30',
      status: 'open',
      etag: '"3"',
    },
    {
      id: f.draftCycleId,
      institutionId: INSTITUTION,
      name: 'Grade 6, 2026–27',
      admissionBasis: 'grade_level',
      startDate: '2026-05-01',
      status: 'draft',
      etag: '"1"',
    },
  ];
  const draftOnly = (applicationId: string): Response | null => {
    const row = rows.get(applicationId);
    if (!row) return problem(404, 'NOT_FOUND');
    if (row.status !== 'draft') return problem(409, 'INVALID_STATE_TRANSITION');
    return null;
  };
  const step =
    (name: string): Handler =>
    async (req, params) => {
      const row = rows.get(params.applicationId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { etag?: string };
      if (body.etag !== undefined && body.etag !== `"${row.version}"`) return problem(412, 'ETAG_MISMATCH');
      const rule = STEPS[name];
      if (!rule || !rule.from.includes(row.status ?? 'draft'))
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The application cannot be changed this way in its current state.',
        });
      if (name === 'submit' && ![...guardians.values()].some((g) => g.applicationId === row.id))
        return problem(422, 'VALIDATION_FAILED', 'Unprocessable', {
          errors: [
            { field: 'guardians', message: 'an application needs a parent or guardian', code: 'REQUIRED' },
          ],
        });
      row.status = rule.to;
      row.version += 1;
      if (name === 'confirmAdmission') row.admissionDate = '2026-03-09';
      return withEtag(row);
    };
  const list =
    <T extends { applicationId: string; version: number }>(map: Map<string, T>): Handler =>
    (req, params) =>
      rows.has(params.applicationId ?? '')
        ? onePage(
            req,
            [...map.values()].filter((part) => part.applicationId === params.applicationId).map(out),
          )
        : problem(404, 'NOT_FOUND');
  return {
    // the institution's classes, named by grade and section (for the enrolment wizard's choice)
    'GET /v1/academics/class-sections': (req) =>
      onePage(
        req,
        [f.classSectionId, f.otherClassSectionId].map((classId, index) => ({
          id: classId,
          institutionId: INSTITUTION,
          academicYearId: f.academicYearId,
          gradeLevelId: id('a35'),
          sectionId: id(`a3${6 + index}`),
          isActive: true,
        })),
      ),
    'GET /v1/academics/grade-levels': (req) =>
      onePage(req, [
        { id: id('a35'), institutionId: INSTITUTION, name: 'Grade 1', slug: 'g1', sortOrder: 1 },
      ]),
    'GET /v1/academics/sections': (req) =>
      onePage(req, [
        { id: id('a36'), institutionId: INSTITUTION, name: 'A' },
        { id: id('a37'), institutionId: INSTITUTION, name: 'B' },
      ]),
    'GET /v1/admission/admission-cycles': (req) => onePage(req, cycles),
    'GET /v1/admission/admission-cycles/{admissionCycleId}': (_req, params) => {
      const cycle = cycles.find((c) => c.id === params.admissionCycleId);
      return cycle ? json(cycle) : problem(404, 'NOT_FOUND');
    },
    'GET /v1/admission/applications': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const cycle = /admissionCycleId = "([^"]+)"/.exec(filter)?.[1];
      const status = /status = "([^"]+)"/.exec(filter)?.[1];
      return onePage(
        req,
        [...rows.values()]
          .filter((row) => (!cycle || row.admissionCycleId === cycle) && (!status || row.status === status))
          .map(out),
      );
    },
    'GET /v1/admission/applications/{applicationId}': (_req, params) => {
      const row = rows.get(params.applicationId ?? '');
      return row ? withEtag(row) : problem(404, 'NOT_FOUND');
    },
    'POST /v1/admission/applications': async (req) => {
      const body = (await req.json()) as admission.Application;
      if (!body.fullName || !body.dateOfBirth || !body.gender || !body.applicationNumber)
        return problem(422, 'VALIDATION_FAILED');
      const row: Row = {
        ...body,
        id: id(`a7${++sequence}`),
        status: 'draft',
        paymentStatus: 'unpaid',
        applicationDate: '2026-03-09T10:00:00Z',
        version: 1,
      };
      rows.set(row.id, row);
      return withEtag(row, 201);
    },
    'PATCH /v1/admission/applications/{applicationId}': async (req, params) => {
      const row = rows.get(params.applicationId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (req.headers.get('If-Match') !== `"${row.version}"`) return problem(412, 'ETAG_MISMATCH');
      Object.assign(row, (await req.json()) as admission.ApplicationUpdate);
      row.version += 1;
      return withEtag(row);
    },
    'GET /v1/admission/applications/{applicationId}/guardians': list(guardians),
    'POST /v1/admission/applications/{applicationId}/guardians': async (req, params) => {
      const refused = draftOnly(params.applicationId ?? '');
      if (refused) return refused;
      const body = (await req.json()) as admission.ApplicationGuardian;
      const taken = [...guardians.values()].some(
        (g) => g.applicationId === params.applicationId && g.relationship === body.relationship,
      );
      if (taken) return problem(409, 'DUPLICATE');
      const part = {
        ...body,
        id: id(`a8${++sequence}`),
        applicationId: params.applicationId ?? '',
        version: 1,
      };
      guardians.set(part.id, part);
      return json(out(part), { status: 201 });
    },
    'DELETE /v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}': (_req, params) => {
      const refused = draftOnly(params.applicationId ?? '');
      if (refused) return refused;
      guardians.delete(params.applicationGuardianId ?? '');
      return new Response(null, { status: 204 });
    },
    'GET /v1/admission/applications/{applicationId}/emergency-contacts': list(contacts),
    'POST /v1/admission/applications/{applicationId}/emergency-contacts': async (req, params) => {
      const refused = draftOnly(params.applicationId ?? '');
      if (refused) return refused;
      const body = (await req.json()) as admission.ApplicationEmergencyContact;
      const part = {
        ...body,
        id: id(`a9${++sequence}`),
        applicationId: params.applicationId ?? '',
        version: 1,
      };
      contacts.set(part.id, part);
      return json(out(part), { status: 201 });
    },
    'DELETE /v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}': (
      _req,
      params,
    ) => {
      const refused = draftOnly(params.applicationId ?? '');
      if (refused) return refused;
      contacts.delete(params.applicationEmergencyContactId ?? '');
      return new Response(null, { status: 204 });
    },
    ...Object.fromEntries(
      Object.keys(STEPS).map((name) => [
        `POST /v1/admission/applications/{applicationId}:${name}`,
        step(name),
      ]),
    ),
    'POST /v1/admission/applications/{applicationId}:enroll': async (req, params) => {
      const row = rows.get(params.applicationId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (!req.headers.get('Idempotency-Key')) return problem(400, 'INVALID_ARGUMENT');
      const body = (await req.json()) as admission.EnrollApplicantRequest;
      if (row.status === 'done' && row.studentId)
        return json({ application: out(row), studentId: row.studentId });
      if (row.status !== 'admitted') return problem(409, 'INVALID_STATE_TRANSITION');
      if (!body.classSectionId && !body.batchId)
        return problem(422, 'VALIDATION_FAILED', 'Unprocessable', {
          errors: [
            { field: 'classSectionId', message: 'a class section or a batch is required', code: 'REQUIRED' },
          ],
        });
      if (options.enrolmentUnavailable)
        return problem(503, 'UNAVAILABLE', 'Unavailable', { detail: 'The sis service could not answer.' });
      row.status = 'done';
      row.studentId = f.studentId;
      if (body.classSectionId) row.sectionId = body.classSectionId;
      row.version += 1;
      return json({
        application: out(row),
        studentId: f.studentId,
        ...(body.createPortalAccounts
          ? {
              operation: {
                operationId: id('a91'),
                module: 'admission',
                kind: 'admission.EnrollApplicant.portalAccounts',
                status: 'succeeded',
                progressPercent: 100,
                createTime: '2026-03-09T10:00:00Z',
                doneTime: '2026-03-09T10:00:01Z',
              },
            }
          : {}),
      });
    },
  };
}
