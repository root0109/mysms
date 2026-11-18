import type { ResourceScreenDef } from '@mysms/web-kit';

/**
 * The console screens (ui/02 §3, PLT-01…05) over the global record types platform staff manage: tenants, SaaS
 * plans, sales enquiries, reserved keywords, notification types and system settings. Each is the shared list
 * manager (04 §5) over one record type of `api/standard-resources.json` with scope `global`; the server allows the
 * writes to platform operators only. The tenant-scoped parts of PLT-02 (subscriptions) and PLT-04 (notification
 * channels) belong to a tenant and are not managed from here.
 *
 * `route` is set where the inventory screen has several routes and names the one this record type answers.
 */
export interface ConsoleScreenDef extends ResourceScreenDef {
  route?: string;
}

export const CONSOLE_SCREENS: readonly ConsoleScreenDef[] = [
  {
    id: 'PLT-01',
    resource: 'platform.Tenant',
    group: 'setup',
    columns: ['code', 'name', 'status', 'primaryEmail'],
    tool: 'clearDemoData',
  },
  {
    id: 'PLT-02',
    resource: 'platform.SaasPlan',
    group: 'setup',
    columns: ['code', 'displayName', 'billingCycleMonths', 'price', 'isActive'],
  },
  {
    id: 'PLT-03',
    resource: 'platform.SalesEnquiry',
    group: 'setup',
    columns: ['contactName', 'organisationName', 'email', 'status'],
  },
  {
    id: 'PLT-04',
    route: '/comms/notification-types',
    resource: 'comms.NotificationType',
    group: 'setup',
    columns: ['code', 'moduleCode', 'roleCode', 'isEnabled'],
  },
  {
    id: 'PLT-04',
    route: '/platform/reserved-keywords',
    resource: 'platform.ReservedKeyword',
    group: 'setup',
    columns: ['keyword'],
  },
  {
    id: 'PLT-05',
    resource: 'platform.SystemSetting',
    group: 'setup',
    columns: ['settingKey', 'name', 'value', 'isActive'],
  },
];

/** The `built` key of `buildAppRoutes`: the inventory id, plus the route for a screen with several. */
export const builtKey = (screen: ConsoleScreenDef) =>
  screen.route ? `${screen.id} ${screen.route}` : screen.id;
