import { createRuntime } from '@mysms/core';
import { fakeApi, fakeBackend, schoolAdmin, TENANT_CODE, type Handler } from '@mysms/fixtures';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WebApp } from '@mysms/web-kit';
import { createMemoryRouter } from 'react-router';
import { expect } from 'vitest';
import { routes } from '../routes';

/**
 * Renders the web app against a fake backend, signs the account in and waits for the screen at `path` (which may
 * carry a query).
 */
export async function signedIn(path: string, me = schoolAdmin, extra: Record<string, Handler> = {}) {
  const api = fakeApi({ ...fakeBackend({ me }), ...extra });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'web/test/jsdom',
    fetch: api.fetch,
  });
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  const { unmount } = render(<WebApp runtime={runtime} router={router} />);
  await userEvent.type(await screen.findByLabelText('School code'), TENANT_CODE);
  await userEvent.type(screen.getByLabelText('Username, e-mail or mobile'), me.user?.username ?? '');
  await userEvent.type(screen.getByLabelText('Password'), 'password');
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
  await waitFor(() => expect(router.state.location.pathname + router.state.location.search).toBe(path));
  return { api, router, runtime, unmount };
}
