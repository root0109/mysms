import { TERMS_VERSION, useAcceptTerms, useI18n, useSession } from '@mysms/core';
import { Banner, Button, Card } from '@mysms/ui-web';
import type { ReactNode } from 'react';
import { Navigate } from 'react-router';
import { accountProblem } from './accountText';

/**
 * ACC-04 Accept terms: the terms statement and the one action that records acceptance (iam.AcceptTerms). As a route
 * it returns home once accepted; the shell also shows it in place of every page until the user has accepted
 * ({@link TermsGate}, ui/03 first-run flow).
 */
export function TermsScreen() {
  const accepted = useSession((s) => Boolean(s.me?.user?.termsAcceptedAt));
  if (accepted) return <Navigate to="/app" replace />;
  return <TermsCard />;
}

function TermsCard() {
  const { t } = useI18n();
  const accept = useAcceptTerms();
  return (
    <div className="page">
      <Card title={t('acc.terms.title')}>
        <p>{t('acc.terms.body')}</p>
        <p className="hint">{t('acc.terms.version', { version: TERMS_VERSION })}</p>
        {accept.isError ? <Banner intent="danger">{t(accountProblem(accept.error))}</Banner> : null}
        <div className="form__actions">
          <Button loading={accept.isPending} onClick={() => accept.mutate(TERMS_VERSION)}>
            {t('acc.terms.accept')}
          </Button>
        </div>
      </Card>
    </div>
  );
}

/** Blocks the signed-in pages until the user has accepted the terms; an administrator acting as them is not asked. */
export function TermsGate({ children }: { children: ReactNode }) {
  const pending = useSession((s) => Boolean(s.me?.user) && !s.me?.user?.termsAcceptedAt && !s.impersonation);
  return pending ? <TermsCard /> : children;
}
