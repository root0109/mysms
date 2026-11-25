import { fakePrivacyGovernance, governanceFixture, privacyOfficer, SELF_SERVICE } from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;

describe('ADM-20 Breach register', () => {
  it("shows the Board's clock, filters the overdue ones and moves a reported breach through its steps", async () => {
    const { api } = await signedIn('/app/privacy/breaches', privacyOfficer, fakePrivacyGovernance());
    expect(await screen.findByRole('heading', { name: 'Breach register' })).toBeInTheDocument();
    const rows = await screen.findAllByRole('row');
    // the Board's nearest deadline first: the contained breach past it, then the one reported 60 hours ago
    expect(rows[1]).toHaveTextContent('PRB-2026-000002');
    expect(rows[1]).toHaveTextContent(/Overdue by \d+ h/);
    expect(rows[2]).toHaveTextContent('PRB-2026-000001');
    expect(rows[2]).toHaveTextContent(/1[01] h left/);

    await userEvent.click(screen.getByRole('radio', { name: 'Overdue' }));
    await waitFor(() => expect(screen.queryByText('PRB-2026-000001')).not.toBeInTheDocument());
    expect(screen.getByText('PRB-2026-000002')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('radio', { name: 'Open' }));

    await userEvent.click(await screen.findByText('PRB-2026-000001'));
    const dialog = await screen.findByRole('dialog', { name: 'Breach PRB-2026-000001' });
    expect(within(dialog).queryByRole('button', { name: 'Close the breach' })).not.toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Triage' }));
    expect(await within(dialog).findByText('Saved.')).toBeInTheDocument();
    const triage = api.calls.find((c) => c.path.endsWith(':triage'));
    expect(JSON.parse(triage?.body ?? '{}')).toEqual({ etag: '"1"' });
    expect(triage?.headers.get('Idempotency-Key')).toBeTruthy();

    await userEvent.click(await within(dialog).findByRole('button', { name: 'Not a breach' }));
    const confirm = within(dialog).getByRole('button', { name: 'Confirm' });
    expect(confirm).toBeDisabled();
    await userEvent.type(within(dialog).getByLabelText('Why this is not a breach'), 'Sent to staff only.');
    await userEvent.click(confirm);
    await waitFor(() => expect(api.calls.some((c) => c.path.endsWith(':markNotABreach'))).toBe(true));
    const marked = api.calls.find((c) => c.path.endsWith(':markNotABreach'));
    expect(JSON.parse(marked?.body ?? '{}')).toEqual({ reason: 'Sent to staff only.', etag: '"2"' });
  });

  it('reports a breach, refusing an awareness time before its detection', async () => {
    const { api } = await signedIn('/app/privacy/breaches', privacyOfficer, fakePrivacyGovernance());
    await screen.findByRole('heading', { name: 'Breach register' });
    const report = card('Report a breach');
    await userEvent.type(
      within(report).getByLabelText('What happened'),
      'A printed class list was left on the bus.',
    );
    fireEvent.change(within(report).getByLabelText('When it was detected'), {
      target: { value: '2026-11-20T10:00' },
    });
    fireEvent.change(within(report).getByLabelText('When the school became aware'), {
      target: { value: '2026-11-20T09:00' },
    });
    await userEvent.click(within(report).getByRole('button', { name: 'Report a breach' }));
    expect(
      await within(report).findByText('must not be earlier than when the breach was detected'),
    ).toBeInTheDocument();

    fireEvent.change(within(report).getByLabelText('When the school became aware'), {
      target: { value: '2026-11-20T11:00' },
    });
    await userEvent.type(within(report).getByLabelText('Modules (codes)'), 'sis, transport');
    await userEvent.click(within(report).getByRole('button', { name: 'Report a breach' }));
    expect(await within(report).findByText(/Breach PRB-2026-000003 is reported/)).toBeInTheDocument();
    const posts = api.calls.filter((c) => c.method === 'POST' && c.path === '/v1/privacy/breaches');
    expect(posts).toHaveLength(2);
    // the retried report keeps its Idempotency-Key, so it is never filed twice
    expect(posts[1]?.headers.get('Idempotency-Key')).toBe(posts[0]?.headers.get('Idempotency-Key'));
    expect(JSON.parse(posts[1]?.body ?? '{}')).toMatchObject({
      description: 'A printed class list was left on the bus.',
      modules: ['sis', 'transport'],
    });
    expect(await screen.findByText('PRB-2026-000003')).toBeInTheDocument();
  });

  it('shows a reader the register without the report form or any step', async () => {
    const reader = { ...privacyOfficer, permissions: [...SELF_SERVICE, 'privacy.breach.read'] };
    await signedIn('/app/privacy/breaches', reader, fakePrivacyGovernance());
    await userEvent.click(await screen.findByText('PRB-2026-000001'));
    const dialog = await screen.findByRole('dialog', { name: 'Breach PRB-2026-000001' });
    expect(within(dialog).queryByRole('button', { name: 'Triage' })).not.toBeInTheDocument();
    expect(within(dialog).queryByRole('heading', { name: 'What is known' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Report a breach' })).not.toBeInTheDocument();
  });
});

describe('ADM-21 Legal holds', () => {
  it('places a hold on named records with only the fields of that scope', async () => {
    const { api } = await signedIn('/app/privacy/legal-holds', privacyOfficer, fakePrivacyGovernance());
    expect(await screen.findByRole('heading', { name: 'Legal holds' })).toBeInTheDocument();
    expect(await screen.findByText(/Review due/)).toBeInTheDocument();
    expect(screen.queryByText('Data category finance')).not.toBeInTheDocument();

    const place = card('Place a hold');
    await userEvent.selectOptions(within(place).getByLabelText('What to keep'), 'records');
    expect(within(place).queryByLabelText('Person (id)')).not.toBeInTheDocument();
    await userEvent.type(within(place).getByLabelText('Module (code)'), 'sis');
    await userEvent.type(within(place).getByLabelText('Record type'), 'student');
    await userEvent.type(within(place).getByLabelText('Record ids'), 's1, s2');
    fireEvent.change(within(place).getByLabelText('Review by'), { target: { value: '2030-01-31' } });
    await userEvent.click(within(place).getByRole('button', { name: 'Place a hold' }));
    expect(await within(place).findByText('The hold is placed.')).toBeInTheDocument();
    const placed = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/privacy/legal-holds');
    expect(JSON.parse(placed?.body ?? '{}')).toEqual({
      reasonCode: 'litigation',
      scope: 'records',
      reviewBy: '2030-01-31',
      module: 'sis',
      entityType: 'student',
      entityIds: ['s1', 's2'],
    });
    expect(await screen.findByText('2 student records in sis')).toBeInTheDocument();
  });

  it('moves a review date under If-Match and releases the hold after a confirmation', async () => {
    const { api } = await signedIn('/app/privacy/legal-holds', privacyOfficer, fakePrivacyGovernance());
    await userEvent.click(await screen.findByText(`One person's data (${governanceFixture.heldPersonId})`));
    const dialog = await screen.findByRole('dialog', { name: 'Hold: Litigation' });
    fireEvent.change(within(dialog).getByLabelText('Review by'), { target: { value: '2030-06-30' } });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Move the review date' }));
    expect(await within(dialog).findByText('Saved.')).toBeInTheDocument();
    const moved = api.calls.find((c) => c.method === 'PATCH');
    expect(moved?.headers.get('If-Match')).toBe('"1"');
    expect(moved?.search).toContain('updateMask=reviewBy');
    expect(JSON.parse(moved?.body ?? '{}')).toEqual({ reviewBy: '2030-06-30' });

    await userEvent.click(within(dialog).getByRole('button', { name: 'Release the hold' }));
    expect(within(dialog).getByText(/This cannot be undone/)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Release the hold' }));
    await waitFor(() => expect(api.calls.some((c) => c.path.endsWith(':release'))).toBe(true));
    const released = api.calls.find((c) => c.path.endsWith(':release'));
    expect(JSON.parse(released?.body ?? '{}')).toEqual({ etag: '"2"' });
    expect(await within(dialog).findByText(/released/)).toBeInTheDocument();
  });
});

describe('ADM-22 Retention', () => {
  it('refuses a period below the legal floor and saves the school setting under If-Match', async () => {
    const { api } = await signedIn('/app/privacy/retention', privacyOfficer, fakePrivacyGovernance());
    expect(await screen.findByRole('heading', { name: 'Retention' })).toBeInTheDocument();
    const policy = card('unsuccessful_applications');
    const period = within(policy).getByLabelText('Keep for (days)');
    await userEvent.clear(period);
    await userEvent.type(period, '100');
    await userEvent.click(within(policy).getByRole('button', { name: 'Save' }));
    expect(within(policy).getByText('Keep it for at least 180 days.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.method === 'PATCH')).toBe(false);

    await userEvent.clear(period);
    await userEvent.type(period, '400');
    await userEvent.selectOptions(within(policy).getByLabelText('Then'), 'archive');
    await userEvent.click(within(policy).getByRole('button', { name: 'Save' }));
    expect(await within(policy).findByText('The setting is saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"0"');
    expect(JSON.parse(patch?.body ?? '{}')).toEqual({ periodDays: 400, action: 'archive' });
    expect(within(card('unsuccessful_applications')).getByText('School setting')).toBeInTheDocument();
    expect(screen.getByText('12 handled · 1 kept on hold')).toBeInTheDocument();
  });
});
