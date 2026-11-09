import { useStore } from 'zustand';
import type { SessionState } from '../auth/session';
import { can } from '../permissions/can';
import type { PermissionCode } from '../generated/permissions';
import { useRuntime } from '../react/useRuntime';

/** Subscribes to part of the session state, e.g. `useSession((s) => s.status)`. */
export function useSession<T>(selector: (state: SessionState) => T): T {
  return useStore(useRuntime().session.store, selector);
}

/** `true` when the signed-in user holds the permission. The UI mirrors security; the server enforces it. */
export function useCan(code: PermissionCode): boolean {
  return useSession((s) => can(s.me?.permissions, code));
}
