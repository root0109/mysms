import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakePrivacyAdmin, json } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  useCreateNotice,
  useCreateNoticeTranslation,
  useCreateNoticeVersion,
  useDeleteNotice,
  useDeleteNoticeTranslation,
  useNoticeTranslations,
  useNoticeVersions,
  useNotices,
  usePublishNoticeVersion,
  usePurposes,
  useUpdateNotice,
  useUpdateNoticeTranslation,
  useUpdateNoticeVersion,
  useUpdatePurpose,
} from './privacyAdmin';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: 'u1', username: 'ravi', locale: 'en-IN' };
const contact = { name: 'Ritu Sharma', email: 'grievance@school.example' };

async function setup() {
  const admin = fakePrivacyAdmin();
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    ...admin,
    // the fake keeps no translations to change or remove: these two answer like the server's success
    'PATCH /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId}':
      () => json({ id: 't1', language: 'en', bodyMarkdown: 'Changed.', etag: '"2"' }),
    'DELETE /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId}':
      () => new Response(null, { status: 204 }),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'ravi', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

describe('privacy administration (ADM-17, ADM-18)', () => {
  it('writes, words and publishes a notice, then switches a purpose on with it', async () => {
    const { api, wrapper } = await setup();
    const { result, rerender } = renderHook(
      ({ noticeId, versionId }: { noticeId: string | null; versionId: string | null }) => ({
        purposes: usePurposes(),
        notices: useNotices(),
        versions: useNoticeVersions(noticeId),
        translations: useNoticeTranslations(noticeId ?? '', versionId),
        createNotice: useCreateNotice(),
        updateNotice: useUpdateNotice(),
        deleteNotice: useDeleteNotice(),
        createVersion: useCreateNoticeVersion(),
        updateVersion: useUpdateNoticeVersion(),
        publish: usePublishNoticeVersion(),
        createTranslation: useCreateNoticeTranslation(),
        updateTranslation: useUpdateNoticeTranslation(),
        deleteTranslation: useDeleteNoticeTranslation(),
        updatePurpose: useUpdatePurpose(),
      }),
      { wrapper, initialProps: { noticeId: null as string | null, versionId: null as string | null } },
    );
    await waitFor(() => expect(result.current.purposes.data?.items).toHaveLength(3));
    expect(result.current.versions.fetchStatus).toBe('idle');

    const notice = await act(() =>
      result.current.createNotice.mutateAsync({ code: 'photos', title: 'Photos' }),
    );
    const renamed = await act(() =>
      result.current.updateNotice.mutateAsync({
        noticeId: notice.id ?? '',
        etag: notice.etag ?? '',
        title: 'Photos at school',
      }),
    );
    expect(renamed.title).toBe('Photos at school');
    const noticeId = notice.id ?? '';
    const draft = await act(() =>
      result.current.createVersion.mutateAsync({
        noticeId,
        draft: {
          purposeCodes: ['photo_video_publication'],
          grievanceContact: contact,
          requiresReconsent: false,
        },
      }),
    );
    const versionId = draft.id ?? '';
    const edited = await act(() =>
      result.current.updateVersion.mutateAsync({
        noticeId,
        noticeVersionId: versionId,
        etag: draft.etag ?? '',
        changes: { requiresReconsent: true },
      }),
    );
    const lastPatch = () => api.calls.filter((c) => c.method === 'PATCH').at(-1);
    expect(lastPatch()?.search).toBe('');
    expect(JSON.parse(lastPatch()?.body ?? '')).toEqual({ requiresReconsent: true });
    rerender({ noticeId, versionId });
    await waitFor(() => expect(result.current.versions.data?.items[0]?.requiresReconsent).toBe(true));

    const wording = await act(() =>
      result.current.createTranslation.mutateAsync({
        noticeId,
        noticeVersionId: versionId,
        wording: { language: 'en', bodyMarkdown: 'We publish photos.' },
      }),
    );
    await waitFor(() => expect(result.current.translations.data?.items).toHaveLength(1));
    await act(() =>
      result.current.updateTranslation.mutateAsync({
        noticeId,
        noticeVersionId: versionId,
        noticeTranslationId: wording.id ?? '',
        etag: wording.etag ?? '',
        changes: { bodyMarkdown: 'Changed.', summaryForChildren: undefined },
      }),
    );
    // a field the change names without a value is sent as null, which clears it
    expect(JSON.parse(lastPatch()?.body ?? '')).toEqual({
      bodyMarkdown: 'Changed.',
      summaryForChildren: null,
    });
    const published = await act(() =>
      result.current.publish.mutateAsync({ noticeId, noticeVersionId: versionId, etag: edited.etag }),
    );
    expect(published.status).toBe('active');

    const photos = result.current.purposes.data?.items.find((p) => p.code === 'photo_video_publication');
    const on = await act(() =>
      result.current.updatePurpose.mutateAsync({
        purposeId: photos?.id ?? '',
        etag: photos?.etag ?? '',
        changes: { enabled: true, noticeId, maxScope: 'institution' },
      }),
    );
    expect(on).toMatchObject({ enabled: true, noticeId, maxScope: 'institution' });
    expect(lastPatch()?.headers.get('If-Match')).toBe('"0"');

    // removing wording, and a notice nothing uses
    await act(() =>
      result.current.deleteTranslation.mutateAsync({
        noticeId,
        noticeVersionId: versionId,
        noticeTranslationId: wording.id ?? '',
        etag: '"2"',
      }),
    );
    const spare = await act(() => result.current.createNotice.mutateAsync({ code: 'spare', title: 'Spare' }));
    await act(() =>
      result.current.deleteNotice.mutateAsync({ noticeId: spare.id ?? '', etag: spare.etag ?? '' }),
    );
    expect(api.calls.filter((c) => c.method === 'DELETE').map((c) => c.path)).toEqual([
      `/v1/privacy/notices/${noticeId}/versions/${versionId}/translations/${wording.id ?? ''}`,
      `/v1/privacy/notices/${spare.id ?? ''}`,
    ]);
  });
});
