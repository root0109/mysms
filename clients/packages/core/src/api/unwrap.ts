import { FetchError, ResponseError, type ApiResponse } from '@mysms/sdk/runtime';
import { ApiError } from './problem';

/**
 * Normalises what a generated client throws:
 * - a non-2xx response (`ResponseError`) becomes an ApiError built from its problem+json body;
 * - a network failure (`FetchError`) is rethrown as its cause (the `TypeError` fetch rejects with), which is
 *   what the retry policy recognises (api/retry.ts);
 * - anything else is rethrown unchanged.
 */
export async function toApiError(error: unknown): Promise<unknown> {
  if (error instanceof ResponseError) {
    const body: unknown = await error.response
      .clone()
      .json()
      .catch(() => undefined);
    return ApiError.fromResponse(error.response, body);
  }
  if (error instanceof FetchError) return error.cause instanceof Error ? error.cause : error;
  return error;
}

/** Awaits a generated client call; failures surface as ApiError (ui/05 §6). */
export async function call<T>(promise: Promise<T>): Promise<T> {
  try {
    return await promise;
  } catch (error) {
    throw await toApiError(error);
  }
}

/** A row together with its ETag (row version) for a later If-Match (ui/05 §6). */
export interface WithEtag<T> {
  data: T;
  etag: string | null;
}

/** Awaits a generated `…Raw` call and keeps the ETag response header next to the body. */
export async function callWithEtag<T>(promise: Promise<ApiResponse<T>>): Promise<WithEtag<T>> {
  const response = await call(promise);
  return { data: await call(response.value()), etag: response.raw.headers.get('ETag') };
}
