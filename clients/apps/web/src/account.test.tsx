import { createRuntime } from '@mysms/core';
import {
  ACCOUNT_ADMIN,
  fakeApi,
  fakeBackend,
  fakeIam,
  iamFixture,
  schoolAdmin,
  type Handler,
} from '@mysms/fixtures';
import { WebApp } from '@mysms/web-kit';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { routes } from './routes';
import { signedIn } from './test/signedIn';

const fx = iamFixture;
const admin = { ...schoolAdmin, permissions: [...ACCOUNT_ADMIN] };

function signedOut(path: string, extra: Record<string, Handler> = fakeIam()) {
  const api = fakeApi({ ...fakeBackend({ me: schoolAdmin }), ...extra });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'web/test/jsdom',
    fetch: api.fetch,
  });
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(<WebApp runtime={runtime} router={router} />);
  return { api, router };
}

describe('ACC-02 Forgot / reset password', () => {
  it('asks for a code with the same answer for everyone, then sets a new password with it', async () => {
    const { api } = signedOut('/app/auth/reset');
    await userEvent.type(await screen.findByLabelText('School code'), 'demo');
    await userEvent.type(screen.getByLabelText('Username, e-mail or mobile'), 'meera');
    await userEvent.click(screen.getByRole('button', { name: 'Send a reset code' }));
    expect(await screen.findByText(/If an account matches/)).toBeInTheDocument();
    expect(api.calls.find((c) => c.path === '/v1/iam/auth:requestPasswordReset')?.body).toBe(
      '{"tenantCode":"demo","identifier":"meera"}',
    );

    await userEvent.click(screen.getByRole('button', { name: 'I have a code' }));
    await userEvent.type(screen.getByLabelText('Code from the e-mail'), 'stale');
    await userEvent.type(screen.getByLabelText('New password'), 'a-new-strong-password');
    await userEvent.type(screen.getByLabelText('Repeat the new password'), 'a-new-strong-password');
    await userEvent.click(screen.getByRole('button', { name: 'Set the new password' }));
    expect(await screen.findByText(/This code is not valid or has expired/)).toBeInTheDocument();
    await userEvent.clear(screen.getByLabelText('Code from the e-mail'));
    await userEvent.type(screen.getByLabelText('Code from the e-mail'), fx.resetToken);
    await userEvent.click(screen.getByRole('button', { name: 'Set the new password' }));
    expect(await screen.findByText(/Your new password is set/)).toBeInTheDocument();
  });

  it('takes the code from the e-mail link and checks the password before sending it', async () => {
    const { api } = signedOut(`/app/auth/reset?token=${encodeURIComponent(fx.resetToken)}`);
    await userEvent.type(await screen.findByLabelText('New password'), 'short');
    await userEvent.click(screen.getByRole('button', { name: 'Set the new password' }));
    expect(screen.getByText(/at least 12/)).toBeInTheDocument();
    expect(screen.queryByLabelText('Code from the e-mail')).not.toBeInTheDocument();
    expect(api.calls.some((c) => c.path === '/v1/iam/auth:resetPassword')).toBe(false);
  });
});

describe('ACC-03 Sign up', () => {
  it('signs a student up and shows the same receipt whatever the address', async () => {
    const { api } = signedOut('/app/auth/signup');
    await userEvent.click(await screen.findByRole('button', { name: 'Sign up' }));
    expect(screen.getByText('Accept the terms to sign up.')).toBeInTheDocument();
    expect(screen.getByText('Choose your school.')).toBeInTheDocument();
    expect(screen.getByText('Enter the school code to see its schools.')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('School code'), 'demo');
    await userEvent.type(screen.getByLabelText('Full name'), 'Asha Learner');
    expect(await screen.findByRole('option', { name: 'South School' })).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('School'), 'South School');
    await userEvent.type(screen.getByLabelText('E-mail'), 'asha@school.example');
    await userEvent.click(screen.getByRole('switch', { name: /I accept the terms/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Sign up' }));
    expect(await screen.findByText(/We sent a message to asha@school.example/)).toBeInTheDocument();
    const body = JSON.parse(api.calls.find((c) => c.path === '/v1/iam/auth:signUp')?.body ?? '{}') as object;
    expect(body).toMatchObject({
      acceptTerms: true,
      personaCategory: 'student',
      institutionId: fx.southSchoolId,
    });
    expect(api.calls.filter((c) => c.path.endsWith('/sign-up-institutions')).map((c) => c.path)).toEqual([
      '/v1/iam/tenant-codes/demo/sign-up-institutions',
    ]);
  });

  it('offers no school where the school code does not accept sign-ups', async () => {
    signedOut('/app/auth/signup');
    await userEvent.type(await screen.findByLabelText('School code'), 'elsewhere');
    await userEvent.tab();
    expect(
      await screen.findByText('This school does not accept sign-ups. Ask the school for an account.'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('School')).toBeDisabled();
  });
});

describe('ACC-05 Verify e-mail', () => {
  it('confirms the code of the e-mail link without a session', async () => {
    signedOut(`/app/auth/verify?channel=email&code=${encodeURIComponent(fx.verificationCode)}`);
    await userEvent.click(await screen.findByRole('button', { name: 'Confirm' }));
    expect(await screen.findByText('Your e-mail address is confirmed.')).toBeInTheDocument();
  });
});

describe('ACC-04 Accept terms', () => {
  it('stands before every page until the user accepts', async () => {
    const fresh = { ...admin, user: { ...schoolAdmin.user, termsAcceptedAt: undefined } };
    const { api } = await signedIn('/app/iam/me/security', fresh, fakeIam({ me: fresh }));
    expect(await screen.findByRole('heading', { name: 'Terms of use' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'I accept' }));
    expect(await screen.findByRole('heading', { name: 'API tokens' })).toBeInTheDocument();
    expect(api.calls.find((c) => c.path === '/v1/iam/me:acceptTerms')?.body).toBe(
      '{"termsVersion":"2026-11"}',
    );
  });
});

describe('ACC-06 Persona switcher', () => {
  it("switches to one of the user's own roles", async () => {
    const { api } = await signedIn('/app', admin, fakeIam());
    await userEvent.click(await screen.findByRole('button', { name: 'All my roles' }));
    const sheet = await screen.findByRole('dialog', { name: 'Work as' });
    await userEvent.click(within(sheet).getByRole('button', { name: 'reviewer (whole school)' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Work as' })).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'reviewer (whole school)' })).toBeInTheDocument();
    expect(api.calls.find((c) => c.path === '/v1/iam/me:switchRole')?.body).toBe(
      `{"roleId":"${fx.reviewerRoleId}"}`,
    );
  });
});

describe('ACC-08 API tokens and other sessions', () => {
  it('shows a new token once, revokes one under its ETag and signs other sessions out', async () => {
    const { api } = await signedIn('/app/iam/me/security', admin, fakeIam());
    const tokens = (await screen.findByRole('heading', { name: 'API tokens' })).closest(
      'section',
    ) as HTMLElement;
    expect(await within(tokens).findByText('Reports export')).toBeInTheDocument();
    await userEvent.type(within(tokens).getByLabelText('Name'), 'CI export');
    await userEvent.click(within(tokens).getByRole('button', { name: 'Create token' }));
    expect(await within(tokens).findByText('mysms_pat_secret-shown-once')).toBeInTheDocument();
    await userEvent.click(within(tokens).getAllByRole('button', { name: 'Revoke' })[0] as HTMLElement);
    await waitFor(() =>
      expect(
        api.calls
          .find((c) => c.method === 'DELETE' && c.path.includes('/api-tokens/'))
          ?.headers.get('If-Match'),
      ).toBe('"1"'),
    );

    const sessions = screen
      .getByRole('heading', { name: 'Signed-in sessions' })
      .closest('section') as HTMLElement;
    expect(await within(sessions).findByText('Firefox (this device)')).toBeInTheDocument();
    await userEvent.click(within(sessions).getByRole('button', { name: 'Sign out all other sessions' }));
    await waitFor(() => expect(within(sessions).queryByText('Android app')).not.toBeInTheDocument());
    expect(api.calls.filter((c) => c.method === 'DELETE' && c.path.includes('/sessions/'))).toHaveLength(1);
  });
});

describe('ADM-08 Users and ADM-09 User detail', () => {
  it('lists accounts by status and opens one', async () => {
    const { router } = await signedIn('/app/iam/user-accounts', admin, fakeIam());
    const table = await screen.findByRole('table', { name: 'Users' });
    expect(await within(table).findByRole('link', { name: 'meera' })).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'Locked');
    await waitFor(() =>
      expect(within(screen.getByRole('table', { name: 'Users' })).queryByText('meera')).toBeNull(),
    );
    expect(router.state.location.search).toBe('?status=locked');
    await userEvent.click(await screen.findByRole('link', { name: 'kabir' }));
    await waitFor(() => expect(router.state.location.pathname).toBe(`/app/iam/user-accounts/${fx.lockedId}`));
  });

  it('adds an account after checking the login name', async () => {
    const { router } = await signedIn('/app/iam/user-accounts', admin, fakeIam());
    await userEvent.click(await screen.findByRole('button', { name: 'Add user' }));
    const dialog = await screen.findByRole('dialog', { name: 'New user account' });
    await userEvent.type(within(dialog).getByLabelText('Username'), 'meera');
    expect(await within(dialog).findByText('This username is taken.')).toBeInTheDocument();
    expect(within(dialog).getByText('Free: meera2')).toBeInTheDocument();
    await userEvent.clear(within(dialog).getByLabelText('Username'));
    await userEvent.type(within(dialog).getByLabelText('Username'), 'nisha');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create' }));
    await waitFor(() =>
      expect(router.state.location.pathname).toMatch(/^\/app\/iam\/user-accounts\/[0-9a-f-]+$/),
    );
  });

  it('activates an account, replaces its roles and acts as it until stopped', async () => {
    const { api, router } = await signedIn(`/app/iam/user-accounts/${fx.studentId}`, admin, fakeIam());
    await userEvent.click(await screen.findByRole('button', { name: 'Activate' }));
    expect(await screen.findByText('Saved.')).toBeInTheDocument();
    expect(api.calls.find((c) => c.path.endsWith(':activate'))?.body).toBe('{"etag":"\\"1\\""}');

    const roles = screen.getByRole('heading', { name: 'Roles' }).closest('section') as HTMLElement;
    expect(await within(roles).findByText('No roles granted.')).toBeInTheDocument();
    await userEvent.selectOptions(within(roles).getByLabelText('Role'), 'Reviewer');
    await userEvent.click(within(roles).getByRole('button', { name: 'Add role' }));
    await userEvent.click(within(roles).getByRole('button', { name: 'Save roles' }));
    await waitFor(() =>
      expect(api.calls.find((c) => c.path.endsWith(':setRoles'))?.body).toBe(
        '{"grants":[{"roleCode":"reviewer","scopeType":"tenant"}]}',
      ),
    );

    const act = screen.getByRole('heading', { name: 'Act as this user' }).closest('section') as HTMLElement;
    await userEvent.click(within(act).getByRole('button', { name: 'Act as this user' }));
    expect(within(act).getByText('This field is required.')).toBeInTheDocument();
    await userEvent.type(within(act).getByLabelText('Reason'), 'Ticket 7');
    await userEvent.click(within(act).getByRole('button', { name: 'Act as this user' }));
    expect(await screen.findByText(/You are acting as meera/)).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/app');
    await userEvent.click(screen.getByRole('button', { name: 'Stop acting as this user' }));
    await waitFor(() => expect(screen.queryByText(/You are acting as/)).not.toBeInTheDocument());
    expect(api.calls.some((c) => c.path === '/v1/iam/auth:stopImpersonation')).toBe(true);
  });

  it('tells a user without access that the list is not theirs', async () => {
    await signedIn(
      '/app/iam/user-accounts',
      schoolAdmin,
      fakeIam({ permissions: ['iam.user_account.get_me'] }),
    );
    expect(await screen.findByText('You do not have access to user accounts.')).toBeInTheDocument();
  });
});
