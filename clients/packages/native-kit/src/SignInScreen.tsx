import { isApiError, useCompleteMfa, useI18n, useSession, useSignIn } from '@mysms/core';
import { Banner, Button, Screen, Text, TextInput } from '@mysms/ui-native';
import { useState } from 'react';

/** ACC-01 on phones and tablets, with the MFA step and the ways to a password reset and sign-up. */
export function SignInScreen({
  defaultTenantCode = '',
  tablet = false,
  onForgot,
  onSignUp,
  onBrowseCourses,
}: {
  defaultTenantCode?: string;
  tablet?: boolean;
  /** Opens the password reset (ACC-02). */
  onForgot: () => void;
  /** Opens sign-up (ACC-03), where the app offers it (the tablet learning app). */
  onSignUp?: () => void;
  /** Opens the course catalogue as a guest (LMS-01), where the app names its school. */
  onBrowseCourses?: () => void;
}) {
  const { t } = useI18n();
  const status = useSession((s) => s.status);
  const signIn = useSignIn();
  const completeMfa = useCompleteMfa();
  const [tenantCode, setTenantCode] = useState(defaultTenantCode);
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const failure = signIn.error ?? completeMfa.error;
  const required = (v: string) => (submitted && !v.trim() ? t('common.validation.required') : undefined);
  const submit = () => {
    setSubmitted(true);
    if (tenantCode.trim() && identifier.trim() && password)
      signIn.mutate({ tenantCode: tenantCode.trim(), identifier: identifier.trim(), password });
  };
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{status === 'mfaPending' ? t('acc.mfa.title') : t('acc.signIn.title')}</Text>
      {status === 'expired' ? <Banner intent="warning">{t('acc.session.expired')}</Banner> : null}
      {failure ? (
        <Banner intent="danger">
          {isApiError(failure) && (failure.status === 401 || failure.status === 400)
            ? t('acc.signIn.failed')
            : t('common.error.generic')}
        </Banner>
      ) : null}
      {status === 'mfaPending' ? (
        <>
          <TextInput
            label={t('acc.mfa.code')}
            keyboardType="number-pad"
            textContentType="oneTimeCode"
            value={code}
            onChangeText={setCode}
          />
          <Button loading={completeMfa.isPending} onPress={() => completeMfa.mutate(code.trim())}>
            {t('acc.mfa.submit')}
          </Button>
        </>
      ) : (
        <>
          <TextInput
            label={t('acc.signIn.tenant')}
            autoCapitalize="none"
            value={tenantCode}
            onChangeText={setTenantCode}
            error={required(tenantCode)}
          />
          <TextInput
            label={t('acc.signIn.identifier')}
            autoCapitalize="none"
            textContentType="username"
            value={identifier}
            onChangeText={setIdentifier}
            error={required(identifier)}
          />
          <TextInput
            label={t('acc.signIn.password')}
            secureTextEntry
            textContentType="password"
            value={password}
            onChangeText={setPassword}
            error={required(password)}
          />
          <Button loading={signIn.isPending} onPress={submit}>
            {t('acc.signIn.submit')}
          </Button>
          <Button variant="ghost" onPress={onForgot}>
            {t('acc.signIn.forgot')}
          </Button>
          {onSignUp ? (
            <Button variant="ghost" onPress={onSignUp}>
              {t('acc.signIn.signUp')}
            </Button>
          ) : null}
          {onBrowseCourses ? (
            <Button variant="ghost" onPress={onBrowseCourses}>
              {t('lms.native.browseCourses')}
            </Button>
          ) : null}
        </>
      )}
    </Screen>
  );
}
