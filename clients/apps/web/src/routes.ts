import {
  buildAppRoutes,
  HomeScreen,
  ProfileScreen,
  RecordsScreen,
  RESOURCE_SCREENS,
  ResourceScreen,
  SecurityScreen,
  SettingsScreen,
  SettingValuesScreen,
  SignInScreen,
} from '@mysms/web-kit';
import { createElement, type ReactNode } from 'react';
import { appConfig } from './config';
import { WebShell } from './shell/WebShell';

/** The list-manager screens of the inventory, one per record type (web-kit `RESOURCE_SCREENS`). */
const resourceScreens: Record<string, ReactNode> = Object.fromEntries(
  RESOURCE_SCREENS.map((screen) => [screen.id, createElement(ResourceScreen, { screen, key: screen.id })]),
);

/** W: every web screen of the inventory; built ones listed here, the rest show their placeholder. */
export const routes = buildAppRoutes({
  client: 'W',
  Shell: WebShell,
  Home: HomeScreen,
  built: {
    ...resourceScreens,
    'ACC-01': createElement(SignInScreen, { defaultTenantCode: appConfig.tenantCode }),
    'ACC-07': createElement(ProfileScreen),
    'ACC-08': createElement(SecurityScreen),
    'ACC-14': createElement(SettingsScreen),
    'ADM-11': createElement(RecordsScreen),
    'ADM-16': createElement(SettingValuesScreen),
  },
});
