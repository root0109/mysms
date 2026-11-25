import {
  consentActions,
  consentFailureKey,
  consentSubjects,
  refusalScope,
  useCan,
  useI18n,
  useMyChildren,
  useMyConsents,
  useMyNotice,
  useRecordMyConsent,
  useWithdrawMyConsent,
  type ConsentDecision,
  type ConsentScope,
  type ConsentSubject,
  type MyConsent,
} from '@mysms/core';
import { CATALOGS, enumLabel, type MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  Dialog,
  SegmentedControl,
  SelectField,
  Skeleton,
  StatusChip,
  type StatusChipProps,
} from '@mysms/ui-web';
import { useMemo, useState } from 'react';

const STATUS_INTENT: Readonly<Record<NonNullable<MyConsent['myStatus']>, StatusChipProps['intent']>> = {
  granted: 'success',
  refused: 'neutral',
  not_answered: 'warning',
  reconsent_required: 'warning',
  expired: 'neutral',
};

/** The languages a notice can be asked for: those the app has a catalogue for (the server falls back to English). */
const NOTICE_LANGUAGES = Object.keys(CATALOGS);

/**
 * ACC-13 Consent centre (ui/02; design PRIVACY-AND-CONSENT-CENTRE §12): one card per purpose the school asks about,
 * per person the user sees — themself, and each child they are the legal guardian of (the switcher). A card shows
 * the user's own answer and the combined effect ("currently shared with"), never another guardian's answer; the
 * notice is read before deciding, and "Allow" and "Don't allow" weigh the same with nothing chosen in advance. A
 * student under 18 sees their own cards read only, with the day they start deciding.
 */
export function ConsentCentreScreen() {
  const { t } = useI18n();
  const consents = useMyConsents();
  const subjects = useMemo(() => consentSubjects(consents.data?.data ?? []), [consents.data]);
  const mayListChildren = useCan('sis.guardian.list_my_children');
  const children = useMyChildren(mayListChildren && subjects.some((s) => s.capacity === 'guardian'));
  const [chosen, setChosen] = useState<string | undefined>(undefined);

  if (consents.isPending) return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (consents.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void consents.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );

  const names = new Map(
    (children.data?.items ?? []).map((card) => [card.person.id ?? '', card.person.displayName ?? '']),
  );
  const label = subjectLabels(subjects, names, t);
  const current = subjects.find((s) => s.subjectPersonId === chosen) ?? subjects[0];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.consentCentre.title')}</h1>
        <p className="page__subtitle">{t('privacy.consentCentre.intro')}</p>
      </header>
      {current === undefined ? (
        <Card>
          <p>{t('privacy.consentCentre.empty')}</p>
        </Card>
      ) : (
        <>
          {subjects.length > 1 ? (
            <SegmentedControl
              label={t('privacy.consentCentre.subjectLabel')}
              options={subjects.map((s) => ({ value: s.subjectPersonId, label: label(s) }))}
              value={current.subjectPersonId}
              onChange={setChosen}
            />
          ) : null}
          <div className="page__grid">
            {current.rows.map((row) => (
              <ConsentCard key={`${row.subjectPersonId}|${row.purposeCode}`} row={row} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

/** "You" for the user's own cards; a child's name from sis.ListMyChildren, else "Child n" in the server's order. */
function subjectLabels(
  subjects: readonly ConsentSubject[],
  names: ReadonlyMap<string, string>,
  t: (key: MessageKey, values?: Record<string, number>) => string,
) {
  const children = subjects.filter((s) => s.capacity === 'guardian').map((s) => s.subjectPersonId);
  return (subject: ConsentSubject): string =>
    subject.capacity === 'guardian'
      ? names.get(subject.subjectPersonId) ||
        t('privacy.consentCentre.subject.child', { number: children.indexOf(subject.subjectPersonId) + 1 })
      : t('privacy.consentCentre.subject.self');
}

/** One purpose for one person: the answer, its effect, and the ways to decide, change or withdraw it. */
function ConsentCard({ row }: { row: MyConsent }) {
  const { t, format } = useI18n();
  const mayDecide = useCan('privacy.my_consent.create');
  const mayWithdraw = useCan('privacy.my_consent.withdraw');
  const actions = consentActions(row);
  const withdraw = useWithdrawMyConsent();
  const [deciding, setDeciding] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [done, setDone] = useState<MessageKey | null>(null);
  const minor = row.capacity === 'minor_self';
  return (
    <Card
      title={enumLabel(t, 'privacyPurpose', row.purposeCode)}
      actions={
        row.myStatus ? (
          <StatusChip
            intent={STATUS_INTENT[row.myStatus]}
            label={enumLabel(t, 'myConsentStatus', row.myStatus)}
          />
        ) : undefined
      }
    >
      <div className="consent-card">
        <p>
          {row.effectiveWidestScope
            ? t('privacy.consentCentre.sharedWith', {
                scope: enumLabel(t, 'consentScope', row.effectiveWidestScope),
              })
            : t('privacy.consentCentre.notShared')}
        </p>
        {minor && row.decidesFrom ? (
          <p>{t('privacy.consentCentre.minor', { date: format.day(row.decidesFrom) })}</p>
        ) : null}
        {row.decidedAt ? (
          <p className="hint">
            {t('privacy.consentCentre.answeredOn', { date: format.date(row.decidedAt) })}
          </p>
        ) : null}
        {!minor && !actions.decide ? <p className="hint">{t('privacy.consentCentre.noNotice')}</p> : null}
        {done ? <Banner intent="success">{t(done)}</Banner> : null}
        {withdraw.error ? <Banner intent="danger">{t(consentFailureKey(withdraw.error))}</Banner> : null}
        <div className="consent-card__actions">
          {mayDecide && actions.decide ? (
            <Button
              variant="secondary"
              onClick={() => {
                setDone(null);
                setDeciding(true);
              }}
            >
              {row.id ? t('privacy.consentCentre.change') : t('privacy.consentCentre.decide')}
            </Button>
          ) : null}
          {mayWithdraw && actions.withdraw ? (
            <Button
              variant="ghost"
              onClick={() => {
                setDone(null);
                withdraw.reset();
                setConfirming(true);
              }}
            >
              {t('privacy.consentCentre.withdraw')}
            </Button>
          ) : null}
        </div>
      </div>
      {deciding ? (
        <DecisionDialog
          row={row}
          onClose={() => setDeciding(false)}
          onSaved={() => {
            setDeciding(false);
            setDone('privacy.consentCentre.saved');
          }}
        />
      ) : null}
      <Dialog
        open={confirming}
        title={t('privacy.consentCentre.withdrawTitle')}
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={withdraw.isPending}
              onClick={() =>
                withdraw.mutate(
                  { consentRecordId: row.id ?? '', etag: row.etag },
                  {
                    onSettled: () => setConfirming(false),
                    onSuccess: () => setDone('privacy.consentCentre.withdrawn'),
                  },
                )
              }
            >
              {t('privacy.consentCentre.withdrawConfirm')}
            </Button>
          </>
        }
      >
        <p>{t('privacy.consentCentre.withdrawBody')}</p>
      </Dialog>
    </Card>
  );
}

/**
 * The notice in the chosen language, then the decision under exactly the version and language shown: a width purpose
 * asks how far it may be shared before "Allow" ("Don't allow" refuses it everywhere); nothing is chosen in advance.
 */
function DecisionDialog({
  row,
  onClose,
  onSaved,
}: {
  row: MyConsent;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { t, format, locale } = useI18n();
  const [language, setLanguage] = useState(NOTICE_LANGUAGES.includes(locale) ? locale : 'en');
  const notice = useMyNotice(row.noticeCode ?? null, language);
  const record = useRecordMyConsent();
  const widths = row.allowedScopes.length > 1;
  const [scope, setScope] = useState<ConsentScope | undefined>(widths ? undefined : row.allowedScopes[0]);
  const [missingScope, setMissingScope] = useState(false);
  const decide = (decision: ConsentDecision) => {
    const shown = notice.data;
    const sent = decision === 'granted' ? scope : refusalScope(row);
    if (!shown) return;
    if (!sent) {
      setMissingScope(true);
      return;
    }
    record.mutate(
      {
        subjectPersonId: row.subjectPersonId,
        purposeCode: row.purposeCode,
        scope: sent,
        decision,
        notice: shown,
      },
      { onSuccess: onSaved },
    );
  };
  return (
    <Dialog
      open
      title={enumLabel(t, 'privacyPurpose', row.purposeCode)}
      onClose={onClose}
      footer={
        <>
          <Button
            variant="secondary"
            disabled={!notice.data}
            loading={record.isPending && record.variables?.decision === 'refused'}
            onClick={() => decide('refused')}
          >
            {t('privacy.consentCentre.refuse')}
          </Button>
          <Button
            variant="secondary"
            disabled={!notice.data}
            loading={record.isPending && record.variables?.decision === 'granted'}
            onClick={() => decide('granted')}
          >
            {t('privacy.consentCentre.allow')}
          </Button>
        </>
      }
    >
      <div className="consent-card">
        <h3>{t('privacy.consentCentre.notice.title')}</h3>
        <SelectField
          label={t('privacy.consentCentre.notice.language')}
          options={NOTICE_LANGUAGES.map((code) => ({
            value: code,
            label: t(`setting.value.${code}` as MessageKey),
          }))}
          value={language}
          onChange={(event) => setLanguage(event.currentTarget.value)}
        />
        {notice.isPending ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
        {notice.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void notice.refetch()}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {t(consentFailureKey(notice.error))}
          </Banner>
        ) : null}
        {notice.data ? (
          <>
            {notice.data.language !== language ? (
              <Banner intent="info">{t('privacy.consentCentre.notice.fallback')}</Banner>
            ) : null}
            <p className="hint">
              {t('privacy.consentCentre.notice.version', {
                version: notice.data.version,
                date: format.date(notice.data.publishedAt),
              })}
            </p>
            {row.capacity === 'guardian' && notice.data.summaryForChildren ? (
              <section>
                <h4>{t('privacy.consentCentre.notice.forChildren')}</h4>
                <p className="consent-notice">{notice.data.summaryForChildren}</p>
              </section>
            ) : null}
            <p className="consent-notice">{notice.data.bodyMarkdown}</p>
            <section>
              <h4>{t('privacy.consentCentre.notice.grievance')}</h4>
              <p>
                {[
                  notice.data.grievanceContact.name,
                  notice.data.grievanceContact.role,
                  notice.data.grievanceContact.email,
                  notice.data.grievanceContact.phone,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
            </section>
            {widths ? (
              <>
                <SegmentedControl
                  label={t('privacy.consentCentre.scopeLabel')}
                  options={row.allowedScopes.map((value) => ({
                    value,
                    label: enumLabel(t, 'consentScope', value),
                  }))}
                  value={scope}
                  onChange={(value) => {
                    setScope(value);
                    setMissingScope(false);
                  }}
                />
                {missingScope ? <p role="alert">{t('privacy.consentCentre.chooseScope')}</p> : null}
              </>
            ) : null}
          </>
        ) : null}
        {record.error ? <Banner intent="danger">{t(consentFailureKey(record.error))}</Banner> : null}
      </div>
    </Dialog>
  );
}
