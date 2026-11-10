import {
  fakeRecords,
  json,
  onePage,
  platformAdmin,
  problem,
  schoolAdmin,
  SELF_SERVICE,
  type Handler,
} from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const root = document.documentElement;

afterEach(() => {
  for (const attribute of ['data-theme', 'data-density', 'data-reduced-motion', 'lang'])
    root.removeAttribute(attribute);
  root.style.removeProperty('--sms-text-scale');
});

describe('ACC-07 My profile', () => {
  it('shows the account and identity and saves contact details under If-Match', async () => {
    const { api } = await signedIn('/app/people/me');
    expect(await screen.findByRole('heading', { name: 'Ravi Kumar' })).toBeInTheDocument();
    expect(screen.getByText(/kept by your school/)).toBeInTheDocument();
    const email = await screen.findByLabelText('Personal e-mail');
    expect(email).toHaveValue('ravi@school.example');
    await userEvent.clear(email);
    await userEvent.type(email, 'not-an-address');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Enter an e-mail address such as name@example.org.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.method === 'PATCH')).toBe(false);
    await userEvent.clear(email);
    await userEvent.type(email, 'ravi@new.example');
    await userEvent.type(screen.getByLabelText('Web page'), 'https://ravi.example');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Your profile is saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH' && c.path === '/v1/people/me');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(JSON.parse(patch?.body ?? '{}')).toEqual({
      primaryEmail: 'ravi@new.example',
      websiteUrl: 'https://ravi.example',
    });
  });

  it('explains when the account has no person record', async () => {
    await signedIn('/app/people/me', platformAdmin);
    expect(await screen.findByText(/not linked to a person record/)).toBeInTheDocument();
    expect(screen.queryByLabelText('Personal e-mail')).not.toBeInTheDocument();
  });
});

describe('ACC-14 My settings', () => {
  it('changes theme, text size and motion and applies them to the whole app at once', async () => {
    const { api } = await signedIn('/app/iam/me/settings');
    expect(await screen.findByRole('heading', { name: 'My settings' })).toBeInTheDocument();
    expect(root).not.toHaveAttribute('data-theme');
    expect(root).toHaveAttribute('data-density', 'comfortable');
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Theme' }), 'dark');
    await waitFor(() => expect(root).toHaveAttribute('data-theme', 'dark'));
    expect(api.calls.find((c) => c.method === 'PUT')?.path).toBe('/v1/iam/me/settings/ui.theme');
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Theme' }), 'high_contrast');
    await waitFor(() => expect(root).toHaveAttribute('data-theme', 'high-contrast'));
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Text size' }), '125');
    await waitFor(() => expect(root.style.getPropertyValue('--sms-text-scale')).toBe('1.25'));
    await userEvent.click(screen.getByRole('switch', { name: 'Reduce motion' }));
    await waitFor(() => expect(root).toHaveAttribute('data-reduced-motion', 'true'));
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Density' }), 'compact');
    await waitFor(() => expect(root).toHaveAttribute('data-density', 'compact'));

    const themeRow = screen.getByRole('combobox', { name: 'Theme' }).closest('li') as HTMLElement;
    expect(within(themeRow).getByText('Your choice')).toBeInTheDocument();
    await userEvent.click(within(themeRow).getByRole('button', { name: 'Use the inherited value' }));
    await waitFor(() => expect(root).not.toHaveAttribute('data-theme'));
    expect(within(themeRow).getByText('Default')).toBeInTheDocument();
  });

  it('switches the language and shows administrator-only settings as read-only', async () => {
    await signedIn('/app/iam/me/settings');
    const timeout = await screen.findByLabelText('Sign out after inactivity (minutes)');
    expect(timeout).toBeDisabled();
    expect(screen.getAllByText('Set by administrators').length).toBeGreaterThan(0);
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'hi');
    expect(await screen.findByRole('heading', { name: 'मेरी सेटिंग्स' })).toBeInTheDocument();
    await waitFor(() => expect(root).toHaveAttribute('lang', 'hi'));
  });
});

describe('ACC-14 for users who do not administer settings', () => {
  it('does not show administrator-only settings at all', async () => {
    await signedIn('/app/iam/me/settings', platformAdmin);
    expect(await screen.findByRole('combobox', { name: 'Theme' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Sign out after inactivity (minutes)')).not.toBeInTheDocument();
    expect(screen.queryByText('Set by administrators')).not.toBeInTheDocument();
  });
});

describe('ADM-16 Settings by scope', () => {
  it('creates a locked school-wide value that users then see as locked', async () => {
    await signedIn('/app/iam/setting-values');
    expect(await screen.findByRole('heading', { name: 'Settings by scope' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'New value' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.selectOptions(within(dialog).getByRole('combobox', { name: 'Setting' }), 'ui.theme');
    await userEvent.selectOptions(within(dialog).getByRole('combobox', { name: 'Scope' }), 'tenant');
    await userEvent.selectOptions(within(dialog).getByRole('combobox', { name: 'Value' }), 'dark');
    await userEvent.click(within(dialog).getByRole('switch', { name: 'Locked' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Value saved.')).toBeInTheDocument();
    const table = screen.getByRole('table');
    expect(within(table).getByText('Whole school')).toBeInTheDocument();
    expect(within(table).getByText('Dark')).toBeInTheDocument();
    // the administrator's own effective settings follow the lock at once
    await waitFor(() => expect(root).toHaveAttribute('data-theme', 'dark'));
  });

  it('asks for a unit id for narrower scopes and edits a value under If-Match', async () => {
    const { api } = await signedIn('/app/iam/setting-values');
    await userEvent.click(await screen.findByRole('button', { name: 'New value' }));
    let dialog = await screen.findByRole('dialog');
    await userEvent.selectOptions(within(dialog).getByRole('combobox', { name: 'Setting' }), 'ui.density');
    await userEvent.selectOptions(within(dialog).getByRole('combobox', { name: 'Scope' }), 'department');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await within(dialog).findByText('Use the format shown.')).toBeInTheDocument();
    await userEvent.type(within(dialog).getByLabelText('Unit id'), '01960000-0000-7000-8000-0000000000e1');
    await userEvent.selectOptions(within(dialog).getByRole('combobox', { name: 'Value' }), 'compact');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Value saved.')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Department · 01960000'));
    dialog = await screen.findByRole('dialog');
    await userEvent.selectOptions(within(dialog).getByRole('combobox', { name: 'Value' }), 'comfortable');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Value saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(JSON.parse(patch?.body ?? '{}')).toEqual({ value: 'comfortable' });
  });

  it('is offered in the navigation only to callers who may read values', async () => {
    await signedIn('/app/iam/me/settings');
    const nav = await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).getByRole('link', { name: 'Settings by scope' })).toBeInTheDocument();
    expect(within(nav).getByRole('link', { name: 'My settings' })).toBeInTheDocument();
  });

  it('hides the administration link from other users', async () => {
    await signedIn('/app/iam/me/settings', platformAdmin);
    const nav = await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).queryByRole('link', { name: 'Settings by scope' })).not.toBeInTheDocument();
  });
});

describe('ADM-11 Records', () => {
  const categories = () =>
    fakeRecords({
      collection: '/v1/sis/student-categories',
      item: '/v1/sis/student-categories/{studentCategoryId}',
      required: ['institutionId', 'code', 'name'],
      rows: [
        {
          code: 'GEN',
          name: 'General',
          isActive: true,
          institutionId: '01960000-0000-7000-8000-0000000000c1',
        },
      ],
    });
  const registrar = {
    ...schoolAdmin,
    permissions: [...SELF_SERVICE, 'sis.student_category.*'],
  };

  it('lists, creates, edits and deletes the record types the role may use', async () => {
    const { api } = await signedIn('/app/admin/records', registrar, categories());
    const picker = await screen.findByRole('combobox', { name: 'Record type' });
    expect(
      within(picker)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['sis · Student category']);
    expect(await screen.findByText('General')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'New record' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getAllByText('This field is required.')).toHaveLength(3);
    await userEvent.type(
      within(dialog).getByLabelText('Institution'),
      '01960000-0000-7000-8000-0000000000c1',
    );
    await userEvent.type(within(dialog).getByLabelText('Code'), 'RTE');
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Right to education');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Saved.')).toBeInTheDocument();
    const post = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/sis/student-categories');
    expect(JSON.parse(post?.body ?? '{}')).toEqual({
      isActive: true,
      institutionId: '01960000-0000-7000-8000-0000000000c1',
      code: 'RTE',
      name: 'Right to education',
    });

    await userEvent.click(await screen.findByText('Right to education'));
    const edit = await screen.findByRole('dialog');
    const name = await within(edit).findByLabelText('Name');
    await userEvent.clear(name);
    await userEvent.type(name, 'RTE quota');
    await userEvent.click(within(edit).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('RTE quota')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(JSON.parse(patch?.body ?? '{}')).toMatchObject({ code: 'RTE', name: 'RTE quota' });

    await userEvent.click(screen.getByText('RTE quota'));
    const del = await screen.findByRole('dialog');
    await userEvent.click(await within(del).findByRole('button', { name: 'Delete' }));
    await userEvent.click(within(del).getByRole('button', { name: /Delete this record/ }));
    expect(await screen.findByText('Deleted.')).toBeInTheDocument();
    expect(api.calls.find((c) => c.method === 'DELETE')?.headers.get('If-Match')).toBe('"2"');
  });

  it('offers the state transitions the role holds, only in the states that allow them', async () => {
    const notices = fakeRecords({
      collection: '/v1/comms/notices',
      item: '/v1/comms/notices/{noticeId}',
      required: ['title'],
      rows: [{ title: 'Sports day', isPublished: false, isClosed: false }],
    });
    const deactivate: Handler = (_request, params) =>
      new Response(
        JSON.stringify({ id: params.noticeId, title: 'Sports day', isClosed: true, etag: '"2"' }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json', ETag: '"2"' },
        },
      );
    const editor = {
      ...schoolAdmin,
      permissions: [
        ...SELF_SERVICE,
        'comms.notice.read',
        'comms.notice.deactivate_notice',
        'comms.notice.unpublish_notice',
      ],
    };
    const { api } = await signedIn('/app/admin/records', editor, {
      ...notices,
      'POST /v1/comms/notices/{noticeId}:deactivate': deactivate,
    });
    await userEvent.click(await screen.findByText('Sports day'));
    const dialog = await screen.findByRole('dialog');
    const actions = await within(dialog).findByRole('group', { name: 'Actions' });
    // Publish needs a permission the role lacks; Unpublish and Reactivate are not allowed from this state
    expect(
      within(actions)
        .getAllByRole('button')
        .map((b) => b.textContent),
    ).toEqual(['Deactivate']);
    await userEvent.click(within(actions).getByRole('button', { name: 'Deactivate' }));
    await userEvent.type(within(actions).getByLabelText('Reason (optional)'), 'Postponed');
    await userEvent.click(within(actions).getByRole('button', { name: 'Confirm: Deactivate' }));
    expect(await screen.findByText('Deactivate: done.')).toBeInTheDocument();
    const post = api.calls.find((c) => c.method === 'POST' && c.path.endsWith(':deactivate'));
    expect(JSON.parse(post?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Postponed' });
  });

  it('lists a partitioned record type one date range at a time', async () => {
    const days = fakeRecords({
      collection: '/v1/attendance/staff-attendance-days',
      item: '/v1/attendance/staff-attendance-days/{staffAttendanceId}',
      required: ['staffId', 'attendanceDate', 'status'],
      rows: [
        { staffId: '01960000-0000-7000-8000-0000000000d1', attendanceDate: '2026-11-02', status: 'present' },
      ],
    });
    const clerk = { ...schoolAdmin, permissions: [...SELF_SERVICE, 'attendance.staff_attendance.read'] };
    const { api } = await signedIn('/app/admin/records', clerk, days);
    const from = await screen.findByLabelText('From');
    const to = screen.getByLabelText('To');
    await waitFor(() =>
      expect(api.calls.some((c) => c.path === '/v1/attendance/staff-attendance-days')).toBe(true),
    );
    const listed = api.calls.find((c) => c.path === '/v1/attendance/staff-attendance-days');
    const filter = new URLSearchParams(listed?.search).get('filter') ?? '';
    expect(filter).toMatch(
      /^attendanceDate >= "\d{4}-\d{2}-\d{2}" AND attendanceDate <= "\d{4}-\d{2}-\d{2}"$/,
    );
    expect(filter).toContain(`<= "${(to as HTMLInputElement).value}"`);
    expect(filter).toContain(`>= "${(from as HTMLInputElement).value}"`);

    // a range that ends before it starts is not sent
    const before = api.calls.length;
    fireEvent.change(from, { target: { value: '2099-01-01' } });
    expect(await screen.findByText(/Choose a range that ends after it starts/)).toBeInTheDocument();
    expect(
      api.calls.slice(before).filter((c) => c.path === '/v1/attendance/staff-attendance-days'),
    ).toHaveLength(0);
  });

  it('offers nothing to a role without record permissions', async () => {
    // the job-centre permissions only; platformAdmin itself now holds the global record types (PLT-01…05)
    const operator = { ...platformAdmin, permissions: ['platform.operation.read', ...SELF_SERVICE] };
    await signedIn('/app/admin/records', operator);
    expect(await screen.findByText('No record types are available to your role.')).toBeInTheDocument();
  });

  it('shows rows read-only when the role may only read', async () => {
    const reader = { ...schoolAdmin, permissions: [...SELF_SERVICE, 'sis.student_category.read'] };
    await signedIn('/app/admin/records', reader, categories());
    expect(screen.queryByRole('button', { name: 'New record' })).not.toBeInTheDocument();
    await userEvent.click(await screen.findByText('General'));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByLabelText('Name')).toBeDisabled();
    expect(within(dialog).queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
  });
});

describe('list-manager screens of the inventory', () => {
  const students = () =>
    fakeRecords({
      collection: '/v1/sis/students',
      item: '/v1/sis/students/{studentId}',
      required: ['personId', 'institutionId', 'admissionNumber'],
      rows: [
        { admissionNumber: 'A-001', status: 'active', admissionDate: '2026-06-01' },
        { admissionNumber: 'A-002', status: 'alumni', admissionDate: '2020-06-01' },
      ],
    });
  const office = { ...schoolAdmin, permissions: [...SELF_SERVICE, 'sis.student.read'] };

  it('SIS-01 Students has its own heading, columns, status filter and navigation entry', async () => {
    const { api } = await signedIn('/app/sis/students', office, students());
    expect(await screen.findByRole('heading', { name: 'Students' })).toBeInTheDocument();
    expect(await screen.findByText('A-001')).toBeInTheDocument();
    const table = screen.getByRole('table');
    expect(within(table).getByRole('columnheader', { name: 'Admission number' })).toBeInTheDocument();
    expect(within(table).getByRole('columnheader', { name: 'Status' })).toBeInTheDocument();
    // a reader gets no create button
    expect(screen.queryByRole('button', { name: 'New record' })).not.toBeInTheDocument();

    fireEvent.change(screen.getByRole('combobox', { name: 'Status' }), { target: { value: 'alumni' } });
    await waitFor(() =>
      expect(
        api.calls.some(
          (c) =>
            c.path === '/v1/sis/students' &&
            new URLSearchParams(c.search).get('filter') === 'status = "alumni"',
        ),
      ).toBe(true),
    );

    // the navigation shows the Students & staff group open on this screen, and no group the role cannot open
    const nav = screen.getByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).getByText('Students & staff').closest('details')).toHaveAttribute('open');
    expect(within(nav).getByRole('link', { name: 'Students' })).toHaveAttribute('href', '/app/sis/students');
    expect(within(nav).queryByText('Money')).not.toBeInTheDocument();

    // "find a screen": the shortcut focuses the box, and Enter opens the first match
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
    const find = within(nav).getByRole('searchbox', { name: 'Find a screen' });
    expect(find).toHaveFocus();
    await userEvent.type(find, 'stud');
    expect(within(nav).getByRole('list', { name: 'Matching screens' })).toBeInTheDocument();
    await userEvent.type(find, 'zzz');
    expect(within(nav).getByText('No screen matches.')).toBeInTheDocument();
  });

  it('ACA-10 Lesson plans lets the head approve a plan that waits for review, with a reason', async () => {
    const plans = fakeRecords({
      collection: '/v1/academics/lesson-plans',
      item: '/v1/academics/lesson-plans/{lessonPlanId}',
      required: ['title'],
      rows: [{ title: 'Friction', unitNo: '3', unitName: 'Forces', status: 'pending', isPublished: false }],
    });
    const approve: Handler = (_request, params) =>
      new Response(
        JSON.stringify({ id: params.lessonPlanId, title: 'Friction', status: 'approved', etag: '"2"' }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json', ETag: '"2"' },
        },
      );
    const backend = { ...plans, 'POST /v1/academics/lesson-plans/{lessonPlanId}:approve': approve };
    const read = [...SELF_SERVICE, 'academics.lesson_plan.read'];
    const head = {
      ...schoolAdmin,
      permissions: [
        ...read,
        'academics.lesson_plan.approve_lesson_plan',
        'academics.lesson_plan.reject_lesson_plan',
        'academics.lesson_plan.publish_lesson_plan',
      ],
    };

    // the approval queue is the list filtered in the address
    const review = await signedIn('/app/academics/lesson-plans?status=pending', head, backend);
    expect(await screen.findByRole('heading', { name: 'Lesson plans' })).toBeInTheDocument();
    const listed = review.api.calls.filter((c) => c.path === '/v1/academics/lesson-plans').pop();
    expect(new URLSearchParams(listed?.search).get('filter')).toBe('status = "pending"');
    await userEvent.click(await screen.findByText('Friction'));
    const actions = await within(await screen.findByRole('dialog')).findByRole('group', { name: 'Actions' });
    // a plan is published only after it is approved, so Publish is not offered yet
    expect(
      within(actions)
        .getAllByRole('button')
        .map((b) => b.textContent),
    ).toEqual(['Approve', 'Reject']);
    await userEvent.click(within(actions).getByRole('button', { name: 'Approve' }));
    await userEvent.type(within(actions).getByLabelText('Reason (optional)'), 'Clear and complete');
    await userEvent.click(within(actions).getByRole('button', { name: 'Confirm: Approve' }));
    expect(await screen.findByText('Approve: done.')).toBeInTheDocument();
    const post = review.api.calls.find((c) => c.method === 'POST' && c.path.endsWith(':approve'));
    expect(JSON.parse(post?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Clear and complete' });
    review.unmount();

    // the teacher who wrote the plan has no decision to take on it
    const teacher = { ...schoolAdmin, permissions: [...read, 'academics.lesson_plan.submit_lesson_plan'] };
    await signedIn('/app/academics/lesson-plans', teacher, backend);
    await userEvent.click(await screen.findByText('Friction'));
    const dialog = await screen.findByRole('dialog');
    await within(dialog).findByDisplayValue('Forces');
    expect(within(dialog).queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
  });

  it('tells a role without the list permission instead of showing a broken list', async () => {
    await signedIn('/app/hr/staff', office);
    expect(await screen.findByRole('heading', { name: 'Staff directory' })).toBeInTheDocument();
    expect(screen.getByText('Your role may not list these records.')).toBeInTheDocument();
  });
});

describe('ACC-08 Security & sessions', () => {
  // every role manages its own factors and sessions (self-service permissions, iam V1_030_005)
  const user = { ...schoolAdmin, permissions: [...SELF_SERVICE] };
  const userId = schoolAdmin.user?.id ?? '';
  const handlers = (): Record<string, Handler> => {
    let credentials: Record<string, unknown>[] = [];
    let sessions = [
      { id: 's1', sessionRef: 'r1', userAgent: 'Firefox on Linux', ipAddress: '10.0.0.7', etag: '"3"' },
    ];
    return {
      'GET /v1/iam/mfa': () => json({ items: credentials }),
      'POST /v1/iam/mfa:enroll': () => {
        credentials = [{ id: 'm1', credentialType: 'totp', label: 'Phone', isActive: true }];
        return json({
          mfaCredentialId: 'm1',
          mfaToken: 'enrol-token',
          secret: 'JBSWY3DPEHPK3PXP',
          otpauthUrl: 'otpauth://totp/MySMS:admin?secret=JBSWY3DPEHPK3PXP',
          recoveryCodes: ['aaaa-1111', 'bbbb-2222'],
        });
      },
      'POST /v1/iam/mfa:complete': async (req) => {
        const body = (await req.json()) as { code: string };
        if (body.code !== '123456') return problem(401, 'MFA_INVALID');
        credentials = [{ ...credentials[0], verifiedAt: '2026-11-07T08:00:00Z' }];
        return json({ mfaRequired: false });
      },
      'POST /v1/iam/mfa:disable': () => {
        credentials = [];
        return new Response(null, { status: 204 });
      },
      'POST /v1/iam/auth:changePassword': () => new Response(null, { status: 204 }),
      [`GET /v1/iam/users/{userAccountId}/sessions`]: (req) => onePage(req, sessions),
      [`DELETE /v1/iam/users/{userAccountId}/sessions/{userSessionId}`]: (req) => {
        if (req.headers.get('If-Match') !== '"3"') return problem(412, 'PRECONDITION_FAILED');
        sessions = [];
        return new Response(null, { status: 204 });
      },
    };
  };

  it('changes the password only with a long enough, repeated new password', async () => {
    const { api } = await signedIn('/app/iam/me/security', user, handlers());
    await userEvent.type(await screen.findByLabelText('Current password'), 'password');
    await userEvent.type(screen.getByLabelText('New password'), 'short');
    await userEvent.click(screen.getByRole('button', { name: 'Change password' }));
    expect(screen.getByText('Use at least 12 characters.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.path === '/v1/iam/auth:changePassword')).toBe(false);
    await userEvent.clear(screen.getByLabelText('New password'));
    await userEvent.type(screen.getByLabelText('New password'), 'a-much-longer-pass');
    await userEvent.type(screen.getByLabelText('Repeat the new password'), 'a-much-longer-pass');
    await userEvent.click(screen.getByRole('button', { name: 'Change password' }));
    expect(await screen.findByText(/Password changed/)).toBeInTheDocument();
    const post = api.calls.find((c) => c.path === '/v1/iam/auth:changePassword');
    expect(JSON.parse(post?.body ?? '{}')).toEqual({
      currentPassword: 'password',
      newPassword: 'a-much-longer-pass',
    });
  });

  it('adds an authenticator app: secret and recovery codes shown once, then verified with a first code', async () => {
    const { api } = await signedIn('/app/iam/me/security', user, handlers());
    expect(await screen.findByText('No second factor yet.')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Name for this app (optional)'), 'Phone');
    await userEvent.click(screen.getByRole('button', { name: 'Add an authenticator app' }));
    expect(await screen.findByText('JBSWY3DPEHPK3PXP')).toBeInTheDocument();
    expect(screen.getByText('aaaa-1111')).toBeInTheDocument();
    expect(JSON.parse(api.calls.find((c) => c.path === '/v1/iam/mfa:enroll')?.body ?? '{}')).toEqual({
      credentialType: 'totp',
      label: 'Phone',
    });
    await userEvent.type(screen.getByLabelText('First code from the app'), '000000');
    await userEvent.click(screen.getByRole('button', { name: 'Verify and turn on' }));
    expect(
      await screen.findByText('That was not accepted. Check what you entered and try again.'),
    ).toBeInTheDocument();
    await userEvent.clear(screen.getByLabelText('First code from the app'));
    await userEvent.type(screen.getByLabelText('First code from the app'), '123456');
    await userEvent.click(screen.getByRole('button', { name: 'Verify and turn on' }));
    expect(await screen.findByText('Two-step sign-in is on.')).toBeInTheDocument();
    expect(screen.queryByText('JBSWY3DPEHPK3PXP')).not.toBeInTheDocument();
    expect(await screen.findByText('In use')).toBeInTheDocument();
    const complete = api.calls.filter((c) => c.path === '/v1/iam/mfa:complete').pop();
    expect(JSON.parse(complete?.body ?? '{}')).toEqual({ mfaToken: 'enrol-token', code: '123456' });
  });

  it('lists the signed-in sessions and ends one under If-Match', async () => {
    const { api } = await signedIn('/app/iam/me/security', user, handlers());
    expect(await screen.findByText('Firefox on Linux')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'End session' }));
    await waitFor(() => expect(screen.queryByText('Firefox on Linux')).not.toBeInTheDocument());
    const del = api.calls.find((c) => c.method === 'DELETE');
    expect(del?.path).toBe(`/v1/iam/users/${userId}/sessions/s1`);
    expect(del?.headers.get('If-Match')).toBe('"3"');
  });

  it('shows no sessions section to an account that may not read sessions', async () => {
    const limited = {
      ...schoolAdmin,
      permissions: SELF_SERVICE.filter((code) => !code.startsWith('iam.user_session.')),
    };
    await signedIn('/app/iam/me/security', limited, handlers());
    expect(await screen.findByRole('heading', { name: 'Two-step sign-in' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Signed-in sessions' })).not.toBeInTheDocument();
  });
});
