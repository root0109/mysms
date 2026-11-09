import { NavShell, type NavItem } from '@mysms/web-kit';

const NAV: readonly NavItem[] = [
  { to: '/app', label: 'shell.nav.home', end: true },
  { to: '/app/master/countries', label: 'shell.nav.masterData' },
  { to: '/app/platform/operations', label: 'shell.nav.jobs' },
  { to: '/app/people/me', label: 'shell.nav.profile' },
  { to: '/app/iam/me/settings', label: 'shell.nav.settings' },
];

/** The W shell with the red platform band and no institution switcher (ui/02 §1.2). */
export function ConsoleShell() {
  return <NavShell items={NAV} environmentBand="shell.env.platform" />;
}
