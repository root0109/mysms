import { createRuntime } from '@mysms/core';
import {
  COMMS_STAFF,
  PEOPLE_OFFICE,
  SELF_SERVICE,
  SERVICES_MEMBER,
  commsFixture as fx,
  fakeApi,
  fakeBackend,
  fakeComms,
  fakePeople,
  fakeServices,
  schoolAdmin,
} from '@mysms/fixtures';
import { WebApp } from '@mysms/web-kit';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { routes } from './routes';
import { signedIn } from './test/signedIn';

const member = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...SERVICES_MEMBER] };
const bodyOf = (api: Awaited<ReturnType<typeof signedIn>>['api'], method: string, suffix: string) =>
  JSON.parse(
    api.calls.filter((c) => c.method === method && c.path.endsWith(suffix)).at(-1)?.body ?? '{}',
  ) as Record<string, unknown>;

describe('LIB-07 My loans and LIB-02 Title detail', () => {
  it("lists the loans on the member's own card with each title, overdue said in words", async () => {
    const { api } = await signedIn('/app/library/loans?mine', member, fakeServices());
    expect(await screen.findByText('Wings of Fire')).toBeInTheDocument();
    expect(screen.getByText(/Issued · issued .* · due .* · Overdue/)).toBeInTheDocument();
    const cards = api.calls.find((c) => c.path === '/v1/library/library-cards');
    expect(decodeURIComponent(cards?.search ?? '')).toContain(`personId = "${fx.personId}"`);
    const loans = api.calls.find((c) => c.path === '/v1/library/loans');
    expect(decodeURIComponent(loans?.search ?? '')).toContain(`libraryCardId = "${fx.cardId}"`);
  });

  it('shows a title with its copies on their own tab', async () => {
    await signedIn(`/app/library/media/${fx.mediaId}`, member, fakeServices());
    expect(await screen.findByRole('heading', { name: 'Wings of Fire' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('radio', { name: 'Copies' }));
    expect(await screen.findByText('A-0001')).toBeInTheDocument();
  });
});

describe('CAR-01 Job board and CAR-02 Opening detail', () => {
  it('are read by a guest of the school, who is asked to sign in to apply', async () => {
    const api = fakeApi({ ...fakeBackend({ me: schoolAdmin }), ...fakeServices() });
    const runtime = createRuntime({
      baseUrl: 'https://school.example.com',
      clientId: 'web/test/jsdom',
      fetch: api.fetch,
      tenantCode: 'demo',
    });
    const router = createMemoryRouter(routes, { initialEntries: ['/app/careers/job-openings'] });
    render(<WebApp runtime={runtime} router={router} />);
    await userEvent.click(await screen.findByRole('link', { name: 'Mathematics teacher' }));
    expect(await screen.findByRole('heading', { name: 'Mathematics teacher' })).toBeInTheDocument();
    expect(screen.getByText('Grades 6 to 8.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sign in to apply' })).toBeInTheDocument();
    const board = api.calls.find((c) => c.path === '/v1/careers/job-openings');
    expect(decodeURIComponent(board?.search ?? '')).toContain('status = "open"');
    expect(board?.headers.get('X-Tenant-Code')).toBe('demo');
  });

  it('applies with a résumé as the signed-in candidate', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    // a public route shows no sign-in: sign in at home, then open the opening
    const candidate = {
      ...member,
      permissions: [
        ...member.permissions,
        'platform.file_object.create_upload_url',
        'platform.file_object.complete_upload',
      ],
    };
    const { api, router } = await signedIn('/app', candidate, { ...fakeServices(), ...fakePeople() });
    await router.navigate(`/app/careers/job-openings/${fx.openingId}`);
    await userEvent.upload(
      await screen.findByLabelText('Upload your résumé and apply'),
      new File(['cv'], 'cv.pdf', { type: 'application/pdf' }),
    );
    expect(await screen.findByText('Your application was sent.')).toBeInTheDocument();
    const sent = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/applications'));
    expect(sent?.path).toBe(`/v1/careers/job-openings/${fx.openingId}/applications`);
    // the applicant is the caller, from the token: never sent
    expect(JSON.parse(sent?.body ?? '{}')).not.toHaveProperty('applicantUserId');
  });
});

describe('CAR-03 Employer console and CAR-04 My applications', () => {
  it("lists only the poster's own openings", async () => {
    const { api, router } = await signedIn('/app', member, fakeServices());
    await router.navigate('/app/careers/job-openings?mine');
    expect(await screen.findByText('Mathematics teacher')).toBeInTheDocument();
    const listed = api.calls.find((c) => c.path === '/v1/careers/job-openings');
    expect(decodeURIComponent(listed?.search ?? '')).toContain(`postedByUserId = "${fx.me}"`);
  });

  it("shows the candidate's applications with their state", async () => {
    await signedIn('/app/careers/job-applications?mine', member, fakeServices());
    expect(await screen.findByRole('link', { name: 'Librarian' })).toBeInTheDocument();
    expect(screen.getByText('Shortlisted')).toBeInTheDocument();
  });
});

describe('SIS-12 Birthdays', () => {
  it("lists the week's birthdays and anniversaries and greets the people ticked", async () => {
    const office = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...COMMS_STAFF] };
    const { api } = await signedIn('/app/people/birthdays', office, {
      ...fakeComms(),
      'GET /v1/org/institutions': () =>
        new Response(
          JSON.stringify({
            data: [{ id: fx.institutionId, name: 'Main School' }],
            paging: { page: 1, size: 1, first: '/v1/org/institutions' },
          }),
          { headers: { 'Content-Type': 'application/json' } },
        ),
    });
    expect(await screen.findByText('Diya Menon')).toBeInTheDocument();
    expect(screen.getByText('Anil Rao')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('checkbox', { name: 'Work anniversary' }));
    await waitFor(() => expect(screen.queryByText('Anil Rao')).not.toBeInTheDocument());

    await userEvent.click(screen.getByRole('checkbox', { name: 'Greet Diya Menon' }));
    const template = screen.getByLabelText('Template');
    await screen.findByRole('option', { name: 'Birthday wishes' });
    await userEvent.selectOptions(template, fx.templateId);
    await userEvent.click(screen.getByRole('button', { name: 'Greet 1 person' }));
    expect(await screen.findByText('1 greetings queued, 0 skipped.')).toBeInTheDocument();
    expect(bodyOf(api, 'POST', ':sendGreetings')).toEqual({
      personIds: [fx.birthdayPersonId],
      templateId: fx.templateId,
      channel: 'email',
    });
  });
});

describe('ACC-12 Public profile', () => {
  it('is created from the profile and needs the consent first', async () => {
    const person = {
      ...schoolAdmin,
      permissions: [...SELF_SERVICE, ...PEOPLE_OFFICE, 'people.public_profile.create'],
    };
    await signedIn('/app/people/me', person, {
      ...fakePeople(),
      'POST /v1/people/persons/{personId}/public-profile': () =>
        new Response(JSON.stringify({ status: 422, code: 'CONSENT_REQUIRED', title: 'Consent required' }), {
          status: 422,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
    });
    await userEvent.click(await screen.findByRole('button', { name: 'Create my public profile' }));
    expect(await screen.findByText('A public profile needs your consent first.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Privacy and consent' })).toHaveAttribute(
      'href',
      `/app/people/persons/${fx.personId}/consents`,
    );
  });

  it("reads the person's own profile, never another person's", async () => {
    const person = {
      ...schoolAdmin,
      permissions: [...SELF_SERVICE, 'people.public_profile.read', 'people.public_profile.update'],
    };
    const profileId = '01960000-0000-7000-8000-0000000000e1';
    const { api } = await signedIn(`/app/people/public-profiles/${profileId}`, person, {
      'GET /v1/people/persons/{personId}/public-profile/{publicProfileId}': (_req, p) =>
        new Response(
          JSON.stringify({ id: p.publicProfileId, personId: p.personId, headline: 'Maths teacher' }),
          {
            headers: { 'Content-Type': 'application/json', ETag: '"3"' },
          },
        ),
    });
    expect(await screen.findByText('Maths teacher')).toBeInTheDocument();
    expect(api.calls.find((c) => c.path.includes('/public-profile/'))?.path).toBe(
      `/v1/people/persons/${fx.personId}/public-profile/${profileId}`,
    );
  });
});
