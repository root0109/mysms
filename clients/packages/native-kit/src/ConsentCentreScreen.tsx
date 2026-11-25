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
  type MyConsent,
} from '@mysms/core';
import { CATALOGS, enumLabel, type MessageKey } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, Text, useTheme } from '@mysms/ui-native';
import { useMemo, useState } from 'react';
import { View } from 'react-native';

/** A row's identity: the person it is about and the purpose. */
const rowKey = (row: MyConsent) => `${row.subjectPersonId}|${row.purposeCode}`;

/** The languages a notice can be asked for: those the app has a catalogue for (the server falls back to English). */
const NOTICE_LANGUAGES = Object.keys(CATALOGS);

/**
 * ACC-13 Consent centre on the phone and tablet (ui/02; design PRIVACY-AND-CONSENT-CENTRE §12): the web screen's
 * rules — one card per purpose for the chosen person (the child switcher for a parent), the user's own answer and
 * the combined effect, never another guardian's answer, a student under 18 read only — with the notice and the
 * decision on a page of their own instead of a dialog.
 */
export function ConsentCentreScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  const consents = useMyConsents();
  const subjects = useMemo(() => consentSubjects(consents.data?.data ?? []), [consents.data]);
  const mayListChildren = useCan('sis.guardian.list_my_children');
  const children = useMyChildren(mayListChildren && subjects.some((s) => s.capacity === 'guardian'));
  const [chosen, setChosen] = useState<string | undefined>(undefined);
  // the row being decided, by subject and purpose: the page always shows its live state
  const [deciding, setDeciding] = useState<string | null>(null);
  const [done, setDone] = useState<MessageKey | null>(null);

  const decidingRow = subjects.flatMap((s) => s.rows).find((row) => rowKey(row) === deciding);
  if (decidingRow)
    return (
      <DecisionPage
        tablet={tablet}
        row={decidingRow}
        onBack={() => setDeciding(null)}
        onSaved={() => {
          setDeciding(null);
          setDone('privacy.consentCentre.saved');
        }}
      />
    );

  const names = new Map(
    (children.data?.items ?? []).map((card) => [card.person.id ?? '', card.person.displayName ?? '']),
  );
  const childIds = subjects.filter((s) => s.capacity === 'guardian').map((s) => s.subjectPersonId);
  const label = (subjectPersonId: string, capacity: string) =>
    capacity === 'guardian'
      ? names.get(subjectPersonId) ||
        t('privacy.consentCentre.subject.child', { number: childIds.indexOf(subjectPersonId) + 1 })
      : t('privacy.consentCentre.subject.self');
  const current = subjects.find((s) => s.subjectPersonId === chosen) ?? subjects[0];
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('privacy.consentCentre.title')}
      </Text>
      <Text muted>{t('privacy.consentCentre.intro')}</Text>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      {consents.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {consents.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void consents.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {consents.data && current === undefined ? <Text>{t('privacy.consentCentre.empty')}</Text> : null}
      {done ? <Banner intent="success">{t(done)}</Banner> : null}
      {current && subjects.length > 1 ? (
        <ChoiceGroup
          label={t('privacy.consentCentre.subjectLabel')}
          choices={subjects.map((s) => ({
            value: s.subjectPersonId,
            label: label(s.subjectPersonId, s.capacity),
          }))}
          value={current.subjectPersonId}
          onChange={setChosen}
        />
      ) : null}
      {(current?.rows ?? []).map((row) => (
        <ConsentCard
          key={rowKey(row)}
          row={row}
          onDecide={() => {
            setDone(null);
            setDeciding(rowKey(row));
          }}
          onWithdrawn={() => setDone('privacy.consentCentre.withdrawn')}
        />
      ))}
    </Screen>
  );
}

/** One purpose for one person: the answer, its effect, and the ways to decide, change or withdraw it. */
function ConsentCard({
  row,
  onDecide,
  onWithdrawn,
}: {
  row: MyConsent;
  onDecide: () => void;
  onWithdrawn: () => void;
}) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const mayDecide = useCan('privacy.my_consent.create');
  const mayWithdraw = useCan('privacy.my_consent.withdraw');
  const actions = consentActions(row);
  const withdraw = useWithdrawMyConsent();
  const [confirming, setConfirming] = useState(false);
  const minor = row.capacity === 'minor_self';
  return (
    <View
      style={{
        marginTop: theme.space[5],
        padding: theme.space[5],
        gap: theme.space[3],
        borderRadius: theme.radius.md,
        borderWidth: 1,
        borderColor: theme.colors.border,
        backgroundColor: theme.colors.surface,
      }}
    >
      <Text variant="h3">{enumLabel(t, 'privacyPurpose', row.purposeCode)}</Text>
      {row.myStatus ? <Text>{enumLabel(t, 'myConsentStatus', row.myStatus)}</Text> : null}
      <Text>
        {row.effectiveWidestScope
          ? t('privacy.consentCentre.sharedWith', {
              scope: enumLabel(t, 'consentScope', row.effectiveWidestScope),
            })
          : t('privacy.consentCentre.notShared')}
      </Text>
      {minor && row.decidesFrom ? (
        <Text>{t('privacy.consentCentre.minor', { date: format.day(row.decidesFrom) })}</Text>
      ) : null}
      {row.decidedAt ? (
        <Text muted>{t('privacy.consentCentre.answeredOn', { date: format.date(row.decidedAt) })}</Text>
      ) : null}
      {!minor && !actions.decide ? <Text muted>{t('privacy.consentCentre.noNotice')}</Text> : null}
      {withdraw.error ? <Banner intent="danger">{t(consentFailureKey(withdraw.error))}</Banner> : null}
      {mayDecide && actions.decide ? (
        <Button variant="secondary" onPress={onDecide}>
          {row.id ? t('privacy.consentCentre.change') : t('privacy.consentCentre.decide')}
        </Button>
      ) : null}
      {mayWithdraw && actions.withdraw && !confirming ? (
        <Button
          variant="ghost"
          onPress={() => {
            withdraw.reset();
            setConfirming(true);
          }}
        >
          {t('privacy.consentCentre.withdraw')}
        </Button>
      ) : null}
      {confirming ? (
        <>
          <Text variant="h3">{t('privacy.consentCentre.withdrawTitle')}</Text>
          <Text>{t('privacy.consentCentre.withdrawBody')}</Text>
          <Button
            variant="danger"
            loading={withdraw.isPending}
            onPress={() =>
              withdraw.mutate(
                { consentRecordId: row.id ?? '', etag: row.etag },
                { onSettled: () => setConfirming(false), onSuccess: onWithdrawn },
              )
            }
          >
            {t('privacy.consentCentre.withdrawConfirm')}
          </Button>
          <Button variant="secondary" onPress={() => setConfirming(false)}>
            {t('common.action.cancel')}
          </Button>
        </>
      ) : null}
    </View>
  );
}

/**
 * The notice in the chosen language, then the decision under exactly the version and language shown: a width purpose
 * asks how far it may be shared before "Allow" ("Don't allow" refuses it everywhere); nothing is chosen in advance.
 */
function DecisionPage({
  tablet,
  row,
  onBack,
  onSaved,
}: {
  tablet: boolean;
  row: MyConsent;
  onBack: () => void;
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
  const contact = notice.data?.grievanceContact;
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {enumLabel(t, 'privacyPurpose', row.purposeCode)}
      </Text>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      <Text variant="h2">{t('privacy.consentCentre.notice.title')}</Text>
      <ChoiceGroup
        label={t('privacy.consentCentre.notice.language')}
        choices={NOTICE_LANGUAGES.map((code) => ({
          value: code,
          label: t(`setting.value.${code}` as MessageKey),
        }))}
        value={language}
        onChange={setLanguage}
      />
      {notice.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {notice.isError ? (
        <>
          <Banner intent="danger">{t(consentFailureKey(notice.error))}</Banner>
          <Button variant="secondary" onPress={() => void notice.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {notice.data ? (
        <>
          {notice.data.language !== language ? (
            <Banner intent="info">{t('privacy.consentCentre.notice.fallback')}</Banner>
          ) : null}
          <Text muted>
            {t('privacy.consentCentre.notice.version', {
              version: notice.data.version,
              date: format.date(notice.data.publishedAt),
            })}
          </Text>
          {row.capacity === 'guardian' && notice.data.summaryForChildren ? (
            <>
              <Text variant="h3">{t('privacy.consentCentre.notice.forChildren')}</Text>
              <Text>{notice.data.summaryForChildren}</Text>
            </>
          ) : null}
          <Text>{notice.data.bodyMarkdown}</Text>
          <Text variant="h3">{t('privacy.consentCentre.notice.grievance')}</Text>
          <Text>
            {[contact?.name, contact?.role, contact?.email, contact?.phone].filter(Boolean).join(' · ')}
          </Text>
          {widths ? (
            <ChoiceGroup
              label={t('privacy.consentCentre.scopeLabel')}
              hint={missingScope ? t('privacy.consentCentre.chooseScope') : undefined}
              choices={row.allowedScopes.map((value) => ({
                value,
                label: enumLabel(t, 'consentScope', value),
              }))}
              value={scope ?? ''}
              onChange={(value) => {
                setScope(row.allowedScopes.find((s) => s === value));
                setMissingScope(false);
              }}
            />
          ) : null}
          {record.error ? <Banner intent="danger">{t(consentFailureKey(record.error))}</Banner> : null}
          <Button
            variant="secondary"
            loading={record.isPending && record.variables?.decision === 'refused'}
            onPress={() => decide('refused')}
          >
            {t('privacy.consentCentre.refuse')}
          </Button>
          <Button
            variant="secondary"
            loading={record.isPending && record.variables?.decision === 'granted'}
            onPress={() => decide('granted')}
          >
            {t('privacy.consentCentre.allow')}
          </Button>
        </>
      ) : null}
    </Screen>
  );
}
