import {
  buildAppRoutes,
  HomeScreen,
  ProfileScreen,
  ResourceScreen,
  ScreenLoading,
  SettingsScreen,
  SignInScreen,
} from '@mysms/web-kit';
import { createElement, lazy, Suspense, type ComponentType, type ReactNode } from 'react';
import { appConfig } from './config';
import { builtKey, CONSOLE_SCREENS } from './screens/globalScreens';
import { ConsoleShell } from './shell/ConsoleShell';

// built screens load on demand: one chunk per screen keeps the initial route small (ui/05 §10)
const CountriesScreen = lazy(() =>
  import('./screens/CountriesScreen').then((m) => ({ default: m.CountriesScreen })),
);
const JobCentreScreen = lazy(() =>
  import('./screens/JobCentreScreen').then((m) => ({ default: m.JobCentreScreen })),
);
const lazyScreen = (Screen: ComponentType) =>
  createElement(Suspense, { fallback: createElement(ScreenLoading) }, createElement(Screen));

/** The list managers over the global record types (PLT-01…05). */
const globalScreens: Record<string, ReactNode> = Object.fromEntries(
  CONSOLE_SCREENS.map((screen) => [
    builtKey(screen),
    createElement(ResourceScreen, { screen, key: builtKey(screen) }),
  ]),
);

/** C: the platform console's screens of the inventory; built ones listed here, the rest show their placeholder. */
export const routes = buildAppRoutes({
  client: 'C',
  Shell: ConsoleShell,
  Home: HomeScreen,
  built: {
    ...globalScreens,
    'ACC-01': createElement(SignInScreen, { defaultTenantCode: appConfig.tenantCode }),
    'ACC-07': createElement(ProfileScreen),
    'ACC-14': createElement(SettingsScreen),
    'PLT-06': lazyScreen(JobCentreScreen),
    'PLT-07 /master/countries': lazyScreen(CountriesScreen),
  },
});
