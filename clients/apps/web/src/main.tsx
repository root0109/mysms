import '@mysms/tokens/tokens.css';
import '@mysms/ui-web/styles.css';
import '@mysms/web-kit/kit.css';
import { createRuntime, memoryVault } from '@mysms/core';
import { WebApp } from '@mysms/web-kit';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter } from 'react-router';
import { appConfig } from './config';
import { routes } from './routes';

// The refresh token stays in memory on the web (ui/05 §3); an httpOnly-cookie refresh flow can replace the vault.
const runtime = createRuntime({
  baseUrl: appConfig.apiBaseUrl,
  clientId: appConfig.clientId,
  defaultLocale: appConfig.defaultLocale,
  vault: memoryVault(),
  tenantCode: appConfig.tenantCode,
});

const root = document.getElementById('root');
if (!root) throw new Error('#root missing from index.html');
createRoot(root).render(
  <StrictMode>
    <WebApp runtime={runtime} router={createBrowserRouter(routes)} />
  </StrictMode>,
);
