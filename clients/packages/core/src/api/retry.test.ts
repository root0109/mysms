import { describe, expect, it } from 'vitest';
import { ApiError } from './problem';
import { MAX_QUERY_RETRIES, isRetryableError, retryDelay, shouldRetryQuery } from './retry';

const err = (status: number, retryAfterSeconds?: number) =>
  new ApiError({ status, code: 'X', title: 'X', retryAfterSeconds });

describe('retry policy (ui/05 §6)', () => {
  it('retries network failures, 5xx (not 501) and 429 only', () => {
    expect(isRetryableError(new TypeError('Failed to fetch'))).toBe(true);
    expect(isRetryableError(err(500))).toBe(true);
    expect(isRetryableError(err(503))).toBe(true);
    expect(isRetryableError(err(429))).toBe(true);
    expect(isRetryableError(err(501))).toBe(false);
    expect(isRetryableError(err(404))).toBe(false);
    expect(isRetryableError(err(412))).toBe(false);
    expect(isRetryableError(new Error('other'))).toBe(false);
  });

  it('stops after three attempts', () => {
    expect(shouldRetryQuery(0, err(503))).toBe(true);
    expect(shouldRetryQuery(MAX_QUERY_RETRIES - 1, err(503))).toBe(true);
    expect(shouldRetryQuery(MAX_QUERY_RETRIES, err(503))).toBe(false);
    expect(shouldRetryQuery(0, err(400))).toBe(false);
  });

  it('backs off exponentially and honours Retry-After (capped)', () => {
    expect(retryDelay(0, err(503))).toBe(500);
    expect(retryDelay(2, err(503))).toBe(2000);
    expect(retryDelay(20, err(503))).toBe(30_000);
    expect(retryDelay(0, err(429, 3))).toBe(3000);
    expect(retryDelay(0, err(429, 600))).toBe(60_000);
  });
});
