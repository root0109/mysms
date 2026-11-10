import {
  AttendanceSheetScreen,
  AttendanceSummaryScreen,
  MyTimetableScreen,
  StaffLeaveScreen,
  buildAppRoutes,
  HomeworkCheckScreen,
  HomeScreen,
  NotificationPreferencesScreen,
  ProfileScreen,
  RecordsScreen,
  RESOURCE_SCREENS,
  ResourceScreen,
  SecurityScreen,
  SettingsScreen,
  SettingValuesScreen,
  SignInScreen,
  StudentAttendanceScreen,
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
    'ACC-10': createElement(NotificationPreferencesScreen),
    'ACC-14': createElement(SettingsScreen),
    'ASM-03': createElement(HomeworkCheckScreen),
    'ATT-03': createElement(AttendanceSheetScreen),
    'ATT-04': createElement(StudentAttendanceScreen),
    'ATT-05': createElement(AttendanceSummaryScreen),
    'TT-03': createElement(MyTimetableScreen),
    'HR-04': createElement(StaffLeaveScreen),
    'ADM-11': createElement(RecordsScreen),
    'ADM-16': createElement(SettingValuesScreen),
  },
});
