import { attendanceFixture, fakeHomework, json, onePage, schoolAdmin, SELF_SERVICE } from '@mysms/fixtures';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const homeworkId = '01960000-0000-7000-8000-0000000000b1';
const teacherPermissions = [
  ...SELF_SERVICE,
  'assessment.homework.read',
  'assessment.homework.create',
  'assessment.homework.update',
  'assessment.homework.publish_homework',
  'assessment.homework_submission.read',
  'assessment.homework_submission.check_homework_submission',
  'assessment.homework_submission.create',
  'assessment.homework_submission.reply_to_homework_feedback',
  'sis.student.read',
  'people.person.read',
] as const;

describe('assessment screens', () => {
  it('publishes a homework from the board under the row ETag and idempotency key', async () => {
    const me = { ...schoolAdmin, permissions: [...teacherPermissions] };
    const { api } = await signedIn('/app/assessment/homeworks', me, fakeHomework());
    expect(await screen.findByRole('heading', { name: 'Homework board' })).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: 'Publish' }));
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument());
    const request = api.calls.find(
      (call) => call.path === '/v1/assessment/homeworks/' + homeworkId + ':publish',
    );
    expect(request).toBeDefined();
    expect(JSON.parse(request?.body ?? '{}')).toEqual({ etag: '"1"' });
    expect(request?.headers.get('Idempotency-Key')).toBeTruthy();
  });

  it('does not show the homework publish action when the caller lacks its permission', async () => {
    const me = {
      ...schoolAdmin,
      permissions: teacherPermissions.filter(
        (permission) => permission !== 'assessment.homework.publish_homework',
      ),
    };
    await signedIn('/app/assessment/homeworks', me, fakeHomework());
    expect(await screen.findByRole('heading', { name: 'Homework board' })).toBeInTheDocument();
    await screen.findByText('Read chapter 4 and answer the questions.');
    expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument();
  });

  it('requests and reviews assignment approval records and offers Finish for published and ongoing assignments', async () => {
    const assignmentId = '01960000-0000-7000-8000-0000000000a1';
    const publishedId = '01960000-0000-7000-8000-0000000000a2';
    const ongoingId = '01960000-0000-7000-8000-0000000000a3';
    let approvals: Array<Record<string, unknown>> = [];
    const me = {
      ...schoolAdmin,
      permissions: [
        ...SELF_SERVICE,
        'assessment.assignment.read',
        'assessment.assignment_approval.read',
        'assessment.assignment_approval.create',
        'assessment.assignment.approve_assignment',
        'assessment.assignment.finish_assignment',
      ],
    };
    const { api } = await signedIn('/app/assessment/assignments', me, {
      'GET /v1/assessment/assignments': (request) =>
        onePage(request, [
          {
            id: assignmentId,
            name: 'Draft assignment',
            institutionId: 'school-1',
            status: 'draft',
            etag: '"1"',
          },
          {
            id: publishedId,
            name: 'Published assignment',
            institutionId: 'school-1',
            status: 'published',
            etag: '"1"',
          },
          {
            id: ongoingId,
            name: 'Ongoing assignment',
            institutionId: 'school-1',
            status: 'ongoing',
            etag: '"1"',
          },
        ]),
      'GET /v1/assessment/assignments/{assignmentId}/approvals': (request) => onePage(request, approvals),
      'POST /v1/assessment/assignments/{assignmentId}/approvals': async (request) => {
        const body = (await request.json()) as { decision: string };
        expect(body).toEqual({ decision: 'pending' });
        approvals = [
          {
            id: '01960000-0000-7000-8000-0000000000a4',
            assignmentId,
            decision: 'pending',
          },
        ];
        return json(approvals[0]);
      },
      ['POST /v1/assessment/assignments/' + assignmentId + ':approve']: async (request) => {
        const body = (await request.json()) as { etag?: string };
        expect(body.etag).toBe('"1"');
        return json({ id: assignmentId, status: 'published' });
      },
    });

    expect(await screen.findByRole('heading', { name: 'Assignments (official)' })).toBeInTheDocument();
    expect(await screen.findAllByRole('button', { name: 'Finish' })).toHaveLength(2);
    await userEvent.click(await screen.findByRole('button', { name: 'Request approval' }));
    expect(await screen.findByText('Approval requested.')).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: 'Approve' }));

    expect(
      api.calls.some(
        (call) =>
          call.path === `/v1/assessment/assignments/${assignmentId}/approvals` && call.method === 'POST',
      ),
    ).toBe(true);
    expect(api.calls.some((call) => call.path === `/v1/assessment/assignments/${assignmentId}:approve`)).toBe(
      true,
    );
  });

  it('creates only the signed-in student homework receipt and explains the unavailable attachment/text fields', async () => {
    const student = attendanceFixture.students[0];
    const me = {
      ...schoolAdmin,
      user: { ...schoolAdmin.user, personId: student.personId },
      studentIds: [student.id],
      permissions: [
        ...SELF_SERVICE,
        'assessment.homework.read',
        'assessment.homework_submission.read',
        'assessment.homework_submission.create',
      ],
    };
    const createdId = '01960000-0000-7000-8000-0000000000b2';
    const { api } = await signedIn('/app/assessment/homeworks/' + homeworkId, me, {
      ...fakeHomework({ status: 'published' }),
      'GET /v1/assessment/homework-submissions': (request) => onePage(request, []),
      'POST /v1/assessment/homework-submissions': async (request) => {
        const body = (await request.json()) as { homeworkId: string; studentId: string };
        return json(
          { id: createdId, ...body, status: 'unchecked', submittedOn: '2026-03-11', etag: '"1"' },
          { headers: { ETag: '"1"' } },
        );
      },
    });
    expect(await screen.findByRole('heading', { name: 'Homework submission' })).toBeInTheDocument();
    expect(screen.getByText(/has no text field/)).toBeInTheDocument();
    expect(screen.getByText('File attachments are not available yet.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Create submission' }));
    expect(await screen.findByText('Your submission is saved.')).toBeInTheDocument();
    const create = api.calls.find(
      (call) => call.path === '/v1/assessment/homework-submissions' && call.method === 'POST',
    );
    expect(JSON.parse(create?.body ?? '{}')).toEqual({ homeworkId, studentId: student.id });
    expect(create?.headers.get('Idempotency-Key')).toBeTruthy();
    expect(api.calls.some((call) => call.path.startsWith('/v1/sis/students'))).toBe(false);
  });

  it('shows submissions to a permitted reviewer without assignment-read and gates every review action separately', async () => {
    const assignmentId = '01960000-0000-7000-8000-0000000000b3';
    const studentId = '01960000-0000-7000-8000-0000000000b4';
    const me = {
      ...schoolAdmin,
      permissions: [
        ...SELF_SERVICE,
        'assessment.assignment_submission.read',
        'assessment.assignment_submission.accept_assignment_submission',
      ],
    };
    const { api } = await signedIn('/app/assessment/assignments/' + assignmentId, me, {
      'GET /v1/assessment/assignment-submissions': (request) =>
        onePage(request, [
          {
            id: '01960000-0000-7000-8000-0000000000b5',
            assignmentId,
            studentId,
            status: 'submitted',
            content: 'My completed answer',
            submittedAt: '2026-03-02T00:00:00Z',
            etag: '"1"',
          },
        ]),
    });

    expect(await screen.findByRole('heading', { name: 'Assignment grading' })).toBeInTheDocument();
    expect(await screen.findByText('My completed answer')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save grade' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Request changes' })).not.toBeInTheDocument();
    expect(api.calls.some((call) => call.path === `/v1/assessment/assignments/${assignmentId}`)).toBe(false);
  });

  it('records marks and then advances the exam result state from the authorized marks screen', async () => {
    const examId = '01960000-0000-7000-8000-0000000000e1';
    const studentId = attendanceFixture.students[0].id;
    let exam = {
      id: examId,
      name: 'Term 1 Mathematics',
      institutionId: attendanceFixture.institutionId,
      examSessionId: '01960000-0000-7000-8000-0000000000e2',
      subjectId: '01960000-0000-7000-8000-0000000000e3',
      maxMarks: '100',
      status: 'held',
      etag: '"1"',
    };
    let version = 1;
    let resultRows = [
      {
        id: '01960000-0000-7000-8000-0000000000e4',
        examId,
        studentId,
        attendanceStatus: 'expected',
        marks: '72',
        maxMarks: '100',
        etag: '"1"',
      },
      {
        id: '01960000-0000-7000-8000-0000000000e5',
        examId,
        studentId: '01960000-0000-7000-8000-000000000502',
        attendanceStatus: 'present',
        marks: '50',
        maxMarks: '100',
        etag: '"1"',
      },
    ];
    const me = {
      ...schoolAdmin,
      permissions: [
        ...SELF_SERVICE,
        'assessment.exam.read',
        'assessment.exam_result.read',
        'assessment.exam.record_exam_marks',
        'assessment.exam.mark_exam_results_updated',
      ],
    };
    const { api } = await signedIn('/app/assessment/exams/' + examId + '/marks', me, {
      'GET /v1/assessment/exams/{examId}': () => json(exam, { headers: { ETag: '"1"' } }),
      'GET /v1/assessment/exam-results': (request) => onePage(request, resultRows),
      ['POST /v1/assessment/exams/' + examId + ':recordMarks']: async (request) => {
        const body = (await request.json()) as {
          entries: Array<{ studentId: string; attendanceStatus: string; marksObtained?: string }>;
        };
        resultRows = resultRows.map((row) => {
          const entry = body.entries.find((item) => item.studentId === row.studentId);
          return entry
            ? { ...row, attendanceStatus: entry.attendanceStatus, marks: entry.marksObtained ?? row.marks }
            : row;
        });
        return json({
          items: body.entries.map((entry) => ({
            id: '01960000-0000-7000-8000-0000000000e4',
            examId,
            ...entry,
          })),
        });
      },
      ['POST /v1/assessment/exams/' + examId + ':markResultsUpdated']: async (request) => {
        const body = (await request.json()) as { etag?: string };
        expect(body.etag).toBe('"1"');
        version += 1;
        exam = { ...exam, status: 'result_updated', etag: '"' + version + '"' };
        return json(exam, { headers: { ETag: '"' + version + '"' } });
      },
    });
    expect(await screen.findByRole('heading', { name: 'Marks entry' })).toBeInTheDocument();
    await userEvent.selectOptions(screen.getAllByLabelText('Attendance')[0]!, 'present');
    const marks = screen.getAllByLabelText('Marks')[0]!;
    await userEvent.clear(marks);
    await userEvent.type(marks, '88');
    await userEvent.click(screen.getByRole('button', { name: 'Save marks' }));
    expect(await screen.findByText('Marks saved.')).toBeInTheDocument();
    const saved = api.calls.find((call) => call.path.endsWith(':recordMarks'));
    expect(JSON.parse(saved?.body ?? '{}')).toEqual({
      entries: [{ studentId, attendanceStatus: 'present', marksObtained: '88', etag: '"1"' }],
    });
    expect(api.calls.filter((call) => call.path.endsWith(':recordMarks'))).toHaveLength(1);
    expect(api.calls.some((call) => call.path.endsWith(':markResultsUpdated'))).toBe(false);
    await userEvent.click(await screen.findByRole('button', { name: 'Mark results updated' }));
    await waitFor(() =>
      expect(api.calls.some((call) => call.path.endsWith(':markResultsUpdated'))).toBe(true),
    );
    expect(api.calls.some((call) => call.path.endsWith(':recordMarks'))).toBe(true);
  });

  it('offers Complete for exams in held and singular result_updated states', async () => {
    const heldId = '01960000-0000-7000-8000-0000000000f1';
    const updatedId = '01960000-0000-7000-8000-0000000000f2';
    const me = {
      ...schoolAdmin,
      permissions: [...SELF_SERVICE, 'assessment.exam.read', 'assessment.exam.complete_exam'],
    };
    await signedIn('/app/assessment/exams', me, {
      'GET /v1/assessment/exams': (request) =>
        onePage(request, [
          { id: heldId, name: 'Held exam', institutionId: 'school-1', status: 'held', etag: '"1"' },
          {
            id: updatedId,
            name: 'Results exam',
            institutionId: 'school-1',
            status: 'result_updated',
            etag: '"1"',
          },
        ]),
    });
    expect(await screen.findByRole('heading', { name: 'Exams (papers)' })).toBeInTheDocument();
    expect(await screen.findAllByRole('button', { name: 'Complete' })).toHaveLength(2);
  });

  it('lets a student hand in their own assignment: own row only, no submission time, the etag on submit', async () => {
    const assignmentId = '01960000-0000-7000-8000-0000000000c1';
    const studentId = '01960000-0000-7000-8000-0000000000c2';
    const created = '01960000-0000-7000-8000-0000000000c3';
    const me = {
      ...schoolAdmin,
      studentIds: [studentId],
      permissions: [
        ...SELF_SERVICE,
        'assessment.assignment.read',
        'assessment.assignment_submission.read',
        'assessment.assignment_submission.create',
        'assessment.assignment_submission.submit_assignment_submission',
      ],
    };
    const { api } = await signedIn('/app/assessment/assignments/' + assignmentId, me, {
      'GET /v1/assessment/assignments/{assignmentId}': () =>
        json(
          {
            id: assignmentId,
            title: 'Essay on rivers',
            institutionId: 'school-1',
            status: 'published',
            dueAt: '2026-03-20T10:00:00Z',
            etag: '"1"',
          },
          { headers: { ETag: '"1"' } },
        ),
      'GET /v1/assessment/assignment-submissions': (request) => onePage(request, []),
      'POST /v1/assessment/assignment-submissions': async (request) =>
        json(
          { id: created, ...((await request.json()) as object), status: 'draft', etag: '"1"' },
          { headers: { ETag: '"1"' } },
        ),
      [`POST /v1/assessment/assignment-submissions/${created}:submit`]: () =>
        json({ id: created, assignmentId, studentId, status: 'submitted', etag: '"2"' }),
    });
    expect(await screen.findByRole('heading', { name: 'Assignment submission' })).toBeInTheDocument();
    expect(screen.queryByText(/2026-03-20T10:00:00Z/)).not.toBeInTheDocument();
    await userEvent.type(await screen.findByLabelText('Submission text'), 'Rivers shape valleys.');
    await userEvent.click(screen.getByRole('button', { name: 'Submit assignment' }));
    expect(await screen.findByText('Your submission is saved.')).toBeInTheDocument();
    const list = api.calls.find((call) => call.path === '/v1/assessment/assignment-submissions');
    expect(new URLSearchParams(list?.search).get('filter')).toBe(
      `assignmentId = "${assignmentId}" AND studentId = "${studentId}"`,
    );
    const create = api.calls.find(
      (call) => call.path === '/v1/assessment/assignment-submissions' && call.method === 'POST',
    );
    expect(JSON.parse(create?.body ?? '{}')).toEqual({
      assignmentId,
      studentId,
      content: 'Rivers shape valleys.',
    });
    const submit = api.calls.find((call) => call.path.endsWith(':submit'));
    expect(JSON.parse(submit?.body ?? '{}')).toEqual({ etag: '"1"' });
  });
});
