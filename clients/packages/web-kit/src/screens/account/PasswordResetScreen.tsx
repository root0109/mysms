import { useI18n, useRequestPasswordReset, useResetPassword } from '@mysms/core';
import { Banner, Button, Card, TextInput } from '@mysms/ui-web';
import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { MIN_PASSWORD, accountProblem } from './accountText';

/**
 * ACC-02 Forgot / reset password. Without a code it asks for one by e-mail and always answers the same sentence
 * (the server never tells whether an account exists); with the code from the e-mail (the link fills it in) it sets
 * the new password, after which every session of the account has ended and the user signs in again.
 */
export function PasswordResetScreen({ defaultTenantCode = '' }: { defaultTenantCode?: string }) {
  const { t } = useI18n();
  const [params] = useSearchParams();
  const [haveCode, setHaveCode] = useState(Boolean(params.get('token')));
  return (
    <main className="signin" id="main">
      {haveCode ? (
        <ResetForm token={params.get('token') ?? ''} />
      ) : (
        <RequestForm defaultTenantCode={defaultTenantCode} onHaveCode={() => setHaveCode(true)} />
      )}
      <p className="hint">
        <Link to="/app/auth/login">{t('acc.reset.backToSignIn')}</Link>
      </p>
    </main>
  );
}

function RequestForm({
  defaultTenantCode,
  onHaveCode,
}: {
  defaultTenantCode: string;
  onHaveCode: () => void;
}) {
  const { t } = useI18n();
  const ask = useRequestPasswordReset();
  const [tenantCode, setTenantCode] = useState(defaultTenantCode);
  const [identifier, setIdentifier] = useState('');
  const [touched, setTouched] = useState(false);
  const required = (value: string) =>
    touched && !value.trim() ? t('common.validation.required') : undefined;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (tenantCode.trim() && identifier.trim())
      ask.mutate({ tenantCode: tenantCode.trim(), identifier: identifier.trim() });
  };
  return (
    <Card title={t('acc.reset.requestTitle')}>
      <form className="form" noValidate onSubmit={submit}>
        <p className="hint">{t('acc.reset.requestIntro')}</p>
        <TextInput
          label={t('acc.signIn.tenant')}
          autoComplete="organization"
          value={tenantCode}
          error={required(tenantCode)}
          onChange={(e) => setTenantCode(e.target.value)}
        />
        <TextInput
          label={t('acc.signIn.identifier')}
          autoComplete="username"
          value={identifier}
          error={required(identifier)}
          onChange={(e) => setIdentifier(e.target.value)}
        />
        {ask.isSuccess ? <Banner intent="success">{t('acc.reset.requested')}</Banner> : null}
        {ask.isError ? <Banner intent="danger">{t(accountProblem(ask.error))}</Banner> : null}
        <div className="form__actions">
          <Button type="submit" loading={ask.isPending}>
            {t('acc.reset.requestSubmit')}
          </Button>
          <Button type="button" variant="ghost" onClick={onHaveCode}>
            {t('acc.reset.haveCode')}
          </Button>
        </div>
      </form>
    </Card>
  );
}

function ResetForm({ token: given }: { token: string }) {
  const { t } = useI18n();
  const reset = useResetPassword();
  const [token, setToken] = useState(given);
  const [next, setNext] = useState('');
  const [repeat, setRepeat] = useState('');
  const [touched, setTouched] = useState(false);
  const tooShort = (touched || next.length > 0) && next.length < MIN_PASSWORD;
  const mismatch = (touched || repeat.length > 0) && repeat !== next;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (token.trim() && next.length >= MIN_PASSWORD && next === repeat)
      reset.mutate({ token: token.trim(), newPassword: next });
  };
  if (reset.isSuccess)
    return (
      <Card title={t('acc.reset.doneTitle')}>
        <Banner intent="success">{t('acc.reset.done')}</Banner>
        <Link to="/app/auth/login">{t('acc.reset.signInNow')}</Link>
      </Card>
    );
  return (
    <Card title={t('acc.reset.resetTitle')}>
      <form className="form" noValidate onSubmit={submit}>
        {given ? null : (
          <TextInput
            label={t('acc.reset.code')}
            autoComplete="one-time-code"
            value={token}
            error={touched && !token.trim() ? t('common.validation.required') : undefined}
            onChange={(e) => setToken(e.target.value)}
          />
        )}
        <TextInput
          label={t('acc.security.password.new')}
          type="password"
          autoComplete="new-password"
          value={next}
          hint={t('acc.security.password.rule', { min: MIN_PASSWORD })}
          error={tooShort ? t('acc.security.password.tooShort', { min: MIN_PASSWORD }) : undefined}
          onChange={(e) => setNext(e.target.value)}
        />
        <TextInput
          label={t('acc.security.password.repeat')}
          type="password"
          autoComplete="new-password"
          value={repeat}
          error={mismatch ? t('acc.security.password.mismatch') : undefined}
          onChange={(e) => setRepeat(e.target.value)}
        />
        {reset.isError ? <Banner intent="danger">{t(accountProblem(reset.error))}</Banner> : null}
        <div className="form__actions">
          <Button type="submit" loading={reset.isPending}>
            {t('acc.reset.resetSubmit')}
          </Button>
        </div>
      </form>
    </Card>
  );
}
