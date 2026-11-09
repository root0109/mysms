import { useSession } from '@mysms/core';
import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';

/** Route guard: anything under it needs a signed-in session; otherwise go to sign-in and come back after. */
export function RequireSession({ children }: { children: ReactNode }) {
  const status = useSession((s) => s.status);
  const location = useLocation();
  if (status === 'signedIn') return children;
  const next = encodeURIComponent(location.pathname + location.search);
  return <Navigate to={`/app/auth/login?next=${next}`} replace state={{ expired: status === 'expired' }} />;
}
