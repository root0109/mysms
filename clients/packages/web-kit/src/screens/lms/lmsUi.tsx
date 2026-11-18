import { useI18n, lmsFailureText } from '@mysms/core';
import { Banner, Skeleton } from '@mysms/ui-web';
import type { ReactNode } from 'react';
import { LmsRetryBanner } from './lmsShared';

/** The page frame of every LMS screen: its title, an optional subtitle and actions. */
export function LmsPage({
  title,
  subtitle,
  actions,
  children,
}: {
  title: string;
  subtitle?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        {subtitle ? <p className="page__subtitle">{subtitle}</p> : null}
        {actions}
      </header>
      {children}
    </div>
  );
}

/** Loading, failed or not-allowed: the states every LMS screen shows before its content. */
export function LmsState({
  title,
  allowed,
  loading,
  error,
  retry,
}: {
  title: string;
  allowed: boolean;
  loading: boolean;
  error: unknown;
  retry: () => void;
}): ReactNode {
  const { t } = useI18n();
  if (!allowed)
    return (
      <LmsPage title={title}>
        <Banner intent="info">{t('lms.access.denied')}</Banner>
      </LmsPage>
    );
  if (loading) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (error)
    return (
      <LmsPage title={title}>
        <LmsRetryBanner error={error} onRetry={retry} />
      </LmsPage>
    );
  return null;
}

/** A failed action under its control; nothing without an error. */
export function ActionError({ error }: { error: unknown }) {
  const { t } = useI18n();
  if (!error) return null;
  return (
    <p className="sms-field__error" role="alert">
      {lmsFailureText(t, error)}
    </p>
  );
}
