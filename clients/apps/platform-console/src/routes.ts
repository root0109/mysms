import {
  buildAppRoutes,
  HomeScreen,
  ProfileScreen,
  ScreenLoading,
  SettingsScreen,
  SignInScreen,
} from '@mysms/web-kit';
import { createElement, lazy, Suspense, type ComponentType } from 'react';
import { appConfig } from './config';
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

/** C: the platform console's screens of the inventory; built ones listed here, the rest show their placeholder. */
export const routes = buildAppRoutes({
  client: 'C',
  Shell: ConsoleShell,
  Home: HomeScreen,
  built: {
    'ACC-01': createElement(SignInScreen, { defaultTenantCode: appConfig.tenantCode }),
    'ACC-07': createElement(ProfileScreen),
    'ACC-14': createElement(SettingsScreen),
    'PLT-06': lazyScreen(JobCentreScreen),
    'PLT-07 /master/countries': lazyScreen(CountriesScreen),
  },
});
