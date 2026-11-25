import { ORG_ADMIN, SELF_SERVICE, fakeOrg, orgFixture, schoolAdmin } from '@mysms/fixtures';
import { localDay } from '@mysms/core';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const admin = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...ORG_ADMIN] };
const today = localDay(new Date(), 'Asia/Kolkata');

describe('ADM-01 Admin dashboard', () => {
  it('counts the work waiting for the school, each count opening its screen', async () => {
    const { api } = await signedIn('/app/org/overview', admin, fakeOrg(today));
    expect(await screen.findByRole('heading', { name: 'Admin dashboard' })).toBeInTheDocument();
    const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;
    await waitFor(() =>
      expect(within(card('Admission applications to process')).getByText('2')).toBeInTheDocument(),
    );
    expect(within(card('Leave applications to decide')).getByText('1')).toBeInTheDocument();
    await waitFor(() =>
      expect(within(card('Attendance sheets not complete today')).getByText('1')).toBeInTheDocument(),
    );
    expect(within(card('Open feedback threads')).getByText('0')).toBeInTheDocument();
    // a count the role may not list is not shown
    expect(screen.queryByText('Upcoming events')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open: Leave applications to decide' })).toHaveAttribute(
      'href',
      '/app/hr/leave-applications',
    );
    const sheets = api.calls.find((c) => c.path === '/v1/attendance/attendance-sheets');
    expect(new URLSearchParams(sheets?.search).get('filter')).toBe(
      `attendanceDate = "${today}" AND (status = "draft" OR status = "in_progress")`,
    );
  });

  it('tells a role with none of the counts so', async () => {
    await signedIn('/app/org/overview', { ...schoolAdmin, permissions: [...SELF_SERVICE] }, fakeOrg());
    expect(await screen.findByText('Your role has no counts to show here.')).toBeInTheDocument();
  });
});

describe('ADM-02 Institutions', () => {
  it('shows the institutions as a tree by parent, each opening its detail', async () => {
    await signedIn('/app/org/institutions', admin, fakeOrg());
    const tree = await screen.findByRole('heading', { name: 'Institutions by parent' });
    const card = tree.closest('section') as HTMLElement;
    const trust = await within(card).findByRole('link', { name: 'Green Valley Trust' });
    const branch = trust.closest('li') as HTMLElement;
    // the annexe and the school sit under the trust, in name order; the college's parent is out of scope
    expect(
      within(branch)
        .getAllByRole('link')
        .map((a) => a.textContent),
    ).toEqual(['Green Valley Trust', 'Green Valley Annexe', 'Green Valley School']);
    expect(within(branch).getByText(/inactive/)).toBeInTheDocument();
    const college = within(card).getByRole('link', { name: 'Green Valley College' });
    expect(branch).not.toContainElement(college);
    expect(college).toHaveAttribute('href', `/app/org/institutions/${orgFixture.collegeId}`);
  });
});

describe('ADM-03 Institution detail & attributes', () => {
  it('fills in the attributes the type asks for and lists the institution’s departments', async () => {
    const { api } = await signedIn(`/app/org/institutions/${orgFixture.schoolId}`, admin, fakeOrg());
    expect(await screen.findByRole('heading', { name: 'Green Valley School' })).toBeInTheDocument();
    expect(screen.getByText('GVS')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('radio', { name: 'Attributes' }));
    const beds = await screen.findByLabelText('Hostel beds (required)');
    expect(screen.getByLabelText('Board')).toHaveValue('CBSE');
    await userEvent.type(beds, '120');
    await userEvent.click(screen.getByRole('button', { name: 'Save Hostel beds' }));
    await waitFor(() =>
      expect(
        api.calls.some(
          (c) => c.method === 'POST' && c.path === `/v1/org/institutions/${orgFixture.schoolId}/attributes`,
        ),
      ).toBe(true),
    );
    const created = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/attributes'));
    expect(JSON.parse(created?.body ?? '{}')).toMatchObject({
      attrKey: 'Hostel beds',
      attrValue: '120',
      attributeDefinitionId: orgFixture.hostelDefinitionId,
    });
    // a required attribute cannot be emptied
    await waitFor(() => expect(screen.getByLabelText('Hostel beds (required)')).toHaveValue(120));
    await userEvent.clear(screen.getByLabelText('Hostel beds (required)'));
    await userEvent.click(screen.getByRole('button', { name: 'Save Hostel beds' }));
    expect(await screen.findByText('This field is required.')).toBeInTheDocument();
    // a change to an existing value is an update under its etag
    const board = screen.getByLabelText('Board');
    await userEvent.clear(board);
    await userEvent.type(board, 'ICSE');
    await userEvent.click(screen.getByRole('button', { name: 'Save Board' }));
    await waitFor(() => expect(api.calls.some((c) => c.method === 'PATCH')).toBe(true));
    expect(api.calls.find((c) => c.method === 'PATCH')?.headers.get('If-Match')).toBe('"1"');

    await userEvent.click(screen.getByRole('radio', { name: 'Departments' }));
    expect(await screen.findByText('Science')).toBeInTheDocument();
    expect(screen.queryByText('Arts')).not.toBeInTheDocument();
  });

  it('says a record is not available for a malformed or unknown id', async () => {
    await signedIn('/app/org/institutions/not-an-id', admin, fakeOrg());
    expect(await screen.findByText(/This item is not available/)).toBeInTheDocument();
  });
});

describe('ADM-05 Academic years & terms', () => {
  it('makes a planned year current under its etag and generates its terms', async () => {
    const { api } = await signedIn('/app/org/academic-years', admin, fakeOrg());
    await screen.findByRole('option', { name: /^2026-27/ });
    await userEvent.selectOptions(
      screen.getByRole('combobox', { name: 'Academic year' }),
      orgFixture.plannedYearId,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Make current year' }));
    expect(await screen.findByText('The year is now the current year.')).toBeInTheDocument();
    const activate = api.calls.find((c) => c.path.endsWith(':activate'));
    expect(JSON.parse(activate?.body ?? '{}')).toEqual({ etag: '"1"' });
    await userEvent.click(screen.getByRole('button', { name: 'Generate terms' }));
    expect(await screen.findByText('Semester 2')).toBeInTheDocument();
    // a year that is not planned cannot be made current
    await userEvent.selectOptions(
      screen.getByRole('combobox', { name: 'Academic year' }),
      orgFixture.closedYearId,
    );
    expect(await screen.findByText('Whole year')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Make current year' })).not.toBeInTheDocument();
  });
});

describe('ADM-10 Roles & permissions', () => {
  it('grants and takes away a role’s permissions, keeping system roles and fixed permissions read-only', async () => {
    const { api } = await signedIn(`/app/iam/roles?role=${orgFixture.teacherRoleId}`, admin, fakeOrg());
    const read = await screen.findByRole('checkbox', { name: /Read students/ });
    expect(read).toBeChecked();
    expect(screen.getByRole('checkbox', { name: /Read staff/ })).toBeDisabled();
    await userEvent.click(screen.getByRole('checkbox', { name: /Change students/ }));
    await waitFor(() => expect(screen.getByRole('checkbox', { name: /Change students/ })).toBeChecked());
    expect(
      JSON.parse(api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/permissions'))?.body ?? '{}'),
    ).toMatchObject({ permissionId: orgFixture.permissions[1].id });
    await userEvent.click(read);
    await waitFor(() => expect(screen.getByRole('checkbox', { name: /Read students/ })).not.toBeChecked());
    expect(api.calls.find((c) => c.method === 'DELETE')?.headers.get('If-Match')).toBe('"1"');
    await userEvent.type(screen.getByRole('searchbox', { name: 'Find a permission' }), 'staff');
    expect(screen.queryByRole('checkbox', { name: /Read students/ })).not.toBeInTheDocument();

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Role' }), orgFixture.systemRoleId);
    expect(await screen.findByText(/This is a system role/)).toBeInTheDocument();
  });
});
