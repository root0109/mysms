import { useMutation } from '@tanstack/react-query';
import type { LoginRequest } from '../auth/session';
import { useRuntime } from '../react/useRuntime';

/** ACC-01 sign-in: resolves to 'signedIn' or 'mfaRequired'. */
export function useSignIn() {
  const { session } = useRuntime();
  return useMutation({ mutationFn: (body: LoginRequest) => session.signIn(body) });
}

export function useCompleteMfa() {
  const { session } = useRuntime();
  return useMutation({ mutationFn: (code: string) => session.completeMfa(code) });
}

export function useSignOut() {
  const { session } = useRuntime();
  return useMutation({ mutationFn: () => session.signOut() });
}
