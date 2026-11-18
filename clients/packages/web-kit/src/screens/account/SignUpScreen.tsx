import { isApiError, signUpSchoolsNotice, useI18n, useSignUp, useSignUpInstitutions } from '@mysms/core';
import { Banner, Button, Card, SelectField, Switch, TextInput } from '@mysms/ui-web';
import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { accountProblem } from './accountText';

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/**
 * ACC-03 Sign up (LMS self-serve), where the school allows it. The student picks one of the tenant's schools, read when
 * the code is entered; the account's role reaches that school only. The answer looks the same whether or not the
 * address already has an account: either way the student gets an e-mail with a code that sets the password (ACC-02).
 */
export function SignUpScreen({ defaultTenantCode = '' }: { defaultTenantCode?: string }) {
  const { t } = useI18n();
  const signUp = useSignUp();
  const [tenantCode, setTenantCode] = useState(defaultTenantCode);
  const [schoolsCode, setSchoolsCode] = useState(defaultTenantCode.trim());
  const [institutionId, setInstitutionId] = useState('');
  const schools = useSignUpInstitutions(schoolsCode);
  const offered = schools.data ?? [];
  const school = institutionId || (offered.length === 1 ? (offered[0]?.id ?? '') : '');
  const notice = signUpSchoolsNotice(schoolsCode, schools);
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [accept, setAccept] = useState(false);
  const [touched, setTouched] = useState(false);
  const required = (value: string) =>
    touched && !value.trim() ? t('common.validation.required') : undefined;
  const serverField = (field: string) =>
    isApiError(signUp.error) ? signUp.error.fieldErrors()[field] : undefined;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!tenantCode.trim() || !school || !EMAIL.test(email.trim()) || !fullName.trim() || !accept) return;
    signUp.mutate({
      tenantCode: tenantCode.trim(),
      institutionId: school,
      email: email.trim(),
      fullName: fullName.trim(),
      acceptTerms: true,
      personaCategory: 'student',
    });
  };
  if (signUp.isSuccess)
    return (
      <main className="signin" id="main">
        <Card title={t('acc.signUp.sentTitle')}>
          <Banner intent="success">{t('acc.signUp.sent', { email: signUp.data.email ?? email })}</Banner>
          <Link to="/app/auth/reset">{t('acc.signUp.enterCode')}</Link>
        </Card>
      </main>
    );
  return (
    <main className="signin" id="main">
      <Card title={t('acc.signUp.title')}>
        <form className="form" noValidate onSubmit={submit}>
          <p className="hint">{t('acc.signUp.intro')}</p>
          <TextInput
            label={t('acc.signIn.tenant')}
            autoComplete="organization"
            value={tenantCode}
            error={required(tenantCode)}
            onChange={(e) => setTenantCode(e.target.value)}
            onBlur={() => {
              if (tenantCode.trim() === schoolsCode) return;
              setSchoolsCode(tenantCode.trim());
              setInstitutionId('');
            }}
          />
          <SelectField
            label={t('acc.signUp.school')}
            value={school}
            disabled={offered.length === 0}
            options={[
              { value: '', label: t('acc.signUp.schoolPlaceholder') },
              ...offered.map((s) => ({ value: s.id, label: s.name })),
            ]}
            hint={t(notice ?? 'acc.signUp.schoolHint')}
            error={
              touched && !school
                ? t('acc.signUp.schoolRequired')
                : serverField('institutionId')
                  ? t('acc.signUp.schoolInvalid')
                  : undefined
            }
            onChange={(e) => setInstitutionId(e.target.value)}
          />
          <TextInput
            label={t('acc.signUp.fullName')}
            autoComplete="name"
            maxLength={200}
            value={fullName}
            error={required(fullName) ?? serverField('fullName')}
            onChange={(e) => setFullName(e.target.value)}
          />
          <TextInput
            label={t('acc.signUp.email')}
            type="email"
            autoComplete="email"
            maxLength={320}
            value={email}
            error={touched && !EMAIL.test(email.trim()) ? t('acc.signUp.invalidEmail') : serverField('email')}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Switch
            label={t('acc.signUp.acceptTerms')}
            checked={accept}
            error={touched && !accept ? t('acc.signUp.termsRequired') : undefined}
            onChange={setAccept}
          />
          {signUp.isError ? (
            <Banner intent="danger">
              {t(
                isApiError(signUp.error) && signUp.error.status === 403
                  ? 'acc.signUp.closed'
                  : accountProblem(signUp.error),
              )}
            </Banner>
          ) : null}
          <div className="form__actions">
            <Button type="submit" loading={signUp.isPending}>
              {t('acc.signUp.submit')}
            </Button>
          </div>
        </form>
        <p className="hint">
          <Link to="/app/auth/login">{t('acc.signUp.haveAccount')}</Link>
        </p>
      </Card>
    </main>
  );
}
