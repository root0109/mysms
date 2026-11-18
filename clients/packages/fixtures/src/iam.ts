import type { iam } from '@mysms/sdk';
import { SELF_SERVICE, schoolAdmin, tokens } from './data';
import { json, problem, type Handler } from './fakeApi';
import { onePage, pageOf } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/**
 * Accounts and access of one school: two roles, a teacher with both, a student, one locked account; an API token of
 * the signed-in user and two of their sessions (this one and another device).
 */
export const iamFixture = {
  teacherId: id('f11'),
  studentId: id('f12'),
  lockedId: id('f13'),
  teacherRoleId: id('f21'),
  reviewerRoleId: id('f22'),
  apiTokenId: id('f31'),
  thisSessionId: id('f41'),
  otherSessionId: id('f42'),
  resetToken: `${id('f51')}.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA`,
  verificationCode: `${id('f51')}.BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB`,
  /** The two active schools of tenant `demo` a student may sign up at. */
  northSchoolId: id('f61'),
  southSchoolId: id('f62'),
} as const;

/** What a tenant administrator of accounts holds for ADM-08 and ADM-09. */
export const ACCOUNT_ADMIN = [
  ...SELF_SERVICE,
  'iam.user_account.read',
  'iam.user_account.create',
  'iam.user_account.update',
  'iam.user_account.activate_user',
  'iam.user_account.deactivate_user',
  'iam.user_account.unlock_user',
  'iam.user_account.force_password_reset',
  'iam.user_account.set_user_credentials',
  'iam.user_account.set_user_roles',
  'iam.user_account.start_impersonation',
  'iam.user_account.create_api_token',
  'iam.user_role.read',
  'iam.role.read',
] as const;

/** An unsigned JWT-shaped token whose payload carries the claims (the fakes never verify signatures). */
export function fakeJwt(claims: Record<string, unknown>): string {
  const encode = (value: unknown) =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${encode({ alg: 'none' })}.${encode(claims)}.`;
}

export interface FakeIamOptions {
  /** The signed-in user (default: the school administrator). */
  me?: iam.Me;
  /** The signed-in user's permissions (default: an account administrator). */
  permissions?: readonly string[];
}

/**
 * The fake iam of the account screens: users with status steps and grants, roles, the caller's API tokens and
 * sessions, the public reset / sign-up / verification flows (the fixture's codes are the valid ones), the role switch
 * and impersonation. State lives in the closure, so a test sees its own writes.
 */
export function fakeIam(options: FakeIamOptions = {}): Record<string, Handler> {
  const fx = iamFixture;
  const now = '2026-11-01T08:00:00Z';
  const permissions = [...(options.permissions ?? ACCOUNT_ADMIN)];
  const base = options.me ?? schoolAdmin;
  const me: iam.Me = {
    ...base,
    user: { ...base.user },
    permissions,
    roles: [
      { roleCode: 'teacher', roleId: fx.teacherRoleId, scopeType: 'tenant' },
      { roleCode: 'reviewer', roleId: fx.reviewerRoleId, scopeType: 'tenant' },
    ],
  };
  const users: iam.UserAccount[] = [
    {
      id: fx.teacherId,
      username: 'meera',
      email: 'meera@school.example',
      userType: 'teacher',
      status: 'active',
      etag: '"1"',
    },
    { id: fx.studentId, username: 'arjun', userType: 'student', status: 'inactive', etag: '"1"' },
    { id: fx.lockedId, username: 'kabir', userType: 'staff', status: 'locked', etag: '"3"' },
  ];
  const roles: iam.Role[] = [
    { id: fx.teacherRoleId, code: 'teacher', displayName: 'Teacher' },
    { id: fx.reviewerRoleId, code: 'reviewer', displayName: 'Reviewer' },
  ];
  const grants = new Map<string, iam.UserRole[]>([
    [
      fx.teacherId,
      [{ id: id('f61'), userId: fx.teacherId, roleId: fx.teacherRoleId, scopeType: 'tenant', etag: '"1"' }],
    ],
  ]);
  let apiTokens: iam.AuthToken[] = [
    {
      id: fx.apiTokenId,
      tokenType: 'api',
      name: 'Reports export',
      expiresAt: '2027-01-01T00:00:00Z',
      etag: '"1"',
    },
  ];
  let sessions: iam.UserSession[] = [
    { id: fx.thisSessionId, sessionRef: 'this', userAgent: 'Firefox', startedAt: now, etag: '"1"' },
    { id: fx.otherSessionId, sessionRef: 'other', userAgent: 'Android app', startedAt: now, etag: '"1"' },
  ];
  let verified = false;
  const body = async <T>(req: Request) => (await req.json()) as T;
  const user = (userId: string) => users.find((u) => u.id === userId);
  const bump = (etag: string | undefined) => `"${Number((etag ?? '"0"').replace(/\D/g, '')) + 1}"`;
  const step =
    (status: iam.UserAccount['status'], from: readonly string[]): Handler =>
    async (req, p) => {
      const row = user(p.userAccountId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const { etag } = await body<{ etag?: string }>(req);
      if (etag && etag !== row.etag) return problem(412, 'ETAG_MISMATCH');
      if (!from.includes(row.status ?? '')) return problem(409, 'INVALID_STATE_TRANSITION');
      Object.assign(row, { status, etag: bump(row.etag) });
      return json(row);
    };
  const access = fakeJwt({ sub: me.user.id, sid: fx.thisSessionId });
  return {
    'POST /v1/iam/auth:login': () => json({ tokens: { ...tokens, accessToken: access }, user: me.user }),
    'GET /v1/iam/me': (req) =>
      req.headers.get('Authorization') === 'Bearer imp-token'
        ? json({ user: users[0], permissions: [...SELF_SERVICE], roles: [] })
        : json(me),
    'GET /v1/iam/users': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const status = /status = "(\w+)"/.exec(filter)?.[1];
      const text = /username : "([^"]*)"/.exec(filter)?.[1]?.toLowerCase();
      return pageOf(
        req,
        users.filter(
          (u) =>
            (!status || u.status === status) &&
            (!text || (u.username ?? '').includes(text) || (u.email ?? '').includes(text)),
        ),
      );
    },
    'POST /v1/iam/users': async (req) => {
      const created = {
        ...(await body<iam.UserAccount>(req)),
        id: id(`f7${users.length}`),
        status: 'active' as const,
        etag: '"1"',
      };
      users.push(created);
      return json(created, { status: 201 });
    },
    'GET /v1/iam/users/{userAccountId}': (_req, p) => {
      const row = user(p.userAccountId ?? '');
      return row ? json(row) : problem(404, 'NOT_FOUND');
    },
    'POST /v1/iam/users/{userAccountId}:activate': step('active', ['inactive']),
    'POST /v1/iam/users/{userAccountId}:deactivate': step('inactive', ['active', 'locked']),
    'POST /v1/iam/users/{userAccountId}:unlock': step(undefined, ['active', 'inactive', 'locked', 'exited']),
    'POST /v1/iam/users/{userAccountId}:forcePasswordReset': (_req, p) => {
      const row = user(p.userAccountId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (!row.email)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [{ field: 'email', code: 'NO_EMAIL', message: 'no e-mail' }],
        });
      Object.assign(row, { mustResetPassword: true, etag: bump(row.etag) });
      return json(row);
    },
    'POST /v1/iam/users/{userAccountId}:setCredentials': async (req, p) => {
      const row = user(p.userAccountId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const { username, mustResetPassword } = await body<iam.SetUserCredentialsRequest>(req);
      Object.assign(row, { username: username ?? row.username, mustResetPassword, etag: bump(row.etag) });
      return json(row);
    },
    'GET /v1/iam/users:checkUsername': (req) => {
      const name = new URL(req.url).searchParams.get('username') ?? '';
      const taken = users.some((u) => u.username?.toLowerCase() === name.toLowerCase());
      return json(taken ? { available: false, suggestion: `${name}2` } : { available: true });
    },
    'GET /v1/iam/users/{userAccountId}/roles': (req, p) =>
      onePage(req, grants.get(p.userAccountId ?? '') ?? []),
    'POST /v1/iam/users/{userAccountId}:setRoles': async (req, p) => {
      const { grants: wanted } = await body<iam.SetUserRolesRequest>(req);
      const unknown = wanted.findIndex((g) => !roles.some((r) => r.code === g.roleCode));
      if (unknown >= 0)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [
            {
              field: `grants[${unknown}].roleCode`,
              code: 'NOT_FOUND',
              message: 'no active role has this code',
            },
          ],
        });
      const items = wanted.map((g, i) => ({
        id: id(`f8${i}`),
        userId: p.userAccountId,
        roleId: roles.find((r) => r.code === g.roleCode)?.id ?? '',
        scopeType: g.scopeType,
        scopeId: g.scopeId,
        etag: '"1"',
      }));
      grants.set(p.userAccountId ?? '', items);
      return json({ items });
    },
    'GET /v1/iam/roles': (req) => onePage(req, roles),
    'POST /v1/iam/users/{userAccountId}:impersonate': async (req, p) => {
      const { reason } = await body<{ reason: string }>(req);
      if (!reason.trim()) return problem(422, 'VALIDATION_FAILED');
      return json({ accessToken: 'imp-token', expiresInSeconds: 900, impersonatedUserId: p.userAccountId });
    },
    'POST /v1/iam/auth:stopImpersonation': () => new Response(null, { status: 204 }),
    'POST /v1/iam/me:switchRole': async (req) => {
      const { roleId } = await body<{ roleId: string }>(req);
      const grant = me.roles?.find((r) => r.roleId === roleId);
      if (!grant) return problem(422, 'VALIDATION_FAILED');
      return json({
        tokens: { ...tokens, accessToken: fakeJwt({ sid: fx.thisSessionId, role: roleId }) },
        me: { ...me, activeRole: grant },
      });
    },
    'POST /v1/iam/me:acceptTerms': () => {
      me.user = { ...me.user, termsAcceptedAt: now } as iam.UserAccount;
      return json(me.user);
    },
    'GET /v1/iam/users/{userAccountId}/api-tokens': (req) => onePage(req, apiTokens),
    'POST /v1/iam/users/{userAccountId}:createApiToken': async (req) => {
      const { name } = await body<{ name: string }>(req);
      const created: iam.AuthToken = { id: id('f32'), tokenType: 'api', name, etag: '"1"' };
      apiTokens = [...apiTokens, created];
      return json({ token: 'mysms_pat_secret-shown-once', authToken: created });
    },
    'DELETE /v1/iam/users/{userAccountId}/api-tokens/{authTokenId}': (req, p) => {
      const row = apiTokens.find((t) => t.id === p.authTokenId);
      if (!row) return problem(404, 'NOT_FOUND');
      if (req.headers.get('If-Match') !== row.etag) return problem(412, 'ETAG_MISMATCH');
      apiTokens = apiTokens.filter((t) => t.id !== p.authTokenId);
      return new Response(null, { status: 204 });
    },
    'GET /v1/iam/users/{userAccountId}/sessions': (req) => onePage(req, sessions),
    'DELETE /v1/iam/users/{userAccountId}/sessions/{userSessionId}': (_req, p) => {
      sessions = sessions.filter((s) => s.id !== p.userSessionId);
      return new Response(null, { status: 204 });
    },
    'POST /v1/iam/auth:requestPasswordReset': () => new Response(null, { status: 204 }),
    'POST /v1/iam/auth:resetPassword': async (req) => {
      const { token } = await body<{ token: string }>(req);
      return token === fx.resetToken
        ? new Response(null, { status: 204 })
        : problem(422, 'VALIDATION_FAILED', 'Invalid', {
            errors: [{ field: 'token', code: 'INVALID_CODE', message: 'is not valid or has expired' }],
          });
    },
    'GET /v1/iam/tenant-codes/{tenantCode}/sign-up-institutions': (_req, p) =>
      p.tenantCode !== 'demo'
        ? problem(403, 'PERMISSION_DENIED')
        : json({
            items: [
              { id: fx.northSchoolId, name: 'North School' },
              { id: fx.southSchoolId, name: 'South School' },
            ],
          }),
    'POST /v1/iam/auth:signUp': async (req) => {
      const form = await body<iam.SignUpRequest>(req);
      if (form.tenantCode !== 'demo') return problem(403, 'PERMISSION_DENIED');
      if (form.institutionId !== fx.northSchoolId && form.institutionId !== fx.southSchoolId)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [
            { field: 'institutionId', code: 'NOT_FOUND', message: 'must be an active school of this tenant' },
          ],
        });
      return json({ username: form.email.toLowerCase(), email: form.email, userType: 'student' });
    },
    'POST /v1/iam/auth:requestVerification': () =>
      verified ? problem(409, 'INVALID_STATE_TRANSITION') : new Response(null, { status: 204 }),
    'POST /v1/iam/auth:confirmVerification': async (req) => {
      const { code } = await body<{ code: string }>(req);
      if (code !== fx.verificationCode)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [{ field: 'code', code: 'INVALID_CODE', message: 'is not valid or has expired' }],
        });
      verified = true;
      return json({ ...me.user, emailVerifiedAt: now });
    },
  };
}
