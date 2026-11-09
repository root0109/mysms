import type { ReactNode } from 'react';
import type { PermissionCode } from '../generated/permissions';
import { useCan } from '../hooks/session';

/**
 * Renders children only when the signed-in user holds the permission (ui/04 §5). It mirrors security for
 * a clean UI; the server remains the authority on every call.
 */
export function PermissionGate({
  code,
  fallback = null,
  children,
}: {
  code: PermissionCode;
  fallback?: ReactNode;
  children: ReactNode;
}) {
  return useCan(code) ? children : fallback;
}
