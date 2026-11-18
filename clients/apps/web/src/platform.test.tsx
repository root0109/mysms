import {
  PLATFORM_ADMIN,
  SELF_SERVICE,
  fakePlatform,
  onePage,
  platformFixture,
  schoolAdmin,
} from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { signedIn } from './test/signedIn';

const admin = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...PLATFORM_ADMIN] };
const reader = { ...schoolAdmin, permissions: [...SELF_SERVICE] };

afterEach(() => vi.restoreAllMocks());

describe('ADM-07 Branding & theme', () => {
  it('edits a theme under its etag, uploads a logo and previews it', async () => {
    const store = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    const { api } = await signedIn('/app/platform/themes', admin, fakePlatform());
    expect(await screen.findByRole('heading', { name: 'Branding & theme' })).toBeInTheDocument();
    await userEvent.click(await screen.findByText('Classic'));
    const name = screen.getByLabelText('Name');
    await userEvent.clear(name);
    await userEvent.type(name, 'Classic blue');
    fireEvent.change(screen.getByLabelText('Colour color.primary'), { target: { value: '#1d4ed8' } });
    const picker = screen.getByLabelText('Upload logo');
    await userEvent.upload(picker, new File(['logo'], 'logo.png', { type: 'image/png' }));
    expect(await screen.findByText('logo.png is uploaded.')).toBeInTheDocument();
    // the bytes went to the signed URL, without the MySMS token
    const put = store.mock.calls.find(([url]) => String(url).startsWith('https://files.example/upload/'));
    expect(put?.[1]).toMatchObject({ method: 'PUT', headers: { 'Content-Type': 'image/png' } });
    expect(screen.getByRole('group', { name: 'Preview' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Theme saved.')).toBeInTheDocument();
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(JSON.parse(patch?.body ?? '{}')).toMatchObject({
      name: 'Classic blue',
      isDefault: true,
      logoFileId: platformFixture.fileId,
      tokens: { 'color.primary': '#1D4ED8' },
    });
    const create = api.calls.find((c) => c.path === '/v1/platform/files:createUploadUrl');
    expect(JSON.parse(create?.body ?? '{}')).toMatchObject({
      fileName: 'logo.png',
      mimeType: 'image/png',
      sizeBytes: '4',
    });
  });

  it('shows a reader the themes without editing', async () => {
    await signedIn('/app/platform/themes', reader, fakePlatform());
    await userEvent.click(await screen.findByText('Classic'));
    expect(screen.getByLabelText('Name')).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
  });
});

describe('ADM-13 Audit & access logs', () => {
  it('lists the trail of a date range and refuses a range that is too long', async () => {
    const { api } = await signedIn('/app/platform/audit-logs', admin, fakePlatform());
    expect(await screen.findByText('phone — Parent asked')).toBeInTheDocument();
    const list = api.calls.find((c) => c.path === '/v1/platform/audit-logs');
    expect(decodeURIComponent(list?.search ?? '')).toContain('occurredAt >= "');
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2020-01-01' } });
    expect(await screen.findByText(/up to 366 days/)).toBeInTheDocument();
  });

  it('tells a role without the trail so', async () => {
    await signedIn('/app/platform/audit-logs', reader, fakePlatform());
    expect(
      await screen.findByText('The audit trail is read by school administrators only.'),
    ).toBeInTheDocument();
  });
});

describe('ADM-14 Subscription & billing', () => {
  it('cancels the active subscription with a reason and the etag it read', async () => {
    const { api } = await signedIn('/app/platform/tenant-subscriptions', admin, fakePlatform());
    expect(await screen.findByText('Gold')).toBeInTheDocument();
    const row = (await screen.findByText('Active')).closest('tr') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: 'Cancel subscription' }));
    const dialog = screen.getByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText('Reason (optional)'), 'Closing the branch');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel subscription' }));
    expect(await screen.findByText('The subscription is cancelled.')).toBeInTheDocument();
    const cancel = api.calls.find((c) => c.path.endsWith(':cancel'));
    expect(JSON.parse(cancel?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Closing the branch' });
  });
});

describe('ACC-11 Global search', () => {
  it('searches from the address and lists what the caller may see', async () => {
    await signedIn('/app/platform/search?q=annual', admin, fakePlatform());
    expect(await screen.findByText('Annual day photos.zip')).toBeInTheDocument();
    expect(screen.getByText('Annual day')).toBeInTheDocument();
  });
});

describe('ADM-12 Imports & exports', () => {
  const institutions = {
    'GET /v1/org/institutions': (req: Request) =>
      onePage(req, [{ id: platformFixture.institutionId, code: 'MAIN', name: 'Main School' }]),
  };
  const importer = { ...admin, permissions: [...admin.permissions, 'org.institution.read'] };
  /** The platform fakes, with its operations answered where the backend fake routes every module's operations. */
  const platform = () => {
    const handlers = fakePlatform();
    const operations = handlers['GET /v1/platform/operations/{operationId}'];
    return operations ? { ...handlers, 'GET /v1/{module}/operations/{operationId}': operations } : handlers;
  };

  it('checks a CSV file, imports it and lists the refused rows', async () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null);
    const store = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    const { api } = await signedIn('/app/platform/import-export-jobs', importer, {
      ...platform(),
      ...institutions,
    });
    const card = (await screen.findByRole('heading', { name: 'Import records' })).closest(
      'section',
    ) as HTMLElement;
    const type = within(card).getByLabelText('What to import');
    expect(
      within(type)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Students', 'Guardians']);
    expect(await within(card).findByLabelText('Institution')).toHaveValue(platformFixture.institutionId);
    expect(within(card).getByRole('button', { name: 'Check file' })).toBeDisabled();

    await userEvent.click(within(card).getByRole('button', { name: 'Download template' }));
    await waitFor(() =>
      expect(open).toHaveBeenCalledWith(
        'https://files.example/import-template-students.csv?X-Goog-Signature=1',
        '_blank',
        'noopener',
      ),
    );

    await userEvent.upload(
      within(card).getByLabelText('Choose CSV file'),
      new File(['personId\n'], 'students.csv', { type: 'text/csv' }),
    );
    expect(await within(card).findByText('students.csv is uploaded.')).toBeInTheDocument();
    expect(store).toHaveBeenCalled();
    await userEvent.click(within(card).getByRole('button', { name: 'Check file' }));
    expect(await within(card).findByText('Checked only: nothing was imported.')).toBeInTheDocument();
    expect(within(card).getByText('3 rows read, 0 records created, 1 problem found.')).toBeInTheDocument();
    const errors = within(card).getByRole('table', { name: 'Rows that were refused' });
    expect(within(errors).getByText('personId')).toBeInTheDocument();
    expect(within(errors).getByText('required')).toBeInTheDocument();
    const dry = api.calls.find((c) => c.path === '/v1/platform/imports');
    expect(JSON.parse(dry?.body ?? '{}')).toMatchObject({
      entityType: 'students',
      fileId: platformFixture.fileId,
      institutionId: platformFixture.institutionId,
      dryRun: true,
    });
    expect(dry?.headers.get('Idempotency-Key')).toBeTruthy();

    await userEvent.click(within(card).getByRole('button', { name: 'Import' }));
    expect(await within(card).findByText('Importing')).toBeInTheDocument();
    expect(within(card).getByRole('progressbar', { name: 'Import progress' })).toBeInTheDocument();
    expect(await within(card).findByText('Finished', {}, { timeout: 4000 })).toBeInTheDocument();
    expect(within(card).getByText('3 rows read, 2 records created, 1 problem found.')).toBeInTheDocument();
  });

  it('checks guardians for every institution, without choosing one', async () => {
    const { api } = await signedIn('/app/platform/import-export-jobs', importer, {
      ...platform(),
      ...institutions,
    });
    const card = (await screen.findByRole('heading', { name: 'Import records' })).closest(
      'section',
    ) as HTMLElement;
    await userEvent.selectOptions(within(card).getByLabelText('What to import'), 'guardians');
    expect(within(card).getByText(/Guardians belong to every institution/)).toBeInTheDocument();
    expect(within(card).queryByLabelText('Institution')).not.toBeInTheDocument();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    await userEvent.upload(
      within(card).getByLabelText('Choose CSV file'),
      new File(['personId\n'], 'guardians.csv', { type: 'text/csv' }),
    );
    await userEvent.click(await within(card).findByRole('button', { name: 'Check file' }));
    await within(card).findByText('Checked only: nothing was imported.');
    const call = api.calls.find((c) => c.path === '/v1/platform/imports');
    expect(JSON.parse(call?.body ?? '{}')).not.toHaveProperty('institutionId');
  });

  it('runs a report and offers its file', async () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null);
    await signedIn('/app/platform/import-export-jobs', admin, fakePlatform());
    fireEvent.change(await screen.findByLabelText('From'), { target: { value: '2026-11-01' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-11-10' } });
    await userEvent.click(screen.getByRole('button', { name: 'Create file' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Download' }));
    await waitFor(() =>
      expect(open).toHaveBeenCalledWith(
        `https://files.example/${platformFixture.outputFileId}?X-Goog-Signature=1`,
        '_blank',
        'noopener',
      ),
    );
  });

  it('tells a role without imports so', async () => {
    await signedIn('/app/platform/import-export-jobs', reader, fakePlatform());
    expect(await screen.findByText('Your role may not import records.')).toBeInTheDocument();
  });
});
