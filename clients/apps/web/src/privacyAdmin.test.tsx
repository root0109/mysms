import { PRIVACY_ADMIN, SELF_SERVICE, fakePrivacyAdmin, schoolAdmin } from '@mysms/fixtures';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const admin = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...PRIVACY_ADMIN] };
const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;
const PHOTOS = 'Photos and videos of school events';

/** Writes and publishes the first version of a new notice asking about photos, in English. */
async function publishPhotoNotice() {
  const add = card('Add a notice');
  await userEvent.type(within(add).getByLabelText('Code'), 'photo_notice');
  await userEvent.type(within(add).getByLabelText('Title'), 'Photos at school');
  await userEvent.click(within(add).getByRole('button', { name: 'Add a notice' }));
  await screen.findByRole('heading', { name: 'Photos at school' });
  await userEvent.click(within(card('Photos at school')).getByRole('button', { name: 'Versions' }));

  await screen.findByRole('heading', { name: 'New draft version' });
  const draft = card('New draft version');
  await userEvent.click(within(draft).getByRole('switch', { name: PHOTOS }));
  await userEvent.type(within(draft).getByLabelText('Grievance officer'), 'Ritu Sharma');
  await userEvent.type(within(draft).getByLabelText('E-mail'), 'grievance@school.example');
  await userEvent.click(within(draft).getByRole('button', { name: 'New draft version' }));

  await screen.findByRole('heading', { name: 'Version 1' });
  const version = card('Version 1');
  expect(within(version).getByText('Draft')).toBeInTheDocument();
  // publishing needs wording first: the server's sentence is shown
  await userEvent.click(within(version).getByRole('button', { name: 'Publish' }));
  await userEvent.click(
    within(await screen.findByRole('dialog', { name: 'Publish' })).getByRole('button', { name: 'Publish' }),
  );
  expect(
    await within(card('Version 1')).findByText(/needs a translation before it is published/),
  ).toBeInTheDocument();

  await userEvent.type(
    within(card('Version 1')).getByLabelText('Notice text'),
    'We publish photos of school events.',
  );
  await userEvent.click(within(card('Version 1')).getByRole('button', { name: 'Add a language' }));
  expect(await within(card('Version 1')).findByRole('heading', { name: 'English' })).toBeInTheDocument();
  await userEvent.click(within(card('Version 1')).getByRole('button', { name: 'Publish' }));
  await userEvent.click(
    within(await screen.findByRole('dialog', { name: 'Publish' })).getByRole('button', { name: 'Publish' }),
  );
  expect(await within(card('Version 1')).findByText('Active')).toBeInTheDocument();
  expect(
    within(card('Version 1')).getByText(/Published versions are kept exactly as families saw them/),
  ).toBeInTheDocument();
  expect(within(card('Version 1')).queryByLabelText('Notice text')).not.toBeInTheDocument();
}

describe('ADM-18 Privacy notices', () => {
  it('writes a notice version, refuses to publish it without wording, then publishes and freezes it', async () => {
    const { api } = await signedIn('/app/privacy/notices', admin, fakePrivacyAdmin());
    expect(await screen.findByText(/No notices yet/)).toBeInTheDocument();
    await publishPhotoNotice();
    const created = api.calls.find((c) => c.method === 'POST' && c.path.endsWith('/versions'));
    expect(JSON.parse(created?.body ?? '{}')).toEqual({
      purposeCodes: ['photo_video_publication'],
      requiresReconsent: false,
      grievanceContact: { name: 'Ritu Sharma', email: 'grievance@school.example' },
    });
    // a notice with a published version is consent evidence and is not deleted
    await userEvent.click(within(card('Photos at school')).getByRole('button', { name: 'Delete notice' }));
    await userEvent.click(
      within(await screen.findByRole('dialog', { name: 'Delete notice' })).getByRole('button', {
        name: 'Delete',
      }),
    );
    expect(await within(card('Photos at school')).findByText(/kept as consent evidence/)).toBeInTheDocument();
  });
});

describe('ADM-17 Privacy purposes', () => {
  it('switches a purpose on only with a notice that asks for it, under the row ETag', async () => {
    const { api } = await signedIn('/app/privacy/purposes', admin, fakePrivacyAdmin());
    await screen.findByRole('heading', { name: PHOTOS });
    // MySMS decides product analytics: it cannot be switched on by the school
    expect(
      within(card('Product analytics by MySMS')).getByRole('switch', { name: 'Ask families about this' }),
    ).toBeDisabled();

    await userEvent.click(within(card(PHOTOS)).getByRole('switch', { name: 'Ask families about this' }));
    await userEvent.click(within(card(PHOTOS)).getByRole('button', { name: 'Save' }));
    expect(
      await within(card(PHOTOS)).findByText(/must name a notice whose active version lists this purpose/),
    ).toBeInTheDocument();
    const refused = api.calls.find((c) => c.method === 'PATCH');
    expect(refused?.headers.get('If-Match')).toBe('"0"');
    expect(refused?.search).toBe('');
    expect(Object.keys(JSON.parse(refused?.body ?? '{}'))).toEqual(['enabled', 'noticeId', 'maxScope']);
  });

  it('saves the switch, notice and ceiling once the notice is published', async () => {
    const { api, router } = await signedIn('/app/privacy/notices', admin, fakePrivacyAdmin());
    await screen.findByText(/No notices yet/);
    await publishPhotoNotice();
    await router.navigate('/app/privacy/purposes');
    await screen.findByRole('heading', { name: PHOTOS });
    const photos = card(PHOTOS);
    await userEvent.click(within(photos).getByRole('switch', { name: 'Ask families about this' }));
    await userEvent.selectOptions(within(photos).getByLabelText('Notice'), 'Photos at school (photo_notice)');
    await userEvent.selectOptions(
      within(photos).getByLabelText('Widest sharing the school allows'),
      'the whole school',
    );
    await userEvent.click(within(photos).getByRole('button', { name: 'Save' }));
    expect(await within(card(PHOTOS)).findByText(/Saved. Families see the change/)).toBeInTheDocument();
    await waitFor(() => expect(within(card(PHOTOS)).getByText('On')).toBeInTheDocument());
    const saved = api.calls.filter((c) => c.method === 'PATCH').at(-1);
    expect(JSON.parse(saved?.body ?? '{}')).toMatchObject({ enabled: true, maxScope: 'institution' });
  });
});
