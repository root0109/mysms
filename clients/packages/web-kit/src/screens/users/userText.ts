import type { UserAccountStatus, UserType } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import type { StatusChipProps } from '@mysms/ui-web';

type Intent = StatusChipProps['intent'];

/** How an account status reads and looks (api/state-machines.md "iam.UserAccount"). */
export const USER_STATUS: Readonly<Record<UserAccountStatus, { label: MessageKey; intent: Intent }>> = {
  active: { label: 'adm.users.status.active', intent: 'success' },
  inactive: { label: 'adm.users.status.inactive', intent: 'neutral' },
  locked: { label: 'adm.users.status.locked', intent: 'danger' },
  exited: { label: 'adm.users.status.exited', intent: 'neutral' },
};

export const USER_TYPE: Readonly<Record<UserType, MessageKey>> = {
  admin: 'adm.users.type.admin',
  teacher: 'adm.users.type.teacher',
  student: 'adm.users.type.student',
  guardian: 'adm.users.type.guardian',
  staff: 'adm.users.type.staff',
  librarian: 'adm.users.type.librarian',
  accountant: 'adm.users.type.accountant',
  alumni: 'adm.users.type.alumni',
};

/** The page of one account (ADM-09). */
export const userLink = (id: string) => `/app/iam/user-accounts/${id}`;
