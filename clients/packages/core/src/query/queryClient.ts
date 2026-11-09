import { QueryClient } from '@tanstack/react-query';
import { retryDelay, shouldRetryQuery } from '../api/retry';

/** Freshness defaults from ui/05 §4. */
export const STALE_TIME = {
  /** Reference data (countries, currencies, pick-lists). */
  reference: 10 * 60_000,
  /** Lists. */
  list: 30_000,
  /** Detail views refetch in the background on every mount. */
  detail: 0,
} as const;

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: STALE_TIME.list,
        retry: shouldRetryQuery,
        retryDelay,
        refetchOnWindowFocus: false,
      },
      // Writes are never retried automatically; they retry only through their Idempotency-Key path (ui/05 §6).
      mutations: { retry: false },
    },
  });
}
