import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakePrivacy, json, privacyFixture, problem, type Handler } from '@mysms/fixtures';
import { ApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  consentActions,
  consentFailureKey,
  consentSubjects,
  refusalScope,
  useMyConsents,
  useMyNotice,
  useRecordMyConsent,
  useWithdrawMyConsent,
  type ConsentAnswer,
  type MyConsent,
} from './privacy';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: 'u1', username: 'meena', locale: 'en-IN', personId: privacyFixture.parentPersonId };
const CONSENTS = '/v1/privacy/my/consents';
const f = privacyFixture;

async function setup(extra: Record<string, Handler> = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    ...fakePrivacy(),
    ...extra,
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'meena', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

const row = (more: Partial<MyConsent>): MyConsent => ({
  subjectPersonId: 'p1',
  purposeCode: f.photos,
  capacity: 'guardian',
  allowedScopes: ['class', 'institution'],
  ...more,
});

describe('consent centre (ACC-13)', () => {
  it('lists the rows, reads the notice in a language, records an answer and withdraws it', async () => {
    const { api, wrapper } = await setup();
    const { result, rerender } = renderHook(
      ({ code }: { code: string | null }) => ({
        consents: useMyConsents(),
        notice: useMyNotice(code, 'hi'),
        record: useRecordMyConsent(),
        withdraw: useWithdrawMyConsent(),
      }),
      { wrapper, initialProps: { code: null as string | null } },
    );
    await waitFor(() => expect(result.current.consents.data?.data).toHaveLength(6));
    // no notice is asked for until one is opened
    expect(result.current.notice.fetchStatus).toBe('idle');
    rerender({ code: f.noticeCode });
    await waitFor(() => expect(result.current.notice.data?.language).toBe('hi'));

    const notice = result.current.notice.data;
    if (!notice) throw new Error('no notice');
    const saved = await act(() =>
      result.current.record.mutateAsync({
        subjectPersonId: f.kabirPersonId,
        purposeCode: f.photos,
        scope: 'class',
        decision: 'granted',
        notice,
      }),
    );
    expect(saved).toMatchObject({ myStatus: 'granted', effectiveWidestScope: 'class', etag: '"1"' });
    const posted = api.calls.find((c) => c.method === 'POST' && c.path === CONSENTS);
    expect(JSON.parse(posted?.body ?? '{}')).toMatchObject({
      noticeVersionId: f.noticeVersionId,
      noticeLanguage: 'hi',
    });
    // the centre is read again after a change
    await waitFor(() =>
      expect(
        result.current.consents.data?.data.find(
          (r) => r.subjectPersonId === f.kabirPersonId && r.purposeCode === f.photos,
        )?.myStatus,
      ).toBe('granted'),
    );

    const withdrawn = await act(() =>
      result.current.withdraw.mutateAsync({ consentRecordId: saved.id ?? '', etag: saved.etag }),
    );
    expect(withdrawn.myStatus).toBe('not_answered');
    expect(JSON.parse(api.calls.find((c) => c.path.endsWith(':withdraw'))?.body ?? '{}')).toEqual({
      etag: '"1"',
    });
  });

  it('keeps the Idempotency-Key for a retry and draws a new one after an answer the server kept', async () => {
    let failures = 2;
    const consents = fakePrivacy();
    const { api, wrapper } = await setup({
      [`POST ${CONSENTS}`]: (req, params) => {
        if (failures-- > 0) return problem(503, 'UNAVAILABLE');
        const handler = consents[`POST ${CONSENTS}`];
        if (!handler) throw new Error('no handler');
        return handler(req, params);
      },
    });
    const { result } = renderHook(() => useRecordMyConsent(), { wrapper });
    const answer: ConsentAnswer = {
      subjectPersonId: f.ashaPersonId,
      purposeCode: f.marketing,
      scope: 'tenant',
      decision: 'refused',
      notice: { noticeVersionId: f.noticeVersionId, language: 'en' },
    };
    const attempt = async (more: Partial<ConsentAnswer> = {}) => {
      await act(async () => {
        await result.current.mutateAsync({ ...answer, ...more }).catch(() => undefined);
      });
    };
    await attempt(); // 503: may be retried as the same request
    await attempt({ decision: 'granted' }); // another answer instead: a new request, 503 again
    await attempt({ decision: 'granted' }); // its retry, recorded
    await attempt({ scope: 'class' }); // a new decision; refused with 422, which the server keeps
    await attempt({ scope: 'class' }); // sent again unchanged: still a new request
    const keys = api.calls
      .filter((c) => c.method === 'POST' && c.path === CONSENTS)
      .map((c) => c.headers.get('Idempotency-Key'));
    expect(keys).toHaveLength(5);
    expect(keys[1]).not.toBe(keys[0]);
    expect(keys[2]).toBe(keys[1]);
    expect(keys[3]).not.toBe(keys[2]);
    expect(keys[4]).not.toBe(keys[3]);
  });

  it('groups rows by person in the server order', () => {
    const groups = consentSubjects([
      row({ subjectPersonId: 'me', capacity: 'self' }),
      row({ subjectPersonId: 'child', purposeCode: 'a' }),
      row({ subjectPersonId: 'me', capacity: 'self', purposeCode: 'b' }),
    ]);
    expect(groups.map((g) => [g.subjectPersonId, g.capacity, g.rows.length])).toEqual([
      ['me', 'self', 2],
      ['child', 'guardian', 1],
    ]);
  });

  it('decides only under a published notice, withdraws only a live answer, and never for a minor', () => {
    const published = { noticeCode: 'n', noticeVersionId: 'v' };
    expect(consentActions(row(published))).toEqual({ decide: true, withdraw: false });
    expect(consentActions(row({}))).toEqual({ decide: false, withdraw: false });
    // a ceiling below every width leaves nothing to decide
    expect(consentActions(row({ ...published, allowedScopes: [] })).decide).toBe(false);
    expect(consentActions(row({ ...published, id: 'r', myStatus: 'granted' }))).toEqual({
      decide: true,
      withdraw: true,
    });
    expect(consentActions(row({ ...published, id: 'r', myStatus: 'expired' })).withdraw).toBe(false);
    expect(consentActions(row({ ...published, capacity: 'minor_self' }))).toEqual({
      decide: false,
      withdraw: false,
    });
  });

  it('refuses at the narrowest width and maps failures', () => {
    expect(refusalScope(row({}))).toBe('class');
    expect(refusalScope(row({ allowedScopes: [] }))).toBeUndefined();
    const failure = (status: number) => new ApiError({ status, code: 'X', title: 'X' });
    expect(consentFailureKey(failure(412))).toBe('common.conflict.body');
    expect(consentFailureKey(failure(422))).toBe('privacy.consentCentre.error.changed');
    expect(consentFailureKey(failure(404))).toBe('privacy.consentCentre.error.changed');
    expect(consentFailureKey(failure(403))).toBe('privacy.consentCentre.error.notAllowed');
    expect(consentFailureKey(failure(500))).toBe('common.error.generic');
    expect(consentFailureKey(new Error('network'))).toBe('common.error.generic');
  });
});
