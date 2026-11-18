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
  { to: '/app/iam/user-accounts', label: 'screen.ADM08.title', permission: 'iam.user_account.read' },
  { to: '/app/admin/records', label: 'shell.nav.records', anyPermission: RECORD_PERMISSIONS },
  { to: '/app/iam/setting-values', label: 'shell.nav.settingValues', permission: 'iam.setting_value.read' },
];

/** Students & staff also carries leave, which staff and approvers use as one screen and not as a list of records. */
const PEOPLE_EXTRA: readonly NavItem[] = [
  {
    to: '/app/hr/leave-applications',
    label: 'screen.HR04.title',
    anyPermission: [
      'hr.leave_application.create',
      'hr.leave_application.approve_leave',
      'hr.leave_application.reject_leave',
    ],
  },
];

/** Academics also carries the attendance summary, which is a report and not a list of one record type. */
const ACADEMICS_EXTRA: readonly NavItem[] = [
  { to: '/app/timetable/my', label: 'screen.TT03.title', permission: 'timetable.session.get_timetable' },
  {
    to: '/app/attendance/summary',
    label: 'screen.ATT05.title',
    // the screen works on one institution of the caller's, so it needs both
    permission: 'attendance.attendance_record.summarize_attendance',
    anyPermission: ['org.institution.read'],
  },
];

/** Money also carries payroll's screens, which are workflows and "my …" views rather than lists of one record type. */
const MONEY_EXTRA: readonly NavItem[] = [
  { to: '/app/payroll/staff-salaries', label: 'screen.HR06.title', permission: 'payroll.staff_salary.read' },
  {
    to: '/app/payroll/payslips',
    label: 'screen.HR07.title',
    anyPermission: ['payroll.payslip.generate_payslips', 'payroll.payslip.mark_payslip_paid'],
  },
  { to: '/app/payroll/payslips?mine', label: 'screen.HR08.title', permission: 'payroll.payslip.read' },
  {
    to: '/app/payroll/payroll-payments',
    label: 'screen.HR09.title',
    anyPermission: ['payroll.payroll_payment.read', 'payroll.disbursement_account.read'],
  },
  {
    to: '/app/finance/student-fee-dues',
    label: 'screen.FIN03.title',
    permission: 'finance.student_fee_due.read',
  },
  {
    to: '/app/finance/student-fee-dues:generate',
    label: 'screen.FIN02.title',
    permission: 'finance.student_fee_due.generate_fee_dues',
  },
  { to: '/app/finance/invoices', label: 'screen.FIN04.title', permission: 'finance.invoice.read' },
  {
    to: '/app/finance/payments',
    label: 'screen.FIN06.title',
    anyPermission: ['finance.payment.read', 'finance.payment.record'],
  },
  {
    to: '/app/finance/reminders',
    label: 'screen.FIN10.title',
    permission: 'finance.invoice.send_fee_reminders',
  },
  { to: '/app/finance/pay', label: 'screen.FIN07.title', permission: 'finance.payment.start_checkout' },
];

/** Setup → Catalogues carries the salary templates (HR-05). */
const SETUP_PAYROLL: readonly NavItem[] = [
  { to: '/app/finance/fee-heads', label: 'screen.FIN01.title', permission: 'finance.fee_head.read' },
  {
    to: '/app/payroll/salary-templates',
    label: 'screen.HR05.title',
    permission: 'payroll.salary_template.read',
  },
];

/** The route of an inventory screen (its first web route, with the query that tells it from a sibling), under /app. */
const routeOf = (id: string) => `/app${SCREENS.find((s) => s.id === id)?.routes[0] ?? ''}`;

/** Navigation groups of ui/02 §2.1: each list-manager screen appears when the role may list its record type. */
const GROUPS: readonly NavGroup[] = NAV_GROUPS.map((group) => ({
  label: `shell.nav.group.${group}` as MessageKey,
  items: [
    ...RESOURCE_SCREENS.filter((screen) => screen.group === group).map((screen) => ({
      to: routeOf(screen.id),
      label: screenTitleKey(screen.id),
      anyPermission: [RESOURCES.find((r) => r.key === screen.resource)?.ops.List?.permission ?? 'none'],
      ...(screen.navPermission ? { permission: screen.navPermission } : {}),
    })),
    ...(group === 'people' ? PEOPLE_EXTRA : []),
    ...(group === 'academics' ? ACADEMICS_EXTRA : []),
    ...(group === 'money' ? MONEY_EXTRA : []),
    ...(group === 'setup' ? [...SETUP_PAYROLL, ...SETUP_EXTRA] : []),
  ],
}));

export function WebShell() {
  return <NavShell items={NAV} groups={GROUPS} termsGate />;
}
