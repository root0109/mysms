import {
  can,
  currentSessionId,
  isApiError,
  useChangePassword,
  useCreateApiToken,
  useEndOtherSessions,
  useMyApiTokens,
  useRevokeApiToken,
  useDisableMfa,
  useEndSession,
  useEnrollMfa,
  useI18n,
  useMfaCredentials,
  useMySessions,
  useRegenerateRecoveryCodes,
  useSession,
  useVerifyMfaEnrollment,
  type MfaCredential,
  type MfaEnrollment,
  type AuthToken,
  type UserSession,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Skeleton, StatusChip, TextInput, type Column } from '@mysms/ui-web';
import { useState, type FormEvent } from 'react';

type Translate = ReturnType<typeof useI18n>['t'];

/** The server's minimum password length (iam password policy). */
const MIN_PASSWORD = 12;
const CODE = /^\d{6}$|^[A-Za-z0-9-]{8,20}$/;

function errorText(t: Translate, error: unknown): string {
  // a 401 carrying the sign-in codes means the session ended, not that the code was wrong
  if (isApiError(error) && error.status === 401 && /UNAUTHENTICATED|TOKEN/.test(error.code ?? ''))
    return t('acc.security.sessionEnded');
  if (isApiError(error) && (error.status === 400 || error.status === 401 || error.status === 422))
    return t('acc.security.rejected');
  if (isApiError(error) && error.status === 429) return t('acc.security.locked');
  if (isApiError(error) && error.status === 501) return t('acc.security.notAvailable');
  return t('common.error.generic');
}

/**
 * ACC-08 Security & sessions: change the password, add an authenticator app (its secret and recovery codes are
 * shown once), turn a factor off with a current code, replace the recovery codes, end sign-in sessions (one, or every
 * one but this), and create and revoke personal API tokens (the secret is shown once). Every
 * check happens on the server; this screen only collects what the operations need.
 */
export function SecurityScreen() {
  const { t } = useI18n();
  return (
    <div className="page">
      <PasswordCard t={t} />
      <MfaCard t={t} />
      <SessionsCard t={t} />
      <ApiTokensCard t={t} />
    </div>
  );
}

function PasswordCard({ t }: { t: Translate }) {
  const change = useChangePassword();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [repeat, setRepeat] = useState('');
  const [touched, setTouched] = useState(false);
  const tooShort = next.length > 0 && next.length < MIN_PASSWORD;
  const mismatch = repeat.length > 0 && repeat !== next;
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setTouched(true);
    if (!current || next.length < MIN_PASSWORD || next !== repeat) return;
    change.mutate(
      { currentPassword: current, newPassword: next },
      {
        onSuccess: () => {
          setCurrent('');
          setNext('');
          setRepeat('');
          setTouched(false);
        },
      },
    );
  };
  return (
    <Card title={t('acc.security.password.title')}>
      <form className="form" onSubmit={submit} noValidate>
        <p className="hint">{t('acc.security.password.intro')}</p>
        <TextInput
          label={t('acc.security.password.current')}
          type="password"
          autoComplete="current-password"
          value={current}
          error={touched && !current ? t('common.validation.required') : undefined}
          onChange={(e) => setCurrent(e.target.value)}
        />
        <TextInput
          label={t('acc.security.password.new')}
          type="password"
          autoComplete="new-password"
          value={next}
          hint={t('acc.security.password.rule', { min: MIN_PASSWORD })}
          error={
            tooShort || (touched && next.length < MIN_PASSWORD)
              ? t('acc.security.password.tooShort', { min: MIN_PASSWORD })
              : undefined
          }
          onChange={(e) => setNext(e.target.value)}
        />
        <TextInput
          label={t('acc.security.password.repeat')}
          type="password"
          autoComplete="new-password"
          value={repeat}
          error={mismatch || (touched && repeat !== next) ? t('acc.security.password.mismatch') : undefined}
          onChange={(e) => setRepeat(e.target.value)}
        />
        {change.isSuccess ? <Banner intent="success">{t('acc.security.password.changed')}</Banner> : null}
        {change.isError ? <Banner intent="danger">{errorText(t, change.error)}</Banner> : null}
        <div className="form__actions">
          <Button type="submit" loading={change.isPending}>
            {t('acc.security.password.submit')}
          </Button>
        </div>
      </form>
    </Card>
  );
}

function MfaCard({ t }: { t: Translate }) {
  const { format } = useI18n();
  const credentials = useMfaCredentials();
  const enroll = useEnrollMfa();
  const verify = useVerifyMfaEnrollment();
  const disable = useDisableMfa();
  const regenerate = useRegenerateRecoveryCodes();
  const [label, setLabel] = useState('');
  const [enrollment, setEnrollment] = useState<MfaEnrollment | null>(null);
  const [code, setCode] = useState('');
  const [disabling, setDisabling] = useState<MfaCredential | null>(null);
  const [disableCode, setDisableCode] = useState('');
  const [regenCode, setRegenCode] = useState('');
  const [freshCodes, setFreshCodes] = useState<readonly string[] | null>(null);
  const items = (credentials.data?.items ?? []).filter(
    (c) => c.credentialType !== 'recovery_codes' && c.isActive,
  );
  const hasVerified = items.some((c) => c.verifiedAt);
  const columns: Column<MfaCredential>[] = [
    {
      key: 'label',
      header: t('acc.security.mfa.label'),
      render: (c) => c.label ?? t('acc.security.mfa.app'),
    },
    {
      key: 'state',
      header: t('acc.security.mfa.state'),
      render: (c) =>
        c.verifiedAt ? (
          <StatusChip intent="success" label={t('acc.security.mfa.verified')} />
        ) : (
          <StatusChip intent="warning" label={t('acc.security.mfa.pending')} />
        ),
    },
    {
      key: 'lastUsedAt',
      header: t('acc.security.mfa.lastUsed'),
      render: (c) => (c.lastUsedAt ? format.dateTime(c.lastUsedAt) : '—'),
    },
    {
      key: 'actions',
      header: t('adm.records.actions'),
      render: (c) => (
        <Button variant="ghost" onClick={() => setDisabling(c)}>
          {t('acc.security.mfa.disable')}
        </Button>
      ),
    },
  ];
  return (
    <Card title={t('acc.security.mfa.title')}>
      <p className="hint">{t('acc.security.mfa.intro')}</p>
      {credentials.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={2} />
      ) : credentials.isError ? (
        <Banner intent="danger">{errorText(t, credentials.error)}</Banner>
      ) : (
        <DataTable
          caption={t('acc.security.mfa.listCaption')}
          columns={columns}
          rows={items}
          rowKey={(c) => c.id ?? ''}
          loadingLabel={t('common.state.loading')}
          emptyText={t('acc.security.mfa.none')}
        />
      )}

      {disabling ? (
        <form
          className="form"
          aria-label={t('acc.security.mfa.disable')}
          onSubmit={(e) => {
            e.preventDefault();
            if (!CODE.test(disableCode.trim()) || !disabling.id) return;
            disable.mutate(
              { mfaCredentialId: disabling.id, code: disableCode.trim() },
              { onSuccess: () => (setDisabling(null), setDisableCode('')) },
            );
          }}
        >
          <TextInput
            label={t('acc.security.mfa.currentCode')}
            inputMode="numeric"
            autoComplete="one-time-code"
            value={disableCode}
            onChange={(e) => setDisableCode(e.target.value)}
          />
          {disable.isError ? <Banner intent="danger">{errorText(t, disable.error)}</Banner> : null}
          <div className="form__actions">
            <Button type="submit" variant="danger" loading={disable.isPending}>
              {t('acc.security.mfa.confirmDisable')}
            </Button>
            <Button variant="secondary" onClick={() => setDisabling(null)}>
              {t('common.action.cancel')}
            </Button>
          </div>
        </form>
      ) : null}

      {enrollment ? (
        <form
          className="form"
          aria-label={t('acc.security.mfa.verifyTitle')}
          onSubmit={(e) => {
            e.preventDefault();
            if (!/^\d{6}$/.test(code.trim()) || !enrollment.mfaToken) return;
            verify.mutate(
              { mfaToken: enrollment.mfaToken, code: code.trim() },
              {
                onSuccess: () => {
                  // the secret and the recovery codes leave memory once the factor is verified
                  setEnrollment(null);
                  enroll.reset();
                  setCode('');
                  setLabel('');
                },
              },
            );
          }}
        >
          <Banner intent="warning" title={t('acc.security.mfa.onceTitle')}>
            {t('acc.security.mfa.onceBody')}
          </Banner>
          <dl className="secret">
            <dt>{t('acc.security.mfa.secret')}</dt>
            <dd>
              <code>{enrollment.secret}</code>
            </dd>
            {enrollment.otpauthUrl?.startsWith('otpauth://') ? (
              <>
                <dt>{t('acc.security.mfa.link')}</dt>
                <dd>
                  <a href={enrollment.otpauthUrl}>{t('acc.security.mfa.openApp')}</a>
                </dd>
              </>
            ) : null}
            <dt>{t('acc.security.mfa.recoveryCodes')}</dt>
            <dd>
              <ul className="codes">
                {(enrollment.recoveryCodes ?? []).map((c) => (
                  <li key={c}>
                    <code>{c}</code>
                  </li>
                ))}
              </ul>
            </dd>
          </dl>
          <TextInput
            label={t('acc.security.mfa.firstCode')}
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value)}
          />
          {verify.isError ? <Banner intent="danger">{errorText(t, verify.error)}</Banner> : null}
          <div className="form__actions">
            <Button type="submit" loading={verify.isPending}>
              {t('acc.security.mfa.verify')}
            </Button>
          </div>
        </form>
      ) : (
        <form
          className="form"
          aria-label={t('acc.security.mfa.add')}
          onSubmit={(e) => {
            e.preventDefault();
            enroll.mutate(label.trim(), { onSuccess: (result) => setEnrollment(result) });
          }}
        >
          <TextInput
            label={t('acc.security.mfa.newLabel')}
            maxLength={100}
            value={label}
            onChange={(e) => setLabel(e.target.value)}
          />
          {verify.isSuccess ? <Banner intent="success">{t('acc.security.mfa.added')}</Banner> : null}
          {enroll.isError ? <Banner intent="danger">{errorText(t, enroll.error)}</Banner> : null}
          <div className="form__actions">
            <Button type="submit" loading={enroll.isPending}>
              {t('acc.security.mfa.add')}
            </Button>
          </div>
        </form>
      )}

      {hasVerified ? (
        <form
          className="form"
          aria-label={t('acc.security.mfa.regenerate')}
          onSubmit={(e) => {
            e.preventDefault();
            if (!/^\d{6}$/.test(regenCode.trim())) return;
            regenerate.mutate(regenCode.trim(), {
              onSuccess: (result) => (setFreshCodes(result.recoveryCodes ?? []), setRegenCode('')),
            });
          }}
        >
          <TextInput
            label={t('acc.security.mfa.currentCode')}
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={regenCode}
            onChange={(e) => setRegenCode(e.target.value)}
          />
          {regenerate.isError ? <Banner intent="danger">{errorText(t, regenerate.error)}</Banner> : null}
          {freshCodes ? (
            <Banner
              intent="warning"
              title={t('acc.security.mfa.onceTitle')}
              action={
                <Button
                  variant="secondary"
                  onClick={() => {
                    setFreshCodes(null);
                    regenerate.reset();
                  }}
                >
                  {t('acc.security.mfa.savedCodes')}
                </Button>
              }
            >
              <ul className="codes">
                {freshCodes.map((c) => (
                  <li key={c}>
                    <code>{c}</code>
                  </li>
                ))}
              </ul>
            </Banner>
          ) : null}
          <div className="form__actions">
            <Button type="submit" variant="secondary" loading={regenerate.isPending}>
              {t('acc.security.mfa.regenerate')}
            </Button>
          </div>
        </form>
      ) : null}
    </Card>
  );
}

function SessionsCard({ t }: { t: Translate }) {
  const { format } = useI18n();
  const permissions = useSession((s) => s.me?.permissions);
  const mayRead = can(permissions, 'iam.user_session.read');
  const mayEnd = can(permissions, 'iam.user_session.delete');
  const sessions = useMySessions(mayRead);
  const end = useEndSession();
  const endOthers = useEndOtherSessions();
  const current = currentSessionId(useSession((s) => s.accessToken));
  if (!mayRead) return null;
  const rows = (sessions.data?.items ?? []).filter((s) => !s.endedAt);
  const others = rows.filter((s) => s.id !== current);
  const columns: Column<UserSession>[] = [
    {
      key: 'userAgent',
      header: t('acc.security.sessions.device'),
      render: (s) =>
        s.id === current
          ? t('acc.security.sessions.thisDevice', { device: s.userAgent ?? '—' })
          : (s.userAgent ?? '—'),
    },
    { key: 'ipAddress', header: t('acc.security.sessions.address'), render: (s) => s.ipAddress ?? '—' },
    {
      key: 'startedAt',
      header: t('acc.security.sessions.started'),
      render: (s) => (s.startedAt ? format.dateTime(s.startedAt) : '—'),
    },
    {
      key: 'lastSeenAt',
      header: t('acc.security.sessions.lastSeen'),
      render: (s) => (s.lastSeenAt ? format.dateTime(s.lastSeenAt) : '—'),
    },
    ...(mayEnd
      ? [
          {
            key: 'actions',
            header: t('adm.records.actions'),
            render: (s: UserSession) =>
              s.id === current ? null : (
                <Button
                  variant="ghost"
                  loading={end.isPending && end.variables?.id === s.id}
                  onClick={() => s.id && s.etag && end.mutate({ id: s.id, etag: s.etag })}
                >
                  {t('acc.security.sessions.end')}
                </Button>
              ),
          },
        ]
      : []),
  ];
  return (
    <Card
      title={t('acc.security.sessions.title')}
      actions={
        mayEnd && others.length > 0 ? (
          <Button variant="secondary" loading={endOthers.isPending} onClick={() => endOthers.mutate(others)}>
            {t('acc.security.sessions.endOthers')}
          </Button>
        ) : null
      }
    >
      <p className="hint">{t('acc.security.sessions.intro')}</p>
      {end.isError ? <Banner intent="danger">{errorText(t, end.error)}</Banner> : null}
      {endOthers.isError ? <Banner intent="danger">{errorText(t, endOthers.error)}</Banner> : null}
      {sessions.isError ? (
        <Banner intent="danger">{errorText(t, sessions.error)}</Banner>
      ) : (
        <DataTable
          caption={t('common.list.count', { count: rows.length })}
          columns={columns}
          rows={rows}
          rowKey={(s) => s.id ?? s.sessionRef}
          loading={sessions.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('common.state.empty')}
        />
      )}
    </Card>
  );
}

/**
 * Personal API tokens: listed without their secrets, created with a name and an optional expiry (the secret is shown
 * once, right after creation), and revoked at once. Creating needs its own permission; listing and revoking one's own
 * tokens are self-service.
 */
function ApiTokensCard({ t }: { t: Translate }) {
  const { format } = useI18n();
  const permissions = useSession((s) => s.me?.permissions);
  const mayRead = can(permissions, 'iam.auth_token.read');
  const mayCreate = can(permissions, 'iam.user_account.create_api_token');
  const mayRevoke = can(permissions, 'iam.auth_token.delete');
  const tokens = useMyApiTokens(mayRead);
  const create = useCreateApiToken();
  const revoke = useRevokeApiToken();
  const [name, setName] = useState('');
  const [days, setDays] = useState('90');
  const [touched, setTouched] = useState(false);
  if (!mayRead) return null;
  const lifetime = Number(days);
  const lifetimeValid = Number.isInteger(lifetime) && lifetime >= 1 && lifetime <= 365;
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setTouched(true);
    if (!name.trim() || !lifetimeValid) return;
    create.mutate(
      { name: name.trim(), expiresAt: new Date(Date.now() + lifetime * 86_400_000).toISOString() },
      {
        onSuccess: () => {
          setName('');
          setTouched(false);
        },
      },
    );
  };
  const columns: Column<AuthToken>[] = [
    { key: 'name', header: t('acc.security.tokens.name'), render: (row) => row.name ?? '—' },
    {
      key: 'expiresAt',
      header: t('acc.security.tokens.expires'),
      render: (row) => (row.expiresAt ? format.dateTime(row.expiresAt) : '—'),
    },
    {
      key: 'lastUsedAt',
      header: t('acc.security.tokens.lastUsed'),
      render: (row) => (row.lastUsedAt ? format.dateTime(row.lastUsedAt) : t('acc.security.tokens.never')),
    },
    ...(mayRevoke
      ? [
          {
            key: 'actions',
            header: t('adm.records.actions'),
            render: (row: AuthToken) => (
              <Button
                variant="ghost"
                loading={revoke.isPending && revoke.variables?.id === row.id}
                onClick={() => row.id && row.etag && revoke.mutate({ id: row.id, etag: row.etag })}
              >
                {t('acc.security.tokens.revoke')}
              </Button>
            ),
          },
        ]
      : []),
  ];
  return (
    <Card title={t('acc.security.tokens.title')}>
      <p className="hint">{t('acc.security.tokens.intro')}</p>
      {create.data ? (
        <Banner intent="warning" title={t('acc.security.tokens.copyNow')}>
          <code className="secret-once">{create.data.token}</code>
        </Banner>
      ) : null}
      {create.isError ? <Banner intent="danger">{errorText(t, create.error)}</Banner> : null}
      {revoke.isError ? <Banner intent="danger">{errorText(t, revoke.error)}</Banner> : null}
      {tokens.isError ? (
        <Banner intent="danger">{errorText(t, tokens.error)}</Banner>
      ) : (
        <DataTable
          caption={t('common.list.count', { count: tokens.data?.items.length ?? 0 })}
          columns={columns}
          rows={tokens.data?.items}
          rowKey={(row) => row.id ?? row.name ?? ''}
          loading={tokens.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('acc.security.tokens.empty')}
        />
      )}
      {mayCreate ? (
        <form className="form" onSubmit={submit} noValidate>
          <TextInput
            label={t('acc.security.tokens.name')}
            maxLength={120}
            value={name}
            error={touched && !name.trim() ? t('common.validation.required') : undefined}
            onChange={(e) => setName(e.target.value)}
          />
          <TextInput
            label={t('acc.security.tokens.days')}
            type="number"
            min={1}
            max={365}
            value={days}
            error={lifetimeValid ? undefined : t('acc.security.tokens.daysRange')}
            onChange={(e) => setDays(e.target.value)}
          />
          <div className="form__actions">
            <Button type="submit" loading={create.isPending}>
              {t('acc.security.tokens.create')}
            </Button>
          </div>
        </form>
      ) : null}
    </Card>
  );
}
