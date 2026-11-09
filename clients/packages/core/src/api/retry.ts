import { isApiError } from './problem';

/** Automatic retries apply to idempotent reads only, at most three times (ui/05 §6). */
export const MAX_QUERY_RETRIES = 3;
const BASE_DELAY_MS = 500;
const MAX_DELAY_MS = 30_000;
const MAX_RETRY_AFTER_MS = 60_000;

/** A network failure (fetch rejects with TypeError) or a server/429 response is worth retrying. */
export function isRetryableError(error: unknown): boolean {
  if (isApiError(error)) return error.status === 429 || (error.status >= 500 && error.status !== 501);
  return error instanceof TypeError;
}

export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  return failureCount < MAX_QUERY_RETRIES && isRetryableError(error);
}

/** Exponential back-off; a server-provided Retry-After wins (capped at one minute). */
export function retryDelay(attemptIndex: number, error: unknown): number {
  if (isApiError(error) && error.retryAfterSeconds !== undefined) {
    return Math.min(error.retryAfterSeconds * 1000, MAX_RETRY_AFTER_MS);
  }
  return Math.min(BASE_DELAY_MS * 2 ** attemptIndex, MAX_DELAY_MS);
}
