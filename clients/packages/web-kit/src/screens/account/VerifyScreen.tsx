import { useConfirmVerification, useI18n, useRequestVerification, useSession } from '@mysms/core';
import { Banner, Button, Card, TextInput } from '@mysms/ui-web';
import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { accountProblem } from './accountText';

/**
 * ACC-05 Verify e-mail: a signed-in user asks for a code (iam.RequestVerification); the code from the e-mail — or its
 * link, which also works without a session — confirms the address (iam.ConfirmVerification). Mobile numbers cannot be
 * verified yet: no SMS provider is configured, and the screen says so.
 */
export function VerifyScreen() {
  const { t } = useI18n();
  const [params] = useSearchParams();
  const signedIn = useSession((s) => s.status === 'signedIn');
  const user = useSession((s) => s.me?.user);
  const send = useRequestVerification();
  const confirm = useConfirmVerification();
  const [code, setCode] = useState(params.get('code') ?? '');
  const [touched, setTouched] = useState(false);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (code.trim()) confirm.mutate(code.trim());
  };
  const verified = confirm.isSuccess || Boolean(user?.emailVerifiedAt);
  return (
    <main className="signin" id="main">
      <Card title={t('acc.verify.title')}>
        {verified ? (
          <Banner intent="success">{t('acc.verify.done')}</Banner>
        ) : (
          <>
            {signedIn ? (
              user?.email ? (
                <>
                  <p className="hint">{t('acc.verify.intro', { email: user.email })}</p>
                  {send.isSuccess ? <Banner intent="success">{t('acc.verify.sent')}</Banner> : null}
                  {send.isError ? <Banner intent="danger">{t(accountProblem(send.error))}</Banner> : null}
                  <Button variant="secondary" loading={send.isPending} onClick={() => send.mutate()}>
                    {t('acc.verify.send')}
                  </Button>
                </>
              ) : (
                <Banner intent="info">{t('acc.verify.noEmail')}</Banner>
              )
            ) : null}
            <form className="form" noValidate onSubmit={submit}>
              <TextInput
                label={t('acc.verify.code')}
                autoComplete="one-time-code"
                value={code}
                error={touched && !code.trim() ? t('common.validation.required') : undefined}
                onChange={(e) => setCode(e.target.value)}
              />
              {confirm.isError ? <Banner intent="danger">{t(accountProblem(confirm.error))}</Banner> : null}
              <div className="form__actions">
                <Button type="submit" loading={confirm.isPending}>
                  {t('acc.verify.confirm')}
                </Button>
              </div>
            </form>
            <p className="hint">{t('acc.verify.mobileUnavailable')}</p>
          </>
        )}
        <Link to={signedIn ? '/app' : '/app/auth/login'}>
          {t(signedIn ? 'acc.verify.continue' : 'acc.reset.backToSignIn')}
        </Link>
      </Card>
    </main>
  );
}
