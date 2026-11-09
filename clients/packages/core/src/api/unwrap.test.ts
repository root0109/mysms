import { FetchError, ResponseError } from '@mysms/sdk';
import { describe, expect, it } from 'vitest';
import { problem } from '@mysms/fixtures';
import { call, callWithEtag, toApiError } from './unwrap';

const raw = (body: unknown, headers: Record<string, string> = {}) => ({
  raw: new Response(JSON.stringify(body), { headers }),
  value: async () => body,
});

describe('call / callWithEtag', () => {
  it('returns data and the ETag of successful calls', async () => {
    await expect(call(Promise.resolve({ a: 1 }))).resolves.toEqual({ a: 1 });
    await expect(callWithEtag(Promise.resolve(raw({ a: 1 }, { ETag: '"3"' })))).resolves.toEqual({
      data: { a: 1 },
      etag: '"3"',
    });
    await expect(callWithEtag(Promise.resolve(raw({ a: 1 })))).resolves.toEqual({
      data: { a: 1 },
      etag: null,
    });
  });

  it('turns a non-2xx response into an ApiError from its problem body', async () => {
    const failing = () => Promise.reject(new ResponseError(problem(412, 'STALE', 'Stale')));
    await expect(call(failing())).rejects.toMatchObject({ status: 412, code: 'STALE', title: 'Stale' });
    await expect(callWithEtag(failing())).rejects.toMatchObject({ isConflict: true });
  });

  it('copes with a non-JSON error body', async () => {
    const error = await toApiError(new ResponseError(new Response('oops', { status: 502 })));
    expect(error).toMatchObject({ status: 502, code: 'HTTP_502' });
  });

  it('unwraps network failures to their cause and passes other errors through', async () => {
    const cause = new TypeError('Failed to fetch');
    await expect(toApiError(new FetchError(cause))).resolves.toBe(cause);
    const other = new Error('x');
    await expect(toApiError(other)).resolves.toBe(other);
  });
});
