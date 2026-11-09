import { MySmsProvider, type Runtime } from '@mysms/core';
import { RouterProvider, type createBrowserRouter } from 'react-router';
import { BootGate } from './routing/BootGate';
import { PreferencesApplier } from './settings/PreferencesApplier';

export type WebRouter = ReturnType<typeof createBrowserRouter>;

/** Root of a web client: runtime + query cache, the user's interface settings, session restore, then the router. */
export function WebApp({ runtime, router }: { runtime: Runtime; router: WebRouter }) {
  return (
    <MySmsProvider runtime={runtime}>
      <PreferencesApplier />
      <BootGate>
        <RouterProvider router={router} />
      </BootGate>
    </MySmsProvider>
  );
}
