import { describe, expect, it } from 'vitest';
import { ApiError, isApiError, parseRetryAfter } from './problem';

describe('ApiError.fromResponse', () => {
  it('reads the RFC 9457 body', () => {
    const res = new Response(null, { status: 422, statusText: 'Unprocessable' });
    const e = ApiError.fromResponse(res, {
      title: 'Validation failed',
      status: 422,
      code: 'VALIDATION',
      detail: 'Two fields are wrong',
      traceId: 't-1',
      errors: [
        { field: 'iso2', message: 'too long', code: 'MAX' },
        { field: 'iso2', message: 'second message ignored' },
        { field: 'name', message: 'required' },
        { nonsense: true },
      ],
    });
    expect(e).toBeInstanceOf(ApiError);
    expect(isApiError(e)).toBe(true);
    expect(e.status).toBe(422);
    expect(e.code).toBe('VALIDATION');
    expect(e.message).toBe('Validation failed: Two fields are wrong');
    expect(e.traceId).toBe('t-1');
    expect(e.errors).toHaveLength(3);
    expect(e.fieldErrors()).toEqual({ iso2: 'too long', name: 'required' });
  });

  it('falls back to the HTTP status when the body is not a problem', () => {
    const res = new Response(null, {
      status: 503,
      statusText: 'Service Unavailable',
      headers: { 'X-Trace-Id': 'hdr', 'Retry-After': '7' },
    });
    const e = ApiError.fromResponse(res, 'oops');
    expect(e.code).toBe('HTTP_503');
    expect(e.title).toBe('Service Unavailable');
    expect(e.message).toBe('Service Unavailable');
    expect(e.traceId).toBe('hdr');
    expect(e.retryAfterSeconds).toBe(7);
    expect(e.errors).toEqual([]);
  });

  it('uses a generic title when there is no status text', () => {
    expect(ApiError.fromResponse(new Response(null, { status: 500 }), undefined).title).toBe('HTTP 500');
  });

  it('classifies conflicts and not-available', () => {
    const mk = (status: number) => new ApiError({ status, code: 'X', title: 'X' });
    expect(mk(412).isConflict).toBe(true);
    expect(mk(409).isConflict).toBe(true);
    expect(mk(400).isConflict).toBe(false);
    expect(mk(403).isNotAvailable).toBe(true);
    expect(mk(404).isNotAvailable).toBe(true);
    expect(mk(401).isNotAvailable).toBe(false);
    expect(isApiError(new Error('x'))).toBe(false);
  });
});

describe('parseRetryAfter', () => {
  it('parses seconds and HTTP dates', () => {
    expect(parseRetryAfter(null)).toBeUndefined();
    expect(parseRetryAfter('12')).toBe(12);
    const now = Date.parse('2026-11-06T10:00:00Z');
    expect(parseRetryAfter('Fri, 06 Nov 2026 10:00:30 GMT', now)).toBe(30);
    expect(parseRetryAfter('Fri, 06 Nov 2026 09:00:00 GMT', now)).toBe(0);
    expect(parseRetryAfter('soon')).toBeUndefined();
  });
});
