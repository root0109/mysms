import { consentMinorStudent, consentParent, fakePrivacy, privacyFixture } from '@mysms/fixtures';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const route = '/app/privacy/my/consents';
const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;
const PHOTOS = 'Photos and videos of school events';

describe('ACC-13 Consent centre', () => {
  it('lets a parent read the notice and allow photos for one child, at a width they choose', async () => {
    const { api } = await signedIn(route, consentParent, fakePrivacy());
    expect(await screen.findByRole('heading', { name: 'Privacy and consent' })).toBeInTheDocument();
    // one child at a time, named from the children's cards
    const switcher = await screen.findByRole('radiogroup', { name: 'Whose choices' });
    expect(await within(switcher).findByRole('radio', { name: 'Asha Rao' })).toBeChecked();
    expect(within(switcher).getByRole('radio', { name: 'Kabir Rao' })).not.toBeChecked();

    const photos = card(PHOTOS);
    expect(within(photos).getByText('Not answered yet')).toBeInTheDocument();
    expect(within(photos).getByText('Not shared at the moment.')).toBeInTheDocument();
    // no notice published yet: nothing to decide
    expect(
      within(card('Optional SMS and WhatsApp messages')).getByText(/has not published the notice/),
    ).toBeInTheDocument();
    expect(
      within(card('Optional SMS and WhatsApp messages')).queryByRole('button', { name: 'Read and decide' }),
    ).not.toBeInTheDocument();

    await userEvent.click(within(photos).getByRole('button', { name: 'Read and decide' }));
    const dialog = await screen.findByRole('dialog', { name: PHOTOS });
    expect(await within(dialog).findByText(privacyFixture.english)).toBeInTheDocument();
    expect(within(dialog).getByText(privacyFixture.forChildren)).toBeInTheDocument();
    expect(within(dialog).getByText(/Ritu Sharma · Grievance officer/)).toBeInTheDocument();
    // the notice in Hindi, and nothing is chosen in advance
    await userEvent.selectOptions(within(dialog).getByLabelText('Language'), 'hi');
    expect(await within(dialog).findByText(privacyFixture.hindi)).toBeInTheDocument();
    const widths = within(dialog).getByRole('radiogroup', { name: 'Allow sharing with' });
    expect(
      within(widths)
        .getAllByRole('radio')
        .map((r) => (r as HTMLInputElement).checked),
    ).toEqual([false, false]);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Allow' }));
    expect(within(dialog).getByRole('alert')).toHaveTextContent('Choose how far it may be shared.');
    await userEvent.click(within(widths).getByRole('radio', { name: 'the whole school' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Allow' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const sent = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/privacy/my/consents');
    expect(JSON.parse(sent?.body ?? '{}')).toEqual({
      subjectPersonId: privacyFixture.ashaPersonId,
      purposeCode: privacyFixture.photos,
      scope: 'institution',
      decision: 'granted',
      noticeVersionId: privacyFixture.noticeVersionId,
      noticeLanguage: 'hi',
    });
    expect(sent?.headers.get('Idempotency-Key')).toBeTruthy();
    expect(await within(card(PHOTOS)).findByText('Allowed')).toBeInTheDocument();
    expect(within(card(PHOTOS)).getByText('Currently shared with: the whole school')).toBeInTheDocument();
    expect(within(card(PHOTOS)).getByText('Your answer is saved.')).toBeInTheDocument();

    // the other child's answers are separate
    await userEvent.click(within(switcher).getByRole('radio', { name: 'Kabir Rao' }));
    expect(await within(card(PHOTOS)).findByText('Not answered yet')).toBeInTheDocument();
  });

  it('refuses everywhere with "Don\'t allow", then withdraws the answer after a confirmation', async () => {
    const { api } = await signedIn(route, consentParent, fakePrivacy());
    const marketing = 'Promotional and third-party messages';
    await screen.findByRole('heading', { name: marketing });
    await userEvent.click(within(card(marketing)).getByRole('button', { name: 'Read and decide' }));
    const dialog = await screen.findByRole('dialog', { name: marketing });
    // a whole-school purpose has no widths to choose
    expect(within(dialog).queryByRole('radiogroup', { name: 'Allow sharing with' })).not.toBeInTheDocument();
    await within(dialog).findByText(privacyFixture.english);
    await userEvent.click(within(dialog).getByRole('button', { name: "Don't allow" }));
    expect(await within(card(marketing)).findByText('Not allowed')).toBeInTheDocument();
    expect(
      JSON.parse(
        api.calls.find((c) => c.method === 'POST' && c.path === '/v1/privacy/my/consents')?.body ?? '{}',
      ),
    ).toMatchObject({ scope: 'tenant', decision: 'refused', noticeLanguage: 'en' });

    await userEvent.click(within(card(marketing)).getByRole('button', { name: 'Withdraw my answer' }));
    const confirm = await screen.findByRole('dialog', { name: 'Withdraw your answer?' });
    await userEvent.click(within(confirm).getByRole('button', { name: 'Withdraw' }));
    expect(await within(card(marketing)).findByText('Your answer is withdrawn.')).toBeInTheDocument();
    expect(within(card(marketing)).getByText('Not answered yet')).toBeInTheDocument();
    const withdrawal = api.calls.find((c) => c.path.endsWith(':withdraw'));
    expect(JSON.parse(withdrawal?.body ?? '{}')).toEqual({ etag: '"1"' });
  });

  it("shows a student under 18 the effect of their guardians' answers, read only", async () => {
    await signedIn(route, consentMinorStudent, fakePrivacy({ as: 'minor' }));
    await screen.findByRole('heading', { name: PHOTOS });
    const photos = card(PHOTOS);
    expect(within(photos).getByText('Not shared at the moment.')).toBeInTheDocument();
    expect(
      within(photos).getByText(
        /Your parents or guardians decide this until .*2029\. From then on, you decide\./,
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole('radiogroup', { name: 'Whose choices' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Read and decide' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Withdraw my answer' })).not.toBeInTheDocument();
    expect(within(photos).queryByText('Not answered yet')).not.toBeInTheDocument();
  });
});
