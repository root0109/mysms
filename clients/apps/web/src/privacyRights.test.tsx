import { fakeRightsRequests, rightsFixture, rightsOfficer, rightsRequester } from '@mysms/fixtures';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

describe('ACC-15 My privacy requests', () => {
  it("sends a complaint with its category and shows the school's answer to an earlier one", async () => {
    const { api } = await signedIn('/app/privacy/my/requests', rightsRequester, fakeRightsRequests());
    expect(await screen.findByRole('heading', { name: 'My privacy requests' })).toBeInTheDocument();
    expect(await screen.findByText('PRV-2026-000002')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText('What do you want to do?'), 'grievance');
    await userEvent.selectOptions(screen.getByLabelText('What is your complaint about?'), 'consent');
    await userEvent.type(
      screen.getByLabelText('Tell the school what you are asking for'),
      'Photos of me are shared without asking.',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Send request' }));
    expect(await screen.findByText(/Request PRV-2026-000007 is sent/)).toBeInTheDocument();
    const sent = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/privacy/my/requests');
    expect(JSON.parse(sent?.body ?? '{}')).toEqual({
      type: 'grievance',
      subjectPersonId: rightsFixture.raviPersonId,
      description: 'Photos of me are shared without asking.',
      grievanceCategory: 'consent',
    });
    expect(sent?.headers.get('Idempotency-Key')).toBeTruthy();

    await userEvent.click(screen.getByText('PRV-2026-000002'));
    const dialog = await screen.findByRole('dialog', { name: 'Request PRV-2026-000002' });
    expect(within(dialog).getByText(rightsFixture.grievanceAnswer)).toBeInTheDocument();
    expect(within(dialog).queryByText('Spoke to the class teacher.')).not.toBeInTheDocument();
  });

  it('asks a correction for the record and what should change before sending it', async () => {
    const { api } = await signedIn('/app/privacy/my/requests', rightsRequester, fakeRightsRequests());
    await screen.findByRole('heading', { name: 'My privacy requests' });
    await userEvent.selectOptions(screen.getByLabelText('What do you want to do?'), 'correction');
    await userEvent.type(
      screen.getByLabelText('Tell the school what you are asking for'),
      'My surname is wrong.',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Send request' }));
    expect(await screen.findByText('Say which record and what should change.')).toBeInTheDocument();

    await userEvent.type(
      screen.getByLabelText('Which record (for example: student details)'),
      'student details',
    );
    await userEvent.type(screen.getByLabelText('What is wrong (for example: date of birth)'), 'surname');
    await userEvent.type(screen.getByLabelText('What it should say'), 'Kulkarni');
    await userEvent.click(screen.getByRole('button', { name: 'Send request' }));
    await screen.findByText(/is sent/);
    const sent = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/privacy/my/requests');
    expect(JSON.parse(sent?.body ?? '{}')).toMatchObject({
      type: 'correction',
      correction: {
        module: 'people',
        recordType: 'student details',
        fields: [{ field: 'surname', requestedValue: 'Kulkarni' }],
      },
    });
  });

  it('names a nominee, shown masked, and removes them after a confirmation', async () => {
    const { api } = await signedIn('/app/privacy/my/requests', rightsRequester, fakeRightsRequests());
    expect(await screen.findByText('You have not named a nominee.')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("Nominee's full name"), 'Meera Krishnan');
    await userEvent.type(screen.getByLabelText("Nominee's phone or e-mail"), '+91 98450 12345');
    await userEvent.click(screen.getByRole('button', { name: 'Save nominee' }));
    expect(await screen.findByText('Your nominee: ••••hnan · ••••2345')).toBeInTheDocument();
    expect(screen.queryByText(/Meera Krishnan/)).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Remove nominee' }));
    const confirm = await screen.findByRole('dialog', { name: 'Remove your nominee?' });
    await userEvent.click(within(confirm).getByRole('button', { name: 'Remove nominee' }));
    expect(await screen.findByText('Your nominee is removed.')).toBeInTheDocument();
    const revoked = api.calls.find((c) => c.path === '/v1/privacy/my/nominee:revoke');
    expect(JSON.parse(revoked?.body ?? '{}')).toEqual({ etag: '"1"' });
  });
});

describe('ADM-19 Privacy requests', () => {
  it('lists the open requests nearest the deadline first and answers an access request with its summary', async () => {
    const { api } = await signedIn('/app/privacy/rights-requests', rightsOfficer, fakeRightsRequests());
    expect(await screen.findByRole('heading', { name: 'Privacy requests' })).toBeInTheDocument();
    const rows = await screen.findAllByRole('row');
    // header, then the correction due tomorrow before the access request due in ten days; the closed one is hidden
    expect(rows[1]).toHaveTextContent('PRV-2026-000003');
    expect(rows[1]).toHaveTextContent('Due soon');
    expect(rows[2]).toHaveTextContent('PRV-2026-000001');
    expect(screen.queryByText('PRV-2026-000002')).not.toBeInTheDocument();

    await userEvent.click(screen.getByText('PRV-2026-000001'));
    const dialog = await screen.findByRole('dialog', { name: 'Request PRV-2026-000001' });
    expect(await within(dialog).findByText('Preview of the summary that will be sent')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Send the summary and close' }));
    expect(await within(dialog).findByText('Saved.')).toBeInTheDocument();
    const fulfil = api.calls.find((c) => c.path.endsWith(':fulfilAccess'));
    expect(JSON.parse(fulfil?.body ?? '{}')).toEqual({ etag: '"2"' });
  });

  it('declines a correction only with an answer for the requester', async () => {
    const { api } = await signedIn('/app/privacy/rights-requests', rightsOfficer, fakeRightsRequests());
    await userEvent.click(await screen.findByText('PRV-2026-000003'));
    let dialog = await screen.findByRole('dialog', { name: 'Request PRV-2026-000003' });
    expect(within(dialog).getByText('date of birth: 2012-04-09')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Assign' }));
    await userEvent.type(
      within(dialog).getByLabelText('Staff member (user id)'),
      rightsFixture.officerUserId,
    );
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }));
    await waitFor(() => expect(api.calls.some((c) => c.path.endsWith(':assign'))).toBe(true));

    dialog = await screen.findByRole('dialog', { name: 'Request PRV-2026-000003' });
    await userEvent.click(await within(dialog).findByRole('button', { name: 'Close the request' }));
    await userEvent.selectOptions(within(dialog).getByLabelText('Outcome'), 'declined');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }));
    expect(await within(dialog).findByText('Write the answer the requester will see.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.path.endsWith(':close'))).toBe(false);

    await userEvent.type(
      within(dialog).getByLabelText('Answer to the requester'),
      'The register shows 2012-04-08.',
    );
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }));
    await waitFor(() => expect(api.calls.some((c) => c.path.endsWith(':close'))).toBe(true));
    const closed = api.calls.find((c) => c.path.endsWith(':close'));
    expect(JSON.parse(closed?.body ?? '{}')).toMatchObject({
      outcomeCode: 'declined',
      response: 'The register shows 2012-04-08.',
    });
  });

  it('starts an erasure after a confirmation and lists the work asked of each module', async () => {
    const { api } = await signedIn('/app/privacy/rights-requests', rightsOfficer, fakeRightsRequests());
    await userEvent.click(await screen.findByText('PRV-2026-000004'));
    const dialog = await screen.findByRole('dialog', { name: 'Request PRV-2026-000004' });
    expect(within(dialog).queryByRole('heading', { name: 'Work in each module' })).not.toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Start fulfilment' }));
    expect(
      within(dialog).getByText(/Each module that holds personal data will be asked/),
    ).toBeInTheDocument();
    expect(api.calls.some((c) => c.path.endsWith(':startFulfilment'))).toBe(false);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }));
    expect(await within(dialog).findByRole('heading', { name: 'Work in each module' })).toBeInTheDocument();
    const started = api.calls.find((c) => c.path.endsWith(':startFulfilment'));
    expect(JSON.parse(started?.body ?? '{}')).toEqual({ etag: '"2"' });
    const tasks = within(dialog).getByRole('table', { name: 'Work in each module' });
    expect(within(tasks).getAllByText('Sent to the module')).toHaveLength(1);
    expect(within(tasks).getAllByText('Waiting for its turn')).toHaveLength(2);
    expect(within(dialog).queryByRole('button', { name: 'Start fulfilment' })).not.toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: 'Close the request' })).not.toBeInTheDocument();
  });

  it("retries an export's failed task and puts the request back in progress", async () => {
    const { api } = await signedIn('/app/privacy/rights-requests', rightsOfficer, fakeRightsRequests());
    await userEvent.click(await screen.findByText('PRV-2026-000006'));
    const dialog = await screen.findByRole('dialog', { name: 'Request PRV-2026-000006' });
    const tasks = within(dialog).getByRole('table', { name: 'Work in each module' });
    expect(within(tasks).getByText('STORAGE_DOWN')).toBeInTheDocument();
    expect(within(tasks).queryByRole('button', { name: 'Retry iam' })).not.toBeInTheDocument();
    await userEvent.click(within(tasks).getByRole('button', { name: 'Retry sis' }));
    expect(await within(dialog).findByText('Saved.')).toBeInTheDocument();
    const retried = api.calls.find((c) => c.path.endsWith(':retry'));
    expect(retried?.path).toBe(
      `/v1/privacy/rights-requests/${rightsFixture.exportId}/tasks/${rightsFixture.failedTaskId}:retry`,
    );
    expect(JSON.parse(retried?.body ?? '{}')).toEqual({ etag: '"2"' });
    expect(await within(dialog).findByText('6')).toBeInTheDocument();
  });

  it('withdraws every consent of a withdraw-all request in one confirmed step', async () => {
    const { api } = await signedIn('/app/privacy/rights-requests', rightsOfficer, fakeRightsRequests());
    await userEvent.click(await screen.findByText('PRV-2026-000005'));
    const dialog = await screen.findByRole('dialog', { name: 'Request PRV-2026-000005' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Withdraw all consents and close' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirm' }));
    expect(await within(dialog).findByText('Saved.')).toBeInTheDocument();
    const fulfilled = api.calls.find((c) => c.path.endsWith(':fulfilWithdrawAll'));
    expect(JSON.parse(fulfilled?.body ?? '{}')).toEqual({ etag: '"2"' });
  });
});
