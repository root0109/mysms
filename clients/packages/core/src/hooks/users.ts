import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query';
import type { iam } from '@mysms/sdk';
import { RoleApi } from '@mysms/sdk/iam/apis/RoleApi';
import { UserAccountApi } from '@mysms/sdk/iam/apis/UserAccountApi';
import { FIRST_PAGE, allPages, nextPage, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type UserAccount = iam.UserAccount;
export type UserAccountStatus = NonNullable<UserAccount['status']>;
export type UserType = NonNullable<UserAccount['userType']>;
export type UserRole = iam.UserRole;
export type Role = iam.Role;
export type RoleGrant = iam.RoleGrant;
export type RoleScopeType = RoleGrant['scopeType'];

export const USER_ACCOUNT_STATUSES: readonly UserAccountStatus[] = ['active', 'inactive', 'locked', 'exited'];
export const USER_TYPES: readonly UserType[] = [
  'admin',
  'teacher',
  'student',
  'guardian',
  'staff',
  'librarian',
  'accountant',
  'alumni',
];
export const ROLE_SCOPE_TYPES: readonly RoleScopeType[] = [
  'tenant',
  'institution',
  'department',
  'class_section',
  'course',
  'lms_course',
  'lms_cohort',
];

/** The status steps of api/state-machines.md "iam.UserAccount" an administrator can take from a status. */
export function userStatusSteps(status: UserAccount['status']): readonly ('activate' | 'deactivate')[] {
  if (status === 'inactive') return ['activate'];
  if (status === 'active' || status === 'locked') return ['deactivate'];
  return [];
}

/** What the user list is narrowed to. */
export interface UserQuery {
  /** Part of a login name, e-mail or mobile number. */
  search?: string;
  status?: UserAccountStatus;
  userType?: UserType;
}

const quoted = (value: string) => `"${value.replace(/["\\]/g, '')}"`;

/** The iam.ListUserAccounts filter of a query. */
export function userFilter(query: UserQuery): string | undefined {
  const parts: string[] = [];
  const search = query.search?.trim();
  if (search) {
    const q = quoted(search);
    parts.push(`(username : ${q} OR email : ${q} OR mobileNo : ${q})`);
  }
  if (query.status) parts.push(`status = ${quoted(query.status)}`);
  if (query.userType) parts.push(`userType = ${quoted(query.userType)}`);
  return parts.length ? parts.join(' AND ') : undefined;
}

const USERS = queryKey('iam', 'user-accounts');
const USER = (id: string) => queryKey('iam', 'user-account', id);
const USER_ROLES = (id: string) => queryKey('iam', 'user-roles', id);

async function refreshUser(qc: QueryClient, id?: string): Promise<void> {
  void qc.invalidateQueries({ queryKey: queryKey('records', 'iam.UserAccount') });
  await Promise.all([
    qc.invalidateQueries({ queryKey: USERS }),
    id ? qc.invalidateQueries({ queryKey: USER(id) }) : Promise.resolve(),
  ]);
}

/** ADM-08: user accounts, page by page (iam.ListUserAccounts), newest first. */
export function useUserAccounts(query: UserQuery, enabled = true) {
  const { api } = useRuntime();
  const filter = userFilter(query);
  return useInfiniteQuery({
    queryKey: [...USERS, filter ?? ''],
    queryFn: ({ pageParam }) =>
      call(
        api.get(UserAccountApi).listUserAccounts({
          size: 25,
          sort: ['createTime desc'],
          ...(filter ? { filter } : {}),
          ...pageQuery(pageParam),
        }),
      ),
    initialPageParam: FIRST_PAGE,
    getNextPageParam: (last) => nextPage(last.paging),
    enabled,
    staleTime: STALE_TIME.list,
  });
}

/** ADM-09: one account (iam.GetUserAccount). */
export function useUserAccount(id: string, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: USER(id),
    queryFn: () => call(api.get(UserAccountApi).getUserAccount({ userAccountId: id })),
    enabled: enabled && Boolean(id),
    staleTime: STALE_TIME.detail,
  });
}

/** iam.CreateUserAccount: a new account without a password (set credentials or require a reset next). */
export function useCreateUserAccount() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (account: Pick<UserAccount, 'username' | 'email' | 'mobileNo' | 'userType'>) =>
      call(api.get(UserAccountApi).createUserAccount({ userAccount: account })),
    onSuccess: (created) => refreshUser(qc, created.id),
  });
}

/** iam.ActivateUser / iam.DeactivateUser / iam.UnlockUser under the account's ETag. */
export function useUserStatusStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      etag,
      step,
    }: {
      id: string;
      etag: string;
      step: 'activate' | 'deactivate' | 'unlock';
    }) => {
      const users = api.get(UserAccountApi);
      if (step === 'activate')
        return call(users.activateUser({ userAccountId: id, activateUserRequest: { etag } }));
      if (step === 'deactivate')
        return call(users.deactivateUser({ userAccountId: id, deactivateUserRequest: { etag } }));
      return call(users.unlockUser({ userAccountId: id, unlockUserRequest: { etag } }));
    },
    onSuccess: (_, { id }) => refreshUser(qc, id),
  });
}

/** iam.ForcePasswordReset: a new password at the next sign-in; the account is signed out and mailed a code. */
export function useForcePasswordReset() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, etag }: { id: string; etag: string }) =>
      call(
        api
          .get(UserAccountApi)
          .forcePasswordReset({ userAccountId: id, forcePasswordResetRequest: { etag } }),
      ),
    onSuccess: (_, { id }) => refreshUser(qc, id),
  });
}

/** iam.SetUserCredentials: a login name and a temporary password chosen by an administrator. */
export function useSetUserCredentials() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      ...body
    }: {
      id: string;
      username?: string;
      temporaryPassword: string;
      mustResetPassword: boolean;
    }) =>
      call(
        api.get(UserAccountApi).setUserCredentials({ userAccountId: id, setUserCredentialsRequest: body }),
      ),
    onSuccess: (_, { id }) => refreshUser(qc, id),
  });
}

/** Whether a login name is free (iam.CheckUsernameAvailable), with a free variant when it is not. */
export function useUsernameAvailability(username: string, enabled = true) {
  const { api } = useRuntime();
  const name = username.trim();
  return useQuery({
    queryKey: queryKey('iam', 'username', name),
    queryFn: () => call(api.get(UserAccountApi).checkUsernameAvailable({ username: name })),
    enabled: enabled && name.length > 0,
    staleTime: STALE_TIME.list,
    retry: false,
  });
}

/** ADM-09: the account's role grants (iam.ListUserRoles), all of them. */
export function useUserRoles(id: string, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: USER_ROLES(id),
    queryFn: () =>
      allPages((page) =>
        call(api.get(UserAccountApi).listUserRoles({ userAccountId: id, size: 100, ...pageQuery(page) })),
      ),
    enabled: enabled && Boolean(id),
    staleTime: STALE_TIME.detail,
  });
}

/** The tenant's roles (iam.ListRoles), for choosing grants. */
export function useRoles(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('iam', 'roles'),
    queryFn: () => allPages((page) => call(api.get(RoleApi).listRoles({ size: 100, ...pageQuery(page) }))),
    enabled,
    staleTime: STALE_TIME.list,
  });
}

/** iam.SetUserRoles: the account's grants become exactly these (only roles the caller could grant). */
export function useSetUserRoles() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, grants }: { id: string; grants: RoleGrant[] }) =>
      call(api.get(UserAccountApi).setUserRoles({ userAccountId: id, setUserRolesRequest: { grants } })),
    onSuccess: (_, { id }) => void qc.invalidateQueries({ queryKey: USER_ROLES(id) }),
  });
}
