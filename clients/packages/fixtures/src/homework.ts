import type { assessment } from '@mysms/sdk';
import { attendanceFixture } from './attendance';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/** One homework of the attendance fixture's class, with two of its three students having handed in. */
export const homeworkFixture = {
  homeworkId: id('b1'),
  submissions: [
    { id: id('b11'), studentId: attendanceFixture.students[0].id, submittedOn: '2026-03-10' },
    { id: id('b12'), studentId: attendanceFixture.students[1].id, submittedOn: '2026-03-11' },
  ],
} as const;

/** The permissions of a teacher who sets and checks homework but needs a reviewer to publish it. */
export const HOMEWORK_TEACHER = [
  'assessment.homework.read',
  'assessment.homework.create',
  'assessment.homework.update',
  'assessment.homework.publish_homework',
  'assessment.homework_approval.read',
  'assessment.homework_submission.read',
  'assessment.homework_submission.check_homework_submission',
  'sis.student_enrollment.list_class_roster',
  'sis.student.read',
  'people.person.read',
] as const;

/** What a reviewer adds: approving and sending back. */
export const HOMEWORK_REVIEWER = [
  'assessment.homework.approve_homework',
  'assessment.homework.reject_homework',
] as const;

export interface FakeHomeworkOptions {
  status?: assessment.Homework['status'];
  /** Whether the caller's role may approve: publishing then needs no review. */
  reviewer?: boolean;
}

/**
 * The homework lifecycle over one in-memory homework, with the server's rules for the slice the screens use:
 * publishing a draft (to pending without the reviewer's role), approving and rejecting only while pending (409), the
 * body etag on every step (412), reviews kept with their reason, and checking a submission under its own etag.
 */
export function fakeHomework(options: FakeHomeworkOptions = {}): Record<string, Handler> {
  const fixture = homeworkFixture;
  let status = options.status ?? 'draft';
  let version = 1;
  const reviews: assessment.HomeworkApproval[] = [];
  const submissions = new Map<string, assessment.HomeworkSubmission & { version: number }>(
    fixture.submissions.map((row) => [
      row.id,
      { ...row, homeworkId: fixture.homeworkId, status: 'unchecked', version: 1 },
    ]),
  );
  const etag = () => `"${version}"`;
  const homework = (): Record<string, unknown> => ({
    id: fixture.homeworkId,
    institutionId: attendanceFixture.institutionId,
    academicYearId: id('e1'),
    classSectionId: attendanceFixture.classSectionId,
    description: 'Read chapter 4 and answer the questions.',
    dueAt: '2026-03-12T10:00:00Z',
    status,
    etag: etag(),
  });
  const answer = (): Response => json(homework(), { headers: { ETag: etag() } });
  const list = (req: Request, items: unknown[]): Response => onePage(req, items);
  const submission = (row: assessment.HomeworkSubmission & { version: number }) => {
    const { version: rowVersion, ...rest } = row;
    return { ...rest, etag: `"${rowVersion}"` };
  };
  const step =
    (from: string, move: (reason: string | undefined) => void): Handler =>
    async (req) => {
      const body = (await req.json()) as { etag?: string; reason?: string };
      if (body.etag !== undefined && body.etag !== etag()) return problem(412, 'PRECONDITION_FAILED');
      if (status !== from) return problem(409, 'INVALID_STATE_TRANSITION');
      move(body.reason);
      version += 1;
      return answer();
    };
  const decide = (decision: 'approved' | 'rejected', reason: string | undefined) => {
    const open = reviews.find((review) => review.decision === 'pending');
    if (open) Object.assign(open, { decision, ...(reason ? { comments: reason } : {}) });
  };
  const base = '/v1/assessment/homeworks/{homeworkId}';
  return {
    // the board lists the same homework (the generic list manager reads it)
    'GET /v1/assessment/homeworks': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const wanted = /status = "(\w+)"/.exec(filter)?.[1];
      return list(req, wanted && wanted !== status ? [] : [homework()]);
    },
    [`GET ${base}`]: (_req, p) =>
      p.homeworkId === fixture.homeworkId ? answer() : problem(404, 'NOT_FOUND'),
    [`GET ${base}/approvals`]: (req) => list(req, reviews),
    [`POST ${base}:publish`]: step('draft', () => {
      if (options.reviewer) {
        status = 'published';
      } else {
        status = 'pending';
        reviews.push({
          id: id(`c${reviews.length + 1}`),
          homeworkId: fixture.homeworkId,
          decision: 'pending',
        });
      }
    }),
    [`POST ${base}:approve`]: step('pending', (reason) => {
      status = 'published';
      decide('approved', reason);
    }),
    [`POST ${base}:reject`]: step('pending', (reason) => {
      status = 'draft';
      decide('rejected', reason);
    }),
    'GET /v1/assessment/homework-submissions': (req) => list(req, [...submissions.values()].map(submission)),
    'POST /v1/assessment/homework-submissions/{homeworkSubmissionId}:check': async (req, p) => {
      const row = submissions.get(p.homeworkSubmissionId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { etag?: string; teacherComments?: string };
      if (body.etag !== undefined && body.etag !== `"${row.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if ((body.teacherComments ?? '').length > 4000)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [{ field: 'teacherComments', message: 'must be at most 4000 characters', code: 'Size' }],
        });
      Object.assign(row, {
        status: 'checked',
        checkedOn: '2026-03-12',
        ...(body.teacherComments ? { teacherComments: body.teacherComments } : {}),
      });
      row.version += 1;
      return json(submission(row));
    },
  };
}
