import { HR_OFFICER, SELF_SERVICE, fakeStaff, schoolAdmin, staffFixture } from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const fx = staffFixture;
const officer = {
  ...schoolAdmin,
  institutions: [{ id: fx.institutionId, code: 'GVS', name: 'Green Valley School' }],
  permissions: [...SELF_SERVICE, ...HR_OFFICER],
};
/** A colleague who may read the directory but change nothing, nor see salaries. */
const reader = {
  ...schoolAdmin,
  permissions: [...SELF_SERVICE, 'hr.staff.read', 'people.person.read', 'hr.designation.read'],
};
const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;
const detail = (staffId: string) => `/app/hr/staff/${staffId}`;

describe('HR-01 Staff directory', () => {
  it('lists staff with their names and narrows them by status, designation and search', async () => {
    const { api, router } = await signedIn('/app/hr/staff', officer, fakeStaff());
    // the table is drawn anew while a narrowed list loads: read it afresh each time
    const table = () => screen.getByRole('table', { name: 'Staff directory' });
    await screen.findByRole('table', { name: 'Staff directory' });
    expect(
      await within(table()).findByRole('link', { name: 'Open the record of Anil Kumar' }),
    ).toBeInTheDocument();
    expect(within(table()).getByText('Meera Iyer')).toBeInTheDocument();
    expect(within(table()).getByText('Kavya Rao')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'Left');
    await waitFor(() => expect(within(table()).queryByText('Anil Kumar')).not.toBeInTheDocument());
    expect(router.state.location.search).toBe('?status=exited');
    expect(await within(table()).findByText('Kavya Rao')).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'All');

    await userEvent.selectOptions(await screen.findByLabelText('Designation'), 'Office clerk');
    await waitFor(() => expect(within(table()).queryByText('Anil Kumar')).not.toBeInTheDocument());
    expect(within(table()).getByText('Meera Iyer')).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Designation'), 'All');

    // a search finds a name through the persons it matches, and an employee number directly
    await userEvent.type(screen.getByLabelText('Employee number or name'), 'anil');
    await waitFor(() => expect(within(table()).queryByText('Meera Iyer')).not.toBeInTheDocument());
    expect(within(table()).getByText('Anil Kumar')).toBeInTheDocument();
    await userEvent.clear(screen.getByLabelText('Employee number or name'));
    await userEvent.type(screen.getByLabelText('Employee number or name'), 'O-2');
    await waitFor(() => expect(within(table()).queryByText('Anil Kumar')).not.toBeInTheDocument());
    expect(within(table()).getByText('Meera Iyer')).toBeInTheDocument();
    const filters = api.calls
      .filter((c) => c.path === '/v1/hr/staff')
      .map((c) => new URLSearchParams(c.search).get('filter'));
    expect(filters).toContain(`(employeeCode : "anil" OR personId = "${fx.teacherPersonId}")`);

    await userEvent.click(within(table()).getByRole('link', { name: 'Open the record of Meera Iyer' }));
    await waitFor(() => expect(router.state.location.pathname).toBe(detail(fx.colleagueId)));
  });

  it('adds a staff member for a person found by name, then opens their record', async () => {
    const { api, router } = await signedIn('/app/hr/staff', officer, fakeStaff());
    await userEvent.click(await screen.findByRole('button', { name: 'Add staff member' }));
    const dialog = await screen.findByRole('dialog', { name: 'New staff member' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    // a staff record is a person's: none chosen yet
    expect(within(dialog).getByText('This field is required.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.method === 'POST' && c.path === '/v1/hr/staff')).toBe(false);
    await userEvent.type(within(dialog).getByLabelText('Find the person by name'), 'zz');
    expect(await within(dialog).findByText('No person matches this name.')).toBeInTheDocument();
    await userEvent.clear(within(dialog).getByLabelText('Find the person by name'));
    await userEvent.type(within(dialog).getByLabelText('Find the person by name'), 'kav');
    await userEvent.selectOptions(await within(dialog).findByLabelText('Person'), 'Kavya Rao');
    await userEvent.type(within(dialog).getByLabelText('Employee number'), 'N-301');
    await userEvent.selectOptions(await within(dialog).findByLabelText('Designation'), 'Teacher');
    await userEvent.selectOptions(within(dialog).getByLabelText('Employment type'), 'Contract');
    await userEvent.type(within(dialog).getByLabelText('Most periods per week'), 'x');
    expect(within(dialog).getByText('Type a whole number.')).toBeInTheDocument();
    await userEvent.clear(within(dialog).getByLabelText('Most periods per week'));
    await userEvent.type(within(dialog).getByLabelText('Most periods per week'), '28');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    // the fake keeps one staff record per person: Kavya already has one
    expect(await within(dialog).findByText(/already exists/)).toBeInTheDocument();
    const post = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/hr/staff');
    expect(JSON.parse(post?.body ?? '{}')).toMatchObject({
      personId: fx.leaverPersonId,
      institutionId: fx.institutionId,
      employeeCode: 'N-301',
      designationId: fx.teacherDesignationId,
      employmentType: 'contract',
      maxPeriodsPerWeek: 28,
    });
    expect(router.state.location.pathname).toBe('/app/hr/staff');

    await userEvent.selectOptions(within(dialog).getByLabelText('Person'), 'Kavitha Menon');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/app\/hr\/staff\/[0-9a-f-]{36}$/));
    expect(await screen.findByText('The staff member is added.')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Kavitha Menon' })).toBeInTheDocument();
  });

  it('offers no adding to a role that may only read, and says so to a role that may not read', async () => {
    await signedIn('/app/hr/staff', reader, fakeStaff());
    expect(await screen.findByRole('table', { name: 'Staff directory' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add staff member' })).not.toBeInTheDocument();
  });

  it('tells a role without the permission that it may not open staff records', async () => {
    await signedIn('/app/hr/staff', { ...schoolAdmin, permissions: [...SELF_SERVICE] }, fakeStaff());
    expect(await screen.findByText('Your role may not open staff records.')).toBeInTheDocument();
  });
});

describe('HR-02 Staff detail', () => {
  it('suspends, takes back and, after a confirmation, marks a staff member as left', async () => {
    const { api } = await signedIn(detail(fx.colleagueId), officer, fakeStaff());
    expect(await screen.findByRole('heading', { name: 'Meera Iyer' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Make inactive' }));
    expect(await screen.findByText('The staff member is now Inactive.')).toBeInTheDocument();
    const step = api.calls.find((c) => c.path.endsWith(':changeStatus'));
    expect(JSON.parse(step?.body ?? '{}')).toEqual({ status: 'inactive', etag: '"1"' });
    await userEvent.click(await screen.findByRole('button', { name: 'Make active' }));
    expect(await screen.findByText('The staff member is now Active.')).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: 'Mark as left' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm: Meera Iyer has left' }));
    expect(await screen.findByText('The staff member is now Left.')).toBeInTheDocument();
    // one who left may only come back, and is given no login account
    expect(await screen.findByRole('button', { name: 'Make active' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Make inactive' })).not.toBeInTheDocument();
    expect(screen.getByText('A staff member who has left is not given a login account.')).toBeInTheDocument();
  });

  it('creates the login account of a staff member who has none, once', async () => {
    const { api } = await signedIn(detail(fx.colleagueId), officer, fakeStaff());
    await userEvent.click(await screen.findByRole('button', { name: 'Create login account' }));
    expect(await screen.findByText(/its login name is the employee number O-201/)).toBeInTheDocument();
    expect(await screen.findByText('Has a login account.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Create login account' })).not.toBeInTheDocument();
    expect(api.calls.filter((c) => c.path.endsWith(':createPortalAccount'))).toHaveLength(1);
  });

  it('says when the service that keeps login accounts cannot answer', async () => {
    await signedIn(detail(fx.colleagueId), officer, fakeStaff({ accountUnavailable: true }));
    await userEvent.click(await screen.findByRole('button', { name: 'Create login account' }));
    expect(
      await screen.findByText('The service that keeps login accounts could not answer. Try again later.'),
    ).toBeInTheDocument();
  });

  it('edits only the fields that changed, under the ETag last read', async () => {
    const { api } = await signedIn(detail(fx.teacherId), officer, fakeStaff());
    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }));
    const dialog = await screen.findByRole('dialog', { name: 'Edit staff member' });
    fireEvent.change(within(dialog).getByLabelText('Specialisation'), { target: { value: 'Optics' } });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('The staff record is saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(new URLSearchParams(patch?.search).get('updateMask')).toBe('specialization');
    expect(await screen.findByText('Optics')).toBeInTheDocument();
  });

  it('keeps departments and subjects: adds, re-points and removes them', async () => {
    const { api } = await signedIn(detail(fx.teacherId), officer, fakeStaff());
    await userEvent.click(await screen.findByRole('radio', { name: 'Departments & subjects' }));
    const departments = card('Departments');
    expect(await within(departments).findByRole('button', { name: 'Remove Science' })).toBeInTheDocument();
    await userEvent.selectOptions(
      within(departments).getByLabelText('Department to add'),
      'Administration office',
    );
    await userEvent.click(within(departments).getByRole('button', { name: 'Add' }));
    expect(
      await within(departments).findByRole('button', { name: 'Remove Administration office' }),
    ).toBeInTheDocument();
    await userEvent.click(within(departments).getByRole('button', { name: 'Remove Science' }));
    await waitFor(() =>
      expect(within(departments).queryByRole('button', { name: 'Remove Science' })).not.toBeInTheDocument(),
    );
    const removal = api.calls.find((c) => c.method === 'DELETE');
    expect(removal?.headers.get('If-Match')).toBe('"1"');

    const subjects = card('Subjects');
    expect(await within(subjects).findByRole('button', { name: 'Remove Physics' })).toBeInTheDocument();
    await userEvent.selectOptions(within(subjects).getByLabelText('Change Physics to'), 'Chemistry');
    await userEvent.click(within(subjects).getByRole('button', { name: 'Change' }));
    expect(await within(subjects).findByRole('button', { name: 'Remove Chemistry' })).toBeInTheDocument();
  });

  it('shows the leave position and applications, and the salary rows, read-only', async () => {
    await signedIn(detail(fx.teacherId), officer, fakeStaff());
    await userEvent.click(await screen.findByRole('radio', { name: 'Leave' }));
    const position = await screen.findByRole('table', { name: 'Leave position' });
    const casual = (await within(position).findByText('Casual leave')).closest('tr') as HTMLElement;
    expect(
      within(casual)
        .getAllByRole('cell')
        .map((cell) => cell.textContent),
    ).toEqual(['Casual leave', '12', '3', '0', '9']);
    expect(screen.getByRole('link', { name: 'Open leave applications' })).toHaveAttribute(
      'href',
      '/app/hr/leave-applications',
    );
    await userEvent.click(screen.getByRole('radio', { name: 'Salary' }));
    const salary = await screen.findByRole('table', { name: 'Salary' });
    expect(await within(salary).findByText('Annual revision')).toBeInTheDocument();
    expect(within(salary).getByText('52,000')).toBeInTheDocument();
  });

  it('offers a reader no steps and no salary, and a missing record is not available', async () => {
    await signedIn(detail(fx.teacherId), reader, fakeStaff());
    expect(await screen.findByRole('heading', { name: 'Anil Kumar' })).toBeInTheDocument();
    expect(screen.getByText('T-101')).toBeInTheDocument();
    for (const name of ['Edit', 'Make inactive', 'Delete', 'Create login account'])
      expect(screen.queryByRole('button', { name })).not.toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: 'Salary' })).not.toBeInTheDocument();
  });

  it('says a record that does not exist is not available', async () => {
    await signedIn(detail('01960000-0000-7000-8000-00000000ffff'), officer, fakeStaff());
    expect(
      await screen.findByText('This staff record does not exist or is not available to your role.'),
    ).toBeInTheDocument();
  });

  it('deletes a staff record after a confirmation and returns to the directory', async () => {
    const { router } = await signedIn(detail(fx.leaverId), officer, fakeStaff());
    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm delete' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/app/hr/staff'));
    expect(await screen.findByText('The staff record is deleted.')).toBeInTheDocument();
  });
});

describe('HR-03 Leave types & balance', () => {
  it('lists the year’s balances, opens one, and refuses an entitlement below the days used', async () => {
    const { api } = await signedIn('/app/hr/leave-types', officer, fakeStaff());
    const balances = await screen.findByRole('table', { name: 'Leave balances' });
    const row = (await within(balances).findByText('Anil Kumar')).closest('tr') as HTMLElement;
    expect(
      within(row)
        .getAllByRole('cell')
        .map((cell) => cell.textContent)
        .slice(0, 5),
    ).toEqual(['Anil Kumar', 'Casual leave', '12', '0', '3']);

    await userEvent.click(within(row).getByRole('button', { name: 'Adjust the entitlement of Anil Kumar' }));
    const adjust = await screen.findByRole('dialog', { name: 'Adjust the entitlement' });
    fireEvent.change(within(adjust).getByLabelText('Entitled'), { target: { value: '2' } });
    await userEvent.click(within(adjust).getByRole('button', { name: 'Save' }));
    expect(await within(adjust).findByText(/is below the days already used/)).toBeInTheDocument();
    fireEvent.change(within(adjust).getByLabelText('Entitled'), { target: { value: '14.5' } });
    await userEvent.click(within(adjust).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('The balance is saved.')).toBeInTheDocument();
    const patch = api.calls.filter((c) => c.method === 'PATCH').at(-1);
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(JSON.parse(patch?.body ?? '{}')).toEqual({ entitledDays: '14.5', carriedForwardDays: '0' });

    await userEvent.click(screen.getByRole('button', { name: 'Open a balance' }));
    const open = await screen.findByRole('dialog', { name: 'Open a leave balance' });
    await userEvent.type(within(open).getByLabelText('Find the staff member'), 'meera');
    await userEvent.selectOptions(
      await within(open).findByRole('combobox', { name: 'Staff member' }),
      'Meera Iyer',
    );
    await userEvent.selectOptions(within(open).getByLabelText('Leave type'), 'Sick leave');
    expect(within(open).getByLabelText('Entitled')).toHaveValue('6');
    await userEvent.click(within(open).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('The balance is saved.')).toBeInTheDocument();
    const post = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/hr/leave-balances');
    expect(JSON.parse(post?.body ?? '{}')).toEqual({
      staffId: fx.colleagueId,
      leaveTypeId: fx.sickId,
      academicYearId: fx.academicYearId,
      entitledDays: '6',
    });
  });

  it('shows a staff member their own position, and no balances of others', async () => {
    const teacher = {
      ...schoolAdmin,
      user: { ...schoolAdmin.user, id: fx.accountId },
      staffIds: [fx.teacherId],
      permissions: [
        ...SELF_SERVICE,
        'hr.staff.read',
        'hr.staff.get_leave_balance',
        'hr.leave_type.read',
        'org.academic_year.read',
      ],
    };
    await signedIn('/app/hr/leave-types', teacher, fakeStaff());
    const mine = await screen.findByRole('table', { name: 'My leave this year' });
    expect(await within(mine).findByText('Casual leave')).toBeInTheDocument();
    expect(screen.queryByRole('table', { name: 'Leave balances' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Apply for leave' })).toHaveAttribute(
      'href',
      '/app/hr/leave-applications',
    );
  });
});
