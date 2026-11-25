import { createRuntime } from '@mysms/core';
import { fakeApi, fakeBackend, json, problem, schoolAdmin, TENANT_CODE, type Handler } from '@mysms/fixtures';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WebApp } from '@mysms/web-kit';
import { createMemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { routes } from './routes';

function renderApp(path: string, overrides: Record<string, Handler> = {}) {
  const api = fakeApi({ ...fakeBackend({ me: schoolAdmin }), ...overrides });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'web/test/jsdom',
    fetch: api.fetch,
  });
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(<WebApp runtime={runtime} router={router} />);
  return { api, router, runtime };
}

async function fillAndSubmit(identifier = schoolAdmin.user?.username ?? '', password = 'password') {
  await userEvent.type(await screen.findByLabelText('School code'), TENANT_CODE);
  await userEvent.type(screen.getByLabelText('Username, e-mail or mobile'), identifier);
  await userEvent.type(screen.getByLabelText('Password'), password);
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
}

describe('web app', () => {
  it('sends a signed-out deep link to sign-in and returns to it afterwards', async () => {
    const { router } = renderApp('/app/org/institutions');
    await waitFor(() => expect(router.state.location.pathname).toBe('/app/auth/login'));
    expect(router.state.location.search).toBe('?next=%2Fapp%2Forg%2Finstitutions');
    await fillAndSubmit();
    await waitFor(() => expect(router.state.location.pathname).toBe('/app/org/institutions'));
    // ADM-02 is built: the deep link lands on its own screen
    expect(await screen.findByRole('heading', { name: 'Institutions' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument();
    expect(screen.getByText('admin.ravi')).toBeInTheDocument();
  });

  it('validates before calling the API', async () => {
    const { api } = renderApp('/app/auth/login');
    await userEvent.click(await screen.findByRole('button', { name: 'Sign in' }));
    expect(await screen.findAllByText('This field is required.')).toHaveLength(3);
    expect(api.calls).toHaveLength(0);
  });

  it('shows the mismatch message on bad credentials', async () => {
    renderApp('/app/auth/login');
    await fillAndSubmit('admin.ravi', 'wrong');
    expect(await screen.findByRole('alert')).toHaveTextContent('Those details did not match');
  });

  it('shows a generic error with the trace reference for server failures', async () => {
    renderApp('/app/auth/login', {
      'POST /v1/iam/auth:login': () => problem(500, 'INTERNAL', 'Boom', { traceId: 'trace-42' }),
    });
    await fillAndSubmit();
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Something went wrong');
    expect(alert).toHaveTextContent('Reference: trace-42');
  });

  it('completes the MFA step', async () => {
    const { router } = renderApp('/app/auth/login', {
      'POST /v1/iam/auth:login': () => json({ mfaRequired: true, mfaToken: 'mfa-1' }),
      'POST /v1/iam/mfa:complete': async (req) => {
        const body = (await req.json()) as { code: string };
        return body.code === '123456'
          ? json({
              tokens: { accessToken: 'a', refreshToken: 'r', expiresInSeconds: 900, tokenType: 'Bearer' },
              user: schoolAdmin.user,
            })
          : problem(400, 'INVALID_CODE', 'Invalid code');
      },
    });
    await fillAndSubmit();
    const code = await screen.findByLabelText('Code from your authenticator app');
    await userEvent.type(code, '12');
    await userEvent.click(screen.getByRole('button', { name: 'Verify' }));
    expect(await screen.findByText('Use the format shown.')).toBeInTheDocument();
    await userEvent.clear(code);
    await userEvent.type(code, '123456');
    await userEvent.click(screen.getByRole('button', { name: 'Verify' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/app'));
    expect(await screen.findByRole('heading', { name: 'Welcome, admin.ravi' })).toBeInTheDocument();
  });

  it('signs out back to the sign-in screen', async () => {
    const { router } = renderApp('/app/auth/login');
    await fillAndSubmit();
    await userEvent.click(await screen.findByRole('button', { name: 'Sign out' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/app/auth/login'));
  });

  it('renders a polite dead end for unknown paths, inside and outside the app', async () => {
    const { router } = renderApp('/nowhere');
    expect(await screen.findByText(/This item is not available/)).toBeInTheDocument();
    await fillAndSubmit().catch(() => undefined);
    await router.navigate('/app/auth/login');
    await fillAndSubmit();
    await router.navigate('/app/definitely/not/a/screen');
    expect(await screen.findByText(/This item is not available/)).toBeInTheDocument();
  });
});

describe('inventory routing in the web app', () => {
  it('renders the variant screen for a query route', async () => {
    const { router } = renderApp('/app/auth/login');
    await fillAndSubmit();
    await router.navigate('/app/timetable/sessions?view=room');
    expect(await screen.findByRole('heading', { name: 'Room/staff view' })).toBeInTheDocument();
    await router.navigate('/app/timetable/sessions');
    // the plain route is TT-02, built as its list manager
    expect(await screen.findByRole('heading', { name: 'Timetable builder' })).toBeInTheDocument();
  });
});
