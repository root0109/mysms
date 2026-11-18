import {
  TERMS_VERSION,
  isApiError,
  signUpSchoolsNotice,
  useAcceptTerms,
  useConfirmVerification,
  useI18n,
  useRequestPasswordReset,
  useRequestVerification,
  useResetPassword,
  useSession,
  useSignUp,
  useSignUpInstitutions,
  useSwitchRole,
  type RoleGrant,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, SwitchRow, Text, TextInput } from '@mysms/ui-native';
import { useState } from 'react';

/** The server's minimum password length (iam password policy). */
const MIN_PASSWORD = 12;
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/** The catalogue sentence for a failed account call (the same reading as the web screens). */
function problem(error: unknown): MessageKey {
  if (!isApiError(error)) return 'common.error.generic';
  if (error.errors.some((e) => e.code === 'INVALID_CODE')) return 'acc.code.invalid';
  if (error.code === 'NOT_CONFIGURED') return 'acc.code.notConfigured';
  if (error.status === 429) return 'acc.code.tooMany';
  if (error.status === 403) return 'acc.code.notAllowed';
  if (error.status === 409) return 'acc.code.alreadyDone';
  if (error.status === 422) return 'acc.code.checkFields';
  return 'common.error.generic';
}

/**
 * ACC-02 on phones and tablets: ask for a reset code (the answer is always the same sentence), then set a new
 * password with the code from the e-mail.
 */
export function PasswordResetScreen({
  defaultTenantCode = '',
  tablet = false,
  onBack,
}: {
  defaultTenantCode?: string;
  tablet?: boolean;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const ask = useRequestPasswordReset();
  const reset = useResetPassword();
  const [haveCode, setHaveCode] = useState(false);
  const [tenantCode, setTenantCode] = useState(defaultTenantCode);
  const [identifier, setIdentifier] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [repeat, setRepeat] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const required = (v: string) => (submitted && !v.trim() ? t('common.validation.required') : undefined);
  if (reset.isSuccess)
    return (
      <Screen tablet={tablet}>
        <Text variant="h1">{t('acc.reset.doneTitle')}</Text>
        <Banner intent="success">{t('acc.reset.done')}</Banner>
        <Button onPress={onBack}>{t('acc.reset.signInNow')}</Button>
      </Screen>
    );
  if (haveCode)
    return (
      <Screen tablet={tablet}>
        <Text variant="h1">{t('acc.reset.resetTitle')}</Text>
        <TextInput
          label={t('acc.reset.code')}
          autoCapitalize="none"
          textContentType="oneTimeCode"
          value={code}
          onChangeText={setCode}
          error={required(code)}
        />
        <TextInput
          label={t('acc.security.password.new')}
          secureTextEntry
          textContentType="newPassword"
          value={password}
          onChangeText={setPassword}
          error={
            (submitted || password.length > 0) && password.length < MIN_PASSWORD
              ? t('acc.security.password.tooShort', { min: MIN_PASSWORD })
              : undefined
          }
        />
        <TextInput
          label={t('acc.security.password.repeat')}
          secureTextEntry
          textContentType="newPassword"
          value={repeat}
          onChangeText={setRepeat}
          error={
            (submitted || repeat.length > 0) && repeat !== password
              ? t('acc.security.password.mismatch')
              : undefined
          }
        />
        {reset.isError ? <Banner intent="danger">{t(problem(reset.error))}</Banner> : null}
        <Button
          loading={reset.isPending}
          onPress={() => {
            setSubmitted(true);
            if (code.trim() && password.length >= MIN_PASSWORD && password === repeat)
              reset.mutate({ token: code.trim(), newPassword: password });
          }}
        >
          {t('acc.reset.resetSubmit')}
        </Button>
        <Button variant="ghost" onPress={onBack}>
          {t('acc.reset.backToSignIn')}
        </Button>
      </Screen>
    );
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('acc.reset.requestTitle')}</Text>
      <Text muted>{t('acc.reset.requestIntro')}</Text>
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
      {ask.isSuccess ? <Banner intent="success">{t('acc.reset.requested')}</Banner> : null}
      {ask.isError ? <Banner intent="danger">{t(problem(ask.error))}</Banner> : null}
      <Button
        loading={ask.isPending}
        onPress={() => {
          setSubmitted(true);
          if (tenantCode.trim() && identifier.trim())
            ask.mutate({ tenantCode: tenantCode.trim(), identifier: identifier.trim() });
        }}
      >
        {t('acc.reset.requestSubmit')}
      </Button>
      <Button
        variant="secondary"
        onPress={() => {
          setSubmitted(false);
          setHaveCode(true);
        }}
      >
        {t('acc.reset.haveCode')}
      </Button>
      <Button variant="ghost" onPress={onBack}>
        {t('acc.reset.backToSignIn')}
      </Button>
    </Screen>
  );
}

/** ACC-03 on the tablet learning app (B): self-service sign-up where the school allows it. */
export function SignUpScreen({
  defaultTenantCode = '',
  tablet = false,
  onBack,
  onEnterCode,
}: {
  defaultTenantCode?: string;
  tablet?: boolean;
  onBack: () => void;
  onEnterCode: () => void;
}) {
  const { t } = useI18n();
  const signUp = useSignUp();
  const [tenantCode, setTenantCode] = useState(defaultTenantCode);
  const [schoolsCode, setSchoolsCode] = useState(defaultTenantCode.trim());
  const [institutionId, setInstitutionId] = useState('');
  const schools = useSignUpInstitutions(schoolsCode);
  const offered = schools.data ?? [];
  const school = institutionId || (offered.length === 1 ? (offered[0]?.id ?? '') : '');
  const notice = signUpSchoolsNotice(schoolsCode, schools);
  const schoolRefused = isApiError(signUp.error) && signUp.error.fieldErrors()['institutionId'] !== undefined;
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [accept, setAccept] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const required = (v: string) => (submitted && !v.trim() ? t('common.validation.required') : undefined);
  if (signUp.isSuccess)
    return (
      <Screen tablet={tablet}>
        <Text variant="h1">{t('acc.signUp.sentTitle')}</Text>
        <Banner intent="success">{t('acc.signUp.sent', { email: signUp.data.email ?? email })}</Banner>
        <Button onPress={onEnterCode}>{t('acc.signUp.enterCode')}</Button>
      </Screen>
    );
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('acc.signUp.title')}</Text>
      <Text muted>{t('acc.signUp.intro')}</Text>
      <TextInput
        label={t('acc.signIn.tenant')}
        autoCapitalize="none"
        value={tenantCode}
        onChangeText={setTenantCode}
        onEndEditing={() => {
          if (tenantCode.trim() === schoolsCode) return;
          setSchoolsCode(tenantCode.trim());
          setInstitutionId('');
        }}
        error={required(tenantCode)}
      />
      {offered.length > 0 ? (
        <ChoiceGroup
          label={t('acc.signUp.school')}
          hint={t('acc.signUp.schoolHint')}
          choices={offered.map((s) => ({ value: s.id, label: s.name }))}
          value={school}
          onChange={setInstitutionId}
        />
      ) : null}
      {notice ? (
        <Banner
          intent={
            notice === 'acc.signUp.schoolNeedsCode' || notice === 'acc.signUp.schoolsLoading'
              ? 'info'
              : 'warning'
          }
        >
          {t(notice)}
        </Banner>
      ) : null}
      {submitted && !school && offered.length > 0 ? (
        <Banner intent="warning">{t('acc.signUp.schoolRequired')}</Banner>
      ) : null}
      {schoolRefused ? <Banner intent="warning">{t('acc.signUp.schoolInvalid')}</Banner> : null}
      <TextInput
        label={t('acc.signUp.fullName')}
        textContentType="name"
        value={fullName}
        onChangeText={setFullName}
        error={required(fullName)}
      />
      <TextInput
        label={t('acc.signUp.email')}
        autoCapitalize="none"
        keyboardType="email-address"
        textContentType="emailAddress"
        value={email}
        onChangeText={setEmail}
        error={submitted && !EMAIL.test(email.trim()) ? t('acc.signUp.invalidEmail') : undefined}
      />
      <SwitchRow label={t('acc.signUp.acceptTerms')} value={accept} onChange={setAccept} />
      {submitted && !accept ? <Banner intent="warning">{t('acc.signUp.termsRequired')}</Banner> : null}
      {signUp.isError ? (
        <Banner intent="danger">
          {t(
            isApiError(signUp.error) && signUp.error.status === 403
              ? 'acc.signUp.closed'
              : problem(signUp.error),
          )}
        </Banner>
      ) : null}
      <Button
        loading={signUp.isPending}
        onPress={() => {
          setSubmitted(true);
          if (tenantCode.trim() && school && fullName.trim() && EMAIL.test(email.trim()) && accept)
            signUp.mutate({
              tenantCode: tenantCode.trim(),
              institutionId: school,
              email: email.trim(),
              fullName: fullName.trim(),
              acceptTerms: true,
              personaCategory: 'student',
            });
        }}
      >
        {t('acc.signUp.submit')}
      </Button>
      <Button variant="ghost" onPress={onBack}>
        {t('acc.signUp.haveAccount')}
      </Button>
    </Screen>
  );
}

/** ACC-04 on phones and tablets: shown in place of the app until the terms are accepted. */
export function TermsScreen({ tablet = false }: { tablet?: boolean }) {
  const { t } = useI18n();
  const accept = useAcceptTerms();
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('acc.terms.title')}</Text>
      <Text>{t('acc.terms.body')}</Text>
      <Text muted>{t('acc.terms.version', { version: TERMS_VERSION })}</Text>
      {accept.isError ? <Banner intent="danger">{t(problem(accept.error))}</Banner> : null}
      <Button loading={accept.isPending} onPress={() => accept.mutate(TERMS_VERSION)}>
        {t('acc.terms.accept')}
      </Button>
    </Screen>
  );
}

/** ACC-05 on phones and tablets: send a code to the account's e-mail address and confirm it. */
export function VerifyScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  const user = useSession((s) => s.me?.user);
  const send = useRequestVerification();
  const confirm = useConfirmVerification();
  const [code, setCode] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const verified = confirm.isSuccess || Boolean(user?.emailVerifiedAt);
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('acc.verify.title')}</Text>
      {verified ? (
        <Banner intent="success">{t('acc.verify.done')}</Banner>
      ) : user?.email ? (
        <>
          <Text muted>{t('acc.verify.intro', { email: user.email })}</Text>
          {send.isSuccess ? <Banner intent="success">{t('acc.verify.sent')}</Banner> : null}
          {send.isError ? <Banner intent="danger">{t(problem(send.error))}</Banner> : null}
          <Button variant="secondary" loading={send.isPending} onPress={() => send.mutate()}>
            {t('acc.verify.send')}
          </Button>
          <TextInput
            label={t('acc.verify.code')}
            autoCapitalize="none"
            textContentType="oneTimeCode"
            value={code}
            onChangeText={setCode}
            error={submitted && !code.trim() ? t('common.validation.required') : undefined}
          />
          {confirm.isError ? <Banner intent="danger">{t(problem(confirm.error))}</Banner> : null}
          <Button
            loading={confirm.isPending}
            onPress={() => {
              setSubmitted(true);
              if (code.trim()) confirm.mutate(code.trim());
            }}
          >
            {t('acc.verify.confirm')}
          </Button>
        </>
      ) : (
        <Banner intent="info">{t('acc.verify.noEmail')}</Banner>
      )}
      <Text muted>{t('acc.verify.mobileUnavailable')}</Text>
      <Button variant="ghost" onPress={onBack}>
        {t('common.action.back')}
      </Button>
    </Screen>
  );
}

const grantKey = (g: Pick<RoleGrant, 'roleId' | 'scopeId'>) => `${g.roleId ?? ''}/${g.scopeId ?? ''}`;

/** ACC-06 on phones and tablets: the role grants to work in (iam.SwitchRole); the app follows the chosen one. */
export function PersonaScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  const roles = useSession((s) => s.me?.roles ?? []);
  const active = useSession((s) => s.me?.activeRole);
  const switchRole = useSwitchRole();
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('acc.persona.title')}</Text>
      <Text muted>{t('acc.persona.intro')}</Text>
      {switchRole.isError ? <Banner intent="danger">{t('acc.persona.failed')}</Banner> : null}
      {roles.map((grant) => {
        const current = Boolean(active) && grantKey(grant) === grantKey(active ?? {});
        return (
          <Button
            key={grantKey(grant)}
            variant={current ? 'primary' : 'secondary'}
            disabled={!grant.roleId}
            loading={switchRole.isPending && switchRole.variables?.roleId === grant.roleId}
            onPress={() =>
              switchRole.mutate(
                { roleId: grant.roleId as string, ...(grant.scopeId ? { scopeId: grant.scopeId } : {}) },
                { onSuccess: onBack },
              )
            }
          >
            {grant.scopeType === 'tenant'
              ? t('acc.persona.tenantWide', { role: grant.roleCode })
              : t('acc.persona.scoped', { role: grant.roleCode, scope: grant.scopeType })}
          </Button>
        );
      })}
      <Button variant="ghost" onPress={onBack}>
        {t('common.action.back')}
      </Button>
    </Screen>
  );
}
