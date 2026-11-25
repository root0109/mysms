import { useI18n, useMyLoans } from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, Skeleton } from '@mysms/ui-web';

/**
 * LIB-07 My loans: the loans on the signed-in member's own library card (a student or a member of staff) — each title
 * with when it was issued, when it is due and its state, newest first. A loan past its due date and not returned says
 * so in words.
 */
export function MyLoansScreen() {
  const { t, format, timeZone } = useI18n();
  const loans = useMyLoans();
  const today = new Intl.DateTimeFormat('en-CA', { timeZone }).format(new Date());
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LIB07.title')}</h1>
        <p className="page__subtitle">{t('lib.mine.intro')}</p>
      </header>
      {!loans.may ? <Banner intent="info">{t('adm.resource.noAccess')}</Banner> : null}
      {loans.isPending ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
      {loans.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={loans.refetch}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : null}
      {loans.may && !loans.isPending && !loans.isError && loans.cards.length === 0 ? (
        <Banner intent="info">{t('lib.mine.noCard')}</Banner>
      ) : null}
      {loans.cards.length > 0 && !loans.isPending && loans.loans.length === 0 ? (
        <p className="hint">{t('lib.mine.none')}</p>
      ) : null}
      {loans.loans.length > 0 ? (
        <Card title={t('lib.mine.loans')}>
          <ul className="leave-list">
            {loans.loans.map((loan) => {
              const due = typeof loan.dueOn === 'string' ? loan.dueOn : '';
              const overdue =
                due !== '' && due < today && (loan.status === 'issued' || loan.status === 'return_pending');
              return (
                <li key={loan.id} className="leave-list__item">
                  <div className="leave-list__what">
                    <span>{loans.titleOf.get(String(loan.mediaId)) || t('lib.mine.untitled')}</span>
                    <span className="hint">
                      {[
                        enumLabel(t, 'loanStatus', String(loan.status ?? '')),
                        typeof loan.issuedOn === 'string'
                          ? t('lib.mine.issued', { day: format.day(loan.issuedOn) })
                          : '',
                        due ? t('lib.mine.due', { day: format.day(due) }) : '',
                        overdue ? t('lib.mine.overdue') : '',
                      ]
                        .filter(Boolean)
                        .join(' · ')}
                    </span>
                  </div>
                </li>
              );
            })}
          </ul>
        </Card>
      ) : null}
    </div>
  );
}
