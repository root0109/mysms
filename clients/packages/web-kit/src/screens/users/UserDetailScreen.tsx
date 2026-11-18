import {
  ROLE_SCOPE_TYPES,
  isApiError,
  useCan,
  useForcePasswordReset,
  useI18n,
  useImpersonate,
  useRoles,
  useSession,
  useSetUserCredentials,
  useSetUserRoles,
  useUserAccount,
  useUserRoles,
  useUserStatusStep,
  userStatusSteps,
  type RoleGrant,
  type RoleScopeType,
  type UserAccount,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, Dialog, SelectField, StatusChip, Switch, TextInput } from '@mysms/ui-web';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { MIN_PASSWORD } from '../account/accountText';
import { USER_STATUS, USER_TYPE } from './userText';

/** What a failed administrator action says: a stale copy, a refusal (no escalation), a missing address, or generic. */
function actionProblem(error: unknown): MessageKey {
  if (!isApiError(error)) return 'common.error.generic';
  if (error.status === 412 || error.status === 409) return 'adm.users.changed';
  if (error.status === 403) return 'adm.users.refused';
  if (error.code === 'NOT_CONFIGURED') return 'acc.code.notConfigured';
  if (error.errors.some((e) => e.code === 'NO_EMAIL')) return 'adm.users.noEmail';
  if (error.status === 422) return 'acc.code.checkFields';
  return 'common.error.generic';
}

/**
 * ADM-09 User detail & grants: the account with its status steps (activate, deactivate, unlock), a required password
 * reset, credentials set by the administrator, its role grants replaced as a set (only roles the administrator could
 * grant), and acting as the user for support (a reason is required; a banner stays until it ends). Every action is
 * offered only with its permission and every write sends the account's ETag.
 */
export function UserDetailScreen() {
  const { t } = useI18n();
  const { userAccountId = '' } = useParams();
  const mayRead = useCan('iam.user_account.read');
  const account = useUserAccount(userAccountId, mayRead);
  const self = useSession((s) => s.me?.user?.id === userAccountId);

  if (!mayRead || (account.isError && isApiError(account.error) && account.error.isNotAvailable))
    return (
      <div className="page">
        <Banner intent="info">{t('common.state.notAvailable')}</Banner>
        <Link to="/app/iam/user-accounts">{t('adm.users.backToList')}</Link>
      </div>
    );
  if (account.isError)
    return (
      <div className="page">
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void account.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      </div>
    );
  if (!account.data) return <div className="page">{t('common.state.loading')}</div>;
  const user = account.data;
  const state = user.status ? USER_STATUS[user.status] : undefined;
  return (
    <div className="page">
      <header className="page__header">
        <Link to="/app/iam/user-accounts">{t('adm.users.backToList')}</Link>
        <h1 className="page__title">{user.username}</h1>
        <p className="page__subtitle">
          <StatusChip intent={state?.intent} label={state ? t(state.label) : '—'} />{' '}
          {user.userType ? t(USER_TYPE[user.userType]) : null}
        </p>
      </header>
      <AccountCard user={user} self={self} />
      <GrantsCard userId={userAccountId} self={self} />
      {self ? null : <ImpersonateCard user={user} />}
    </div>
  );
}

function AccountCard({ user, self }: { user: UserAccount; self: boolean }) {
  const { t, format } = useI18n();
  const mayActivate = useCan('iam.user_account.activate_user');
  const mayDeactivate = useCan('iam.user_account.deactivate_user');
  const mayUnlock = useCan('iam.user_account.unlock_user');
  const mayForce = useCan('iam.user_account.force_password_reset');
  const mayCredentials = useCan('iam.user_account.set_user_credentials');
  const step = useUserStatusStep();
  const force = useForcePasswordReset();
  const [credentials, setCredentials] = useState(false);
  const etag = user.etag ?? '';
  const id = user.id ?? '';
  const steps = userStatusSteps(user.status).filter((s) => (s === 'activate' ? mayActivate : mayDeactivate));
  const failure = step.error ?? force.error;
  return (
    <Card title={t('adm.users.account')}>
      <dl className="facts">
        <dt>{t('adm.users.column.email')}</dt>
        <dd>
          {user.email ?? '—'}{' '}
          {user.email ? t(user.emailVerifiedAt ? 'adm.users.verified' : 'adm.users.unverified') : null}
        </dd>
        <dt>{t('adm.users.column.mobile')}</dt>
        <dd>{user.mobileNo ?? '—'}</dd>
        <dt>{t('adm.users.lastLogin')}</dt>
        <dd>{user.lastLoginAt ? format.dateTime(user.lastLoginAt) : '—'}</dd>
        <dt>{t('adm.users.mustReset')}</dt>
        <dd>{t(user.mustResetPassword ? 'adm.users.yes' : 'adm.users.no')}</dd>
      </dl>
      {step.isSuccess || force.isSuccess ? <Banner intent="success">{t('adm.users.saved')}</Banner> : null}
      {failure ? <Banner intent="danger">{t(actionProblem(failure))}</Banner> : null}
      {self ? (
        <p className="hint">{t('adm.users.ownAccount')}</p>
      ) : (
        <div className="form__actions">
          {steps.map((s) => (
            <Button
              key={s}
              variant={s === 'deactivate' ? 'danger' : 'primary'}
              loading={step.isPending && step.variables?.step === s}
              onClick={() => step.mutate({ id, etag, step: s })}
            >
              {t(s === 'activate' ? 'adm.users.activate' : 'adm.users.deactivate')}
            </Button>
          ))}
          {mayUnlock && (user.failedLoginCount ?? 0) > 0 ? (
            <Button
              variant="secondary"
              loading={step.isPending && step.variables?.step === 'unlock'}
              onClick={() => step.mutate({ id, etag, step: 'unlock' })}
            >
              {t('adm.users.unlock')}
            </Button>
          ) : null}
          {mayForce ? (
            <Button variant="secondary" loading={force.isPending} onClick={() => force.mutate({ id, etag })}>
              {t('adm.users.forceReset')}
            </Button>
          ) : null}
          {mayCredentials ? (
            <Button variant="secondary" onClick={() => setCredentials(true)}>
              {t('adm.users.setCredentials')}
            </Button>
          ) : null}
        </div>
      )}
      {credentials ? <CredentialsDialog user={user} onClose={() => setCredentials(false)} /> : null}
    </Card>
  );
}

/** A login name and a temporary password, which the user changes at the next sign-in unless told otherwise. */
function CredentialsDialog({ user, onClose }: { user: UserAccount; onClose: () => void }) {
  const { t } = useI18n();
  const save = useSetUserCredentials();
  const [username, setUsername] = useState(user.username ?? '');
  const [password, setPassword] = useState('');
  const [mustReset, setMustReset] = useState(true);
  const [touched, setTouched] = useState(false);
  const short = (touched || password.length > 0) && password.length < MIN_PASSWORD;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (password.length < MIN_PASSWORD || !username.trim()) return;
    save.mutate(
      {
        id: user.id ?? '',
        temporaryPassword: password,
        mustResetPassword: mustReset,
        ...(username.trim() !== user.username ? { username: username.trim() } : {}),
      },
      { onSuccess: onClose },
    );
  };
  return (
    <Dialog
      open
      title={t('adm.users.setCredentials')}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button type="submit" form="credentials" loading={save.isPending}>
            {t('common.action.save')}
          </Button>
        </>
      }
    >
      <form id="credentials" className="form" noValidate onSubmit={submit}>
        <p className="hint">{t('adm.users.credentialsIntro')}</p>
        <TextInput
          label={t('adm.users.column.username')}
          maxLength={150}
          value={username}
          error={touched && !username.trim() ? t('common.validation.required') : undefined}
          onChange={(e) => setUsername(e.target.value)}
        />
        <TextInput
          label={t('adm.users.temporaryPassword')}
          type="password"
          autoComplete="new-password"
          value={password}
          error={short ? t('acc.security.password.tooShort', { min: MIN_PASSWORD }) : undefined}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Switch label={t('adm.users.mustResetNext')} checked={mustReset} onChange={setMustReset} />
        {save.isError ? <Banner intent="danger">{t(actionProblem(save.error))}</Banner> : null}
      </form>
    </Dialog>
  );
}

const SCOPE_TYPE: Readonly<Record<RoleScopeType, MessageKey>> = {
  tenant: 'adm.users.scope.tenant',
  institution: 'adm.users.scope.institution',
  department: 'adm.users.scope.department',
  class_section: 'adm.users.scope.classSection',
  course: 'adm.users.scope.course',
  lms_course: 'adm.users.scope.lmsCourse',
  lms_cohort: 'adm.users.scope.lmsCohort',
};
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** The account's role grants, edited as a whole and saved with iam.SetUserRoles. */
function GrantsCard({ userId, self }: { userId: string; self: boolean }) {
  const { t } = useI18n();
  const mayRead = useCan('iam.user_role.read');
  const mayRoles = useCan('iam.role.read');
  const maySet = useCan('iam.user_account.set_user_roles') && mayRoles && !self;
  const grants = useUserRoles(userId, mayRead);
  const roles = useRoles(mayRoles);
  const setRoles = useSetUserRoles();
  const [draft, setDraft] = useState<RoleGrant[] | null>(null);
  const [roleCode, setRoleCode] = useState('');
  const [scopeType, setScopeType] = useState<RoleScopeType>('tenant');
  const [scopeId, setScopeId] = useState('');
  if (!mayRead) return null;
  const codeOf = new Map((roles.data?.items ?? []).map((r) => [r.id, r.code]));
  const nameOf = new Map((roles.data?.items ?? []).map((r) => [r.code, r.displayName ?? r.code]));
  const current: RoleGrant[] = (grants.data?.items ?? []).map((g) => ({
    roleCode: codeOf.get(g.roleId) ?? g.roleId,
    scopeType: g.scopeType ?? 'tenant',
    ...(g.scopeId ? { scopeId: g.scopeId } : {}),
  }));
  const shown = draft ?? current;
  const scopeNeeded = scopeType !== 'tenant';
  const scopeValid = !scopeNeeded || UUID.test(scopeId.trim());
  const add = () => {
    if (!roleCode || !scopeValid) return;
    const grant: RoleGrant = { roleCode, scopeType, ...(scopeNeeded ? { scopeId: scopeId.trim() } : {}) };
    setDraft([...shown, grant]);
    setScopeId('');
  };
  return (
    <Card title={t('adm.users.grants')}>
      {grants.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {grants.isPending ? <p>{t('common.state.loading')}</p> : null}
      {grants.data && shown.length === 0 ? <p className="hint">{t('adm.users.noGrants')}</p> : null}
      <ul className="persona-list">
        {shown.map((g, index) => (
          <li key={`${g.roleCode}/${g.scopeType}/${g.scopeId ?? ''}`}>
            {nameOf.get(g.roleCode) ?? g.roleCode} — {t(SCOPE_TYPE[g.scopeType])}
            {g.scopeId ? ` (${g.scopeId})` : ''}
            {maySet ? (
              <Button variant="ghost" onClick={() => setDraft(shown.filter((_, i) => i !== index))}>
                {t('adm.users.removeGrant')}
              </Button>
            ) : null}
          </li>
        ))}
      </ul>
      {maySet ? (
        <>
          <div className="page__grid">
            <SelectField
              label={t('adm.users.role')}
              value={roleCode}
              onChange={(e) => setRoleCode(e.target.value)}
              options={[
                { value: '', label: t('adm.users.chooseRole') },
                ...(roles.data?.items ?? [])
                  .filter((r) => !r.isSystem && r.isActive !== false)
                  .map((r) => ({ value: r.code, label: r.displayName ?? r.code })),
              ]}
            />
            <SelectField
              label={t('adm.users.scopeType')}
              value={scopeType}
              onChange={(e) => setScopeType(e.target.value as RoleScopeType)}
              options={ROLE_SCOPE_TYPES.map((value) => ({ value, label: t(SCOPE_TYPE[value]) }))}
            />
            {scopeNeeded ? (
              <TextInput
                label={t('adm.users.scopeId')}
                value={scopeId}
                error={scopeId && !scopeValid ? t('common.validation.pattern') : undefined}
                onChange={(e) => setScopeId(e.target.value)}
              />
            ) : null}
          </div>
          {setRoles.isError ? <Banner intent="danger">{t(actionProblem(setRoles.error))}</Banner> : null}
          {setRoles.isSuccess && !draft ? <Banner intent="success">{t('adm.users.saved')}</Banner> : null}
          <div className="form__actions">
            <Button variant="secondary" disabled={!roleCode || !scopeValid} onClick={add}>
              {t('adm.users.addGrant')}
            </Button>
            <Button
              disabled={!draft}
              loading={setRoles.isPending}
              onClick={() =>
                draft && setRoles.mutate({ id: userId, grants: draft }, { onSuccess: () => setDraft(null) })
              }
            >
              {t('adm.users.saveGrants')}
            </Button>
            {draft ? (
              <Button variant="ghost" onClick={() => setDraft(null)}>
                {t('common.action.cancel')}
              </Button>
            ) : null}
          </div>
        </>
      ) : null}
    </Card>
  );
}

/** Acting as the user for support: a reason is required and kept in the audit log with both identities. */
function ImpersonateCard({ user }: { user: UserAccount }) {
  const { t } = useI18n();
  const navigate = useNavigate();
  const may = useCan('iam.user_account.start_impersonation');
  const impersonate = useImpersonate();
  const [reason, setReason] = useState('');
  const [touched, setTouched] = useState(false);
  if (!may || user.status !== 'active') return null;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!reason.trim()) return;
    // the switch to the user clears this page's data (and may unmount it): wait for the call itself, not the card
    impersonate
      .mutateAsync({ userId: user.id ?? '', reason: reason.trim() })
      .then(() => navigate('/app'))
      .catch(() => undefined);
  };
  return (
    <Card title={t('adm.users.impersonate')}>
      <form className="form" noValidate onSubmit={submit}>
        <p className="hint">{t('adm.users.impersonateIntro')}</p>
        <TextInput
          label={t('adm.users.reason')}
          maxLength={500}
          value={reason}
          error={touched && !reason.trim() ? t('common.validation.required') : undefined}
          onChange={(e) => setReason(e.target.value)}
        />
        {impersonate.isError ? <Banner intent="danger">{t(actionProblem(impersonate.error))}</Banner> : null}
        <div className="form__actions">
          <Button type="submit" variant="danger" loading={impersonate.isPending}>
            {t('adm.users.impersonateStart')}
          </Button>
        </div>
      </form>
    </Card>
  );
}
