import { RECORD_PERMISSIONS, RESOURCES } from '@mysms/core';
import {
  NAV_GROUPS,
  NavShell,
  RESOURCE_SCREENS,
  screenTitleKey,
  type NavGroup,
  type NavItem,
} from '@mysms/web-kit';
import { SCREENS } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';

const NAV: readonly NavItem[] = [
  { to: '/app', label: 'shell.nav.home', end: true },
  { to: '/app/people/me', label: 'shell.nav.profile' },
  { to: '/app/comms/in-app-notifications', label: 'shell.nav.inbox' },
  { to: '/app/iam/me/settings', label: 'shell.nav.settings' },
  { to: '/app/iam/me/security', label: 'shell.nav.security' },
];

/** Setup ends with the generic records manager and the settings by scope (ui/02 §2.1, Setup → Data, Access). */
const SETUP_EXTRA: readonly NavItem[] = [
  { to: '/app/admin/records', label: 'shell.nav.records', anyPermission: RECORD_PERMISSIONS },
  { to: '/app/iam/setting-values', label: 'shell.nav.settingValues', permission: 'iam.setting_value.read' },
];

/** The route of an inventory screen (its first web route, query variants dropped), under /app. */
const routeOf = (id: string) => `/app${(SCREENS.find((s) => s.id === id)?.routes[0] ?? '').split('?')[0]}`;

/** Navigation groups of ui/02 §2.1: each list-manager screen appears when the role may list its record type. */
const GROUPS: readonly NavGroup[] = NAV_GROUPS.map((group) => ({
  label: `shell.nav.group.${group}` as MessageKey,
  items: [
    ...RESOURCE_SCREENS.filter((screen) => screen.group === group).map((screen) => ({
      to: routeOf(screen.id),
      label: screenTitleKey(screen.id),
      anyPermission: [RESOURCES.find((r) => r.key === screen.resource)?.ops.List?.permission ?? 'none'],
    })),
    ...(group === 'setup' ? SETUP_EXTRA : []),
  ],
}));

export function WebShell() {
  return <NavShell items={NAV} groups={GROUPS} />;
}
