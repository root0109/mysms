import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import type { Runtime } from '../runtime';
import { RuntimeContext } from './runtimeContext';

/** Wraps an app: provides the runtime and its query client. */
export function MySmsProvider({ runtime, children }: { runtime: Runtime; children: ReactNode }) {
  return (
    <RuntimeContext.Provider value={runtime}>
      <QueryClientProvider client={runtime.queryClient}>{children}</QueryClientProvider>
    </RuntimeContext.Provider>
  );
}
