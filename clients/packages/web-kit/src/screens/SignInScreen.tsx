import { zodResolver } from '@hookform/resolvers/zod';
import { isApiError, useCompleteMfa, useI18n, useSession, useSignIn } from '@mysms/core';
import { Banner, Button, Card, TextInput } from '@mysms/ui-web';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useSearchParams } from 'react-router';
import { z } from 'zod';
import { safeNext } from '../routing/safeNext';

const credentials = z.object({
  tenantCode: z.string().trim().min(1),
  identifier: z.string().trim().min(1),
  password: z.string().min(1),
});
const mfa = z.object({
  code: z
    .string()
    .trim()
    .regex(/^\d{6,8}$/),
});

/** ACC-01 Sign in, with the MFA step when the account requires it, and the ways to a reset (ACC-02) and sign-up. */
export function SignInScreen({ defaultTenantCode = '' }: { defaultTenantCode?: string }) {
  const { t } = useI18n();
  const status = useSession((s) => s.status);
  const [params] = useSearchParams();
  const location = useLocation();
  const next = safeNext(params.get('next'));
  const signIn = useSignIn();
  const completeMfa = useCompleteMfa();
  const expired = (location.state as { expired?: boolean } | null)?.expired === true;

  const credentialsForm = useForm<z.infer<typeof credentials>>({
    resolver: zodResolver(credentials),
    defaultValues: { tenantCode: defaultTenantCode, identifier: '', password: '' },
  });
  const mfaForm = useForm<z.infer<typeof mfa>>({ resolver: zodResolver(mfa), defaultValues: { code: '' } });

  if (status === 'signedIn') return <Navigate to={next} replace />;

  const failure = signIn.error ?? completeMfa.error;
  const failureText = failure
    ? isApiError(failure) && (failure.status === 401 || failure.status === 400)
      ? t('acc.signIn.failed')
      : t('common.error.generic')
    : null;
  const traceId = failure && isApiError(failure) ? failure.traceId : undefined;
  const required = t('common.validation.required');

  return (
    <main className="signin" id="main">
      <Card title={status === 'mfaPending' ? t('acc.mfa.title') : t('acc.signIn.title')}>
        {expired ? <Banner intent="warning">{t('acc.session.expired')}</Banner> : null}
        {failureText ? (
          <Banner intent="danger">
            {failureText}
            {traceId ? <span className="signin__trace"> {t('common.error.trace', { traceId })}</span> : null}
          </Banner>
        ) : null}
        {status === 'mfaPending' ? (
          <form noValidate onSubmit={mfaForm.handleSubmit((v) => completeMfa.mutate(v.code))}>
            <TextInput
              label={t('acc.mfa.code')}
              inputMode="numeric"
              autoComplete="one-time-code"
              error={mfaForm.formState.errors.code ? t('common.validation.pattern') : undefined}
              {...mfaForm.register('code')}
            />
            <Button type="submit" loading={completeMfa.isPending}>
              {t('acc.mfa.submit')}
            </Button>
          </form>
        ) : (
          <form noValidate onSubmit={credentialsForm.handleSubmit((v) => signIn.mutate(v))}>
            <TextInput
              label={t('acc.signIn.tenant')}
              autoComplete="organization"
              error={credentialsForm.formState.errors.tenantCode ? required : undefined}
              {...credentialsForm.register('tenantCode')}
            />
            <TextInput
              label={t('acc.signIn.identifier')}
              autoComplete="username"
              error={credentialsForm.formState.errors.identifier ? required : undefined}
              {...credentialsForm.register('identifier')}
            />
            <TextInput
              label={t('acc.signIn.password')}
              type="password"
              autoComplete="current-password"
              error={credentialsForm.formState.errors.password ? required : undefined}
              {...credentialsForm.register('password')}
            />
            <Button type="submit" loading={signIn.isPending}>
              {t('acc.signIn.submit')}
            </Button>
            <p className="hint">
              <Link to="/app/auth/reset">{t('acc.signIn.forgot')}</Link>
            </p>
            <p className="hint">
              <Link to="/app/auth/signup">{t('acc.signIn.signUp')}</Link>
            </p>
          </form>
        )}
      </Card>
    </main>
  );
}
