import { createRuntime } from '@mysms/core';
import {
  countries,
  fakeApi,
  fakeBackend,
  operations,
  platformAdmin,
  problem,
  TENANT_CODE,
  type Handler,
} from '@mysms/fixtures';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WebApp } from '@mysms/web-kit';
import { createMemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { routes } from './routes';

const BASE = 'https://console.example.com';

async function signedIn(
  path: string,
  opts: { me?: typeof platformAdmin; overrides?: Record<string, Handler> } = {},
) {
  const api = fakeApi({ ...fakeBackend({ me: opts.me ?? platformAdmin }), ...opts.overrides });
  const runtime = createRuntime({ baseUrl: BASE, clientId: 'console/test/jsdom', fetch: api.fetch });
  const router = createMemoryRouter(routes, {
    initialEntries: [`/app/auth/login?next=${encodeURIComponent(path)}`],
  });
  render(<WebApp runtime={runtime} router={router} />);
  await userEvent.type(await screen.findByLabelText('School code'), TENANT_CODE);
  await userEvent.type(
    screen.getByLabelText('Username, e-mail or mobile'),
    platformAdmin.user?.username ?? '',
  );
  await userEvent.type(screen.getByLabelText('Password'), 'password');
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
  await waitFor(() => expect(router.state.location.pathname).toBe(path.split('?')[0]));
  // wait for the committed shell, not just the router state, before any synchronous assertion
  await screen.findByRole('navigation', { name: 'Main navigation' });
  return { api, router };
}

const india = countries[0]!;

describe('platform console shell', () => {
  it('shows the platform band and placeholders for screens not built yet', async () => {
    const { router } = await signedIn('/app/master/states');
    expect(screen.getByText('Platform console')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'PLT-07 · Master data' })).toBeInTheDocument();
    await router.navigate('/app/platform/tenants');
    expect(await screen.findByRole('heading', { name: 'PLT-01 · Tenants' })).toBeInTheDocument();
  });
});

describe('PLT-07 countries', () => {
  it('lists countries with translated status labels', async () => {
    await signedIn('/app/master/countries');
    const table = await screen.findByRole('table', { name: '4 items' });
    expect(within(table).getByText('India')).toBeInTheDocument();
    expect(within(table).getAllByText('Active')).toHaveLength(3);
    expect(within(table).getByText('Inactive')).toBeInTheDocument();
  });

  it('edits under If-Match, sending only the changed field', async () => {
    const { api } = await signedIn('/app/master/countries');
    await userEvent.click(await screen.findByRole('row', { name: /India/ }));
    const name = await screen.findByLabelText('Name');
    await waitFor(() => expect(name).toHaveValue('India'));
    await userEvent.clear(name);
    await userEvent.type(name, 'Bharat');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Bharat saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(JSON.parse(patch?.body ?? '{}')).toEqual({ name: 'Bharat' });
    expect(await screen.findByRole('cell', { name: 'Bharat' })).toBeInTheDocument();
  });

  it('turns a stale version into the conflict flow and saves after reloading', async () => {
    const { api } = await signedIn('/app/master/countries');
    await userEvent.click(await screen.findByRole('row', { name: /India/ }));
    const name = await screen.findByLabelText('Name');
    await waitFor(() => expect(name).toHaveValue('India'));
    // someone else edits the row in the meantime
    await api.fetch(
      new Request(`${BASE}/v1/master/countries/${india.id}`, {
        method: 'PATCH',
        headers: { Authorization: 'Bearer other', 'If-Match': '"1"', 'Content-Type': 'application/json' },
        body: JSON.stringify({ shortName: 'IN' }),
      }),
    );
    await userEvent.clear(name);
    await userEvent.type(name, 'Bharat');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Someone else changed this')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Reload latest' }));
    await waitFor(() => expect(screen.getByLabelText('Short name')).toHaveValue('IN'));
    await userEvent.clear(screen.getByLabelText('Name'));
    await userEvent.type(screen.getByLabelText('Name'), 'Bharat');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Bharat saved.')).toBeInTheDocument();
    expect(api.calls.filter((c) => c.method === 'PATCH').map((c) => c.headers.get('If-Match'))).toEqual([
      '"1"',
      '"1"',
      '"2"',
    ]);
  });

  it('validates on the client before calling the API', async () => {
    const { api } = await signedIn('/app/master/countries');
    await userEvent.click(await screen.findByRole('row', { name: /India/ }));
    const name = await screen.findByLabelText('Name');
    await waitFor(() => expect(name).toHaveValue('India'));
    await userEvent.clear(name);
    await userEvent.clear(screen.getByLabelText('Phone prefix'));
    await userEvent.type(screen.getByLabelText('Phone prefix'), '91');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('This field is required.')).toBeInTheDocument();
    expect(screen.getByText('Use the format shown.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.method === 'PATCH')).toBe(false);
  });

  it('creates a country and maps server field errors onto the inputs', async () => {
    let attempts = 0;
    await signedIn('/app/master/countries', {
      overrides: {
        'POST /v1/master/countries': () =>
          ++attempts === 1
            ? problem(422, 'VALIDATION_FAILED', 'Check the fields', {
                errors: [{ field: 'name', message: 'A country with this name exists.' }],
              })
            : new Response(JSON.stringify({ id: 'new', iso2: 'BT', name: 'Bhutan' }), {
                status: 201,
                headers: { 'Content-Type': 'application/json', ETag: '"1"' },
              }),
      },
    });
    await userEvent.click(await screen.findByRole('button', { name: 'New country' }));
    const dialog = await screen.findByRole('dialog', { name: 'New country' });
    await userEvent.type(within(dialog).getByLabelText('ISO 2'), 'bt');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await within(dialog).findByText('Use the format shown.')).toBeInTheDocument();
    await userEvent.clear(within(dialog).getByLabelText('ISO 2'));
    await userEvent.type(within(dialog).getByLabelText('ISO 2'), 'BT');
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Bhutan');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await within(dialog).findByText('A country with this name exists.')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Bhutan saved.')).toBeInTheDocument();
  });

  it('deletes after confirmation, and hides delete without the permission', async () => {
    await signedIn('/app/master/countries');
    await userEvent.click(await screen.findByRole('row', { name: /Nepal/ }));
    await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Nepal'));
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
    expect(screen.getByText('Delete Nepal? It will no longer appear in pick-lists.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
    expect(await screen.findByText('Nepal deleted.')).toBeInTheDocument();
    expect(await screen.findByRole('table', { name: '3 items' })).toBeInTheDocument();
  });

  it('offers no delete or create to an operator without those permissions', async () => {
    const readOnly = { ...platformAdmin, permissions: ['master.country.read', 'master.country.update'] };
    await signedIn('/app/master/countries', { me: readOnly });
    // absence is only meaningful once the screen has rendered
    await screen.findByRole('table', { name: '4 items' });
    expect(screen.queryByRole('button', { name: 'New country' })).not.toBeInTheDocument();
    await userEvent.click(await screen.findByRole('row', { name: /India/ }));
    await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('India'));
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
  });
});

describe('PLT-06 job centre', () => {
  it('lists jobs from every module and cancels at the owning module', async () => {
    const { api } = await signedIn('/app/platform/operations');
    const table = await screen.findByRole('table', { name: '3 items' });
    expect(within(table).getByText('Running')).toBeInTheDocument();
    expect(within(table).getByText('Done')).toBeInTheDocument();
    expect(within(table).getByText('Failed')).toBeInTheDocument();
    expect(screen.getByText(/can lag a few seconds/)).toBeInTheDocument();
    // progress shows for running and finished-successfully jobs only
    expect(within(table).getAllByRole('progressbar')).toHaveLength(2);
    const cancelButtons = within(table).getAllByRole('button', { name: 'Cancel job' });
    expect(cancelButtons).toHaveLength(1);
    await userEvent.click(cancelButtons[0]!);
    expect(
      await screen.findByText('Cancellation requested for finance.GenerateFeeDues.'),
    ).toBeInTheDocument();
    expect(
      api.calls.some(
        (c) =>
          c.method === 'POST' && c.path === `/v1/finance/operations/${operations[0]?.operationId}:cancel`,
      ),
    ).toBe(true);
  });

  it('hides cancel when the user may not cancel that module’s jobs', async () => {
    await signedIn('/app/platform/operations', {
      me: { ...platformAdmin, permissions: ['platform.operation.read'] },
    });
    const table = await screen.findByRole('table', { name: '3 items' });
    expect(within(table).queryByRole('button', { name: 'Cancel job' })).not.toBeInTheDocument();
  });
});
